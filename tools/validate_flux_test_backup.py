#!/usr/bin/env python3
"""Validate the generated Flux test backup and simulate merge import."""

from __future__ import annotations

import json
import shutil
import sqlite3
import tempfile
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SCHEMA_PATH = ROOT / "app" / "schemas" / "com.example.flux.core.database.FluxDatabase" / "10.json"
BACKUP_PATH = ROOT / "testdata" / "backups" / "flux_time_todo_diary.zip"
MERGE_TABLES = [
    "calendar_static_holidays",
    "calendar_holidays",
    "calendar_subscription",
    "calendar_events",
    "diaries",
    "diary_tags",
    "diary_tag_links",
    "diary_search_index",
    "todo_projects",
    "todos",
    "todo_subtasks",
    "todo_history",
    "attachment_metadata",
]


def main() -> None:
    assert BACKUP_PATH.is_file(), f"找不到备份包: {BACKUP_PATH}"
    with tempfile.TemporaryDirectory(prefix="flux_import_test_", ignore_cleanup_errors=True) as temp_name:
        temp_root = Path(temp_name)
        staged_db = extract_like_import_use_case(temp_root)
        validate_backup_database(staged_db)
        simulate_merge_import(staged_db, temp_root / "main.db")
    print("导入备份验证通过")


def extract_like_import_use_case(temp_root: Path) -> Path:
    restore_root = temp_root / "restore"
    restore_root.mkdir()
    with zipfile.ZipFile(BACKUP_PATH) as archive:
        for info in archive.infolist():
            if info.is_dir():
                continue
            clean_name = info.filename.replace("\\", "/").lstrip("/")
            if not clean_name.startswith("data/"):
                continue
            target = (restore_root / clean_name).resolve()
            if not str(target).startswith(str(restore_root.resolve())):
                raise AssertionError("备份包包含非法路径")
            target.parent.mkdir(parents=True, exist_ok=True)
            with archive.open(info) as source, target.open("wb") as output:
                shutil.copyfileobj(source, output)

    staged_db = restore_root / "data" / "flux.db"
    assert staged_db.is_file() and staged_db.stat().st_size > 0, "备份包缺少 data/flux.db"
    return staged_db


def validate_backup_database(db_path: Path) -> None:
    with sqlite3.connect(db_path) as conn:
        assert conn.execute("PRAGMA integrity_check").fetchone()[0] == "ok"
        assert conn.execute("PRAGMA user_version").fetchone()[0] == 10
        expected_counts = {
            "diaries": 72,
            "diary_search_index": 67,
            "todo_projects": 4,
            "todos": 96,
            "todo_subtasks": 144,
            "todo_history": 224,
            "calendar_events": 84,
        }
        for table, expected in expected_counts.items():
            actual = conn.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0]
            assert actual == expected, f"{table} 数量不对: {actual} != {expected}"

        todo_columns = {row[1] for row in conn.execute("PRAGMA table_info(todos)")}
        forbidden_columns = {"recurrence", "recurrence_interval", "recurrence_until", "parent_todo_id"}
        assert todo_columns.isdisjoint(forbidden_columns), "todos 表仍包含重复待办字段"

        assert conn.execute("SELECT COUNT(*) FROM todos WHERE due_at IS NULL").fetchone()[0] > 0
        assert conn.execute("SELECT COUNT(*) FROM todos WHERE start_at IS NULL").fetchone()[0] > 0
        assert conn.execute("SELECT COUNT(*) FROM todos WHERE reminder_minutes IS NOT NULL").fetchone()[0] > 0
        assert conn.execute("SELECT COUNT(*) FROM calendar_events WHERE recurrence_rule IS NOT NULL").fetchone()[0] > 0
        assert conn.execute("SELECT COUNT(*) FROM diaries WHERE deleted_at IS NOT NULL").fetchone()[0] > 0


def simulate_merge_import(staged_db: Path, main_db: Path) -> None:
    create_empty_database(main_db)
    with sqlite3.connect(main_db) as conn:
        conn.execute(f"ATTACH DATABASE '{escape_path(staged_db)}' AS backup")
        try:
            conn.execute("PRAGMA foreign_keys=OFF")
            conn.execute("BEGIN TRANSACTION")
            for table in MERGE_TABLES:
                if not table_exists(conn, "main", table) or not table_exists(conn, "backup", table):
                    continue
                main_columns = table_columns(conn, "main", table)
                backup_columns = table_columns(conn, "backup", table)
                shared_columns = [name for name, _ in main_columns if name in {col for col, _ in backup_columns}]
                if not shared_columns:
                    continue
                primary_keys = [name for name, pk in main_columns if pk > 0 and name in {col for col, _ in backup_columns}]
                quoted_columns = ", ".join(quote_identifier(name) for name in shared_columns)
                table_name = quote_identifier(table)
                if primary_keys:
                    key_join = " AND ".join(
                        f"main.{table_name}.{quote_identifier(key)} = backup.{table_name}.{quote_identifier(key)}"
                        for key in primary_keys
                    )
                    conn.execute(
                        f"""
                        DELETE FROM main.{table_name}
                        WHERE EXISTS (
                            SELECT 1 FROM backup.{table_name}
                            WHERE {key_join}
                        )
                        """
                    )
                conn.execute(
                    f"""
                    INSERT OR IGNORE INTO main.{table_name} ({quoted_columns})
                    SELECT {quoted_columns} FROM backup.{table_name}
                    """
                )
            conn.execute("COMMIT")
        except Exception:
            conn.execute("ROLLBACK")
            raise
        finally:
            conn.execute("PRAGMA foreign_keys=ON")
            conn.execute("DETACH DATABASE backup")

        assert conn.execute("SELECT COUNT(*) FROM diaries").fetchone()[0] == 72
        assert conn.execute("SELECT COUNT(*) FROM todos").fetchone()[0] == 96
        assert conn.execute("SELECT COUNT(*) FROM calendar_events").fetchone()[0] == 84


def create_empty_database(db_path: Path) -> None:
    schema = json.loads(SCHEMA_PATH.read_text(encoding="utf-8"))
    with sqlite3.connect(db_path) as conn:
        conn.execute("PRAGMA foreign_keys=OFF")
        for entity in schema["database"]["entities"]:
            table = entity["tableName"]
            conn.execute(entity["createSql"].replace("`${TABLE_NAME}`", f"`{table}`"))
            for index in entity.get("indices", []):
                conn.execute(index["createSql"].replace("`${TABLE_NAME}`", f"`{table}`"))
        conn.execute("PRAGMA user_version=10")
        conn.commit()


def table_exists(conn: sqlite3.Connection, schema: str, table: str) -> bool:
    row = conn.execute(
        f"SELECT name FROM {schema}.sqlite_master WHERE type='table' AND name=?",
        (table,),
    ).fetchone()
    return row is not None


def table_columns(conn: sqlite3.Connection, schema: str, table: str) -> list[tuple[str, int]]:
    return [(row[1], row[5]) for row in conn.execute(f"PRAGMA {schema}.table_info('{table}')")]


def quote_identifier(value: str) -> str:
    return '"' + value.replace('"', '""') + '"'


def escape_path(path: Path) -> str:
    return str(path.resolve()).replace("'", "''")


if __name__ == "__main__":
    main()
