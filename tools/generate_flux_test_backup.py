#!/usr/bin/env python3
"""Generate a Flux backup ZIP with broad diary, todo, and calendar-time data."""

from __future__ import annotations

import json
import shutil
import sqlite3
import zipfile
from datetime import date, datetime, timedelta
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SCHEMA_PATH = ROOT / "app" / "schemas" / "com.example.flux.core.database.FluxDatabase" / "10.json"
OUTPUT_ROOT = ROOT / "testdata" / "backups" / "flux_time_todo_diary"
DATA_DIR = OUTPUT_ROOT / "data"
DB_PATH = DATA_DIR / "flux.db"
ZIP_PATH = OUTPUT_ROOT.with_suffix(".zip")


MOODS = ["开心", "平静", "低落", "紧张", "期待", "疲惫", "焦虑", "感恩", "兴奋", "放松", "伤心", "愤怒"]
WEATHERS = ["晴", "多云", "小雨", "大雨", "雪", "雾", "阴", "雷阵雨"]
LOCATIONS = ["北京", "上海", "广州", "深圳", "杭州", "成都", "南京", "武汉", "西安", "苏州"]
EVENT_COLORS = ["#4A90E2", "#34A853", "#F9AB00", "#EA4335", "#9C27B0", "#00ACC1"]


def main() -> None:
    reset_output()
    with sqlite3.connect(DB_PATH) as conn:
        conn.execute("PRAGMA foreign_keys=OFF")
        create_schema(conn)
        seed_diaries(conn)
        seed_todos(conn)
        seed_calendar_events(conn)
        conn.execute("PRAGMA user_version=10")
        conn.commit()
        assert conn.execute("PRAGMA integrity_check").fetchone()[0] == "ok"
    create_zip()
    print(f"数据库: {DB_PATH}")
    print(f"备份包: {ZIP_PATH}")


def reset_output() -> None:
    if OUTPUT_ROOT.exists():
        shutil.rmtree(OUTPUT_ROOT)
    if ZIP_PATH.exists():
        ZIP_PATH.unlink()
    DATA_DIR.mkdir(parents=True, exist_ok=True)


def create_schema(conn: sqlite3.Connection) -> None:
    schema = json.loads(SCHEMA_PATH.read_text(encoding="utf-8"))
    for entity in schema["database"]["entities"]:
        table = entity["tableName"]
        create_sql = entity["createSql"].replace("`${TABLE_NAME}`", f"`{table}`")
        conn.execute(create_sql)
        for index in entity.get("indices", []):
            index_sql = index["createSql"].replace("`${TABLE_NAME}`", f"`{table}`")
            conn.execute(index_sql)


def seed_diaries(conn: sqlite3.Connection) -> None:
    base = date(2025, 12, 20)
    rows = []
    search_rows = []
    for index in range(72):
        entry_date = base + timedelta(days=index * 3)
        entry_time = f"{(index * 3) % 24:02d}:{(index * 7) % 60:02d}"
        title = f"测试日记 {index + 1:02d} - {entry_date.isoformat()}"
        mood = MOODS[index % len(MOODS)]
        weather = WEATHERS[index % len(WEATHERS)]
        location = LOCATIONS[index % len(LOCATIONS)]
        content = "\n".join(
            [
                f"这是第 {index + 1} 篇用于导入测试的日记。",
                f"日期 {entry_date.isoformat()}，时间 {entry_time}，心情 {mood}，天气 {weather}。",
                "覆盖跨年、跨月、早晚时间、收藏、删除和恢复字段。",
                "- 中文搜索关键词：导入测试 时间 日记",
            ]
        )
        deleted_at = timestamp(entry_date, 23, 30) if index in {8, 21, 34, 47, 60} else None
        restored_at = timestamp(entry_date + timedelta(days=2), 9, 15) if index in {9, 35} else None
        restored_into_id = f"diary-restored-target-{index:02d}" if restored_at else None
        created_at = timestamp(entry_date, 7, 20)
        updated_at = timestamp(entry_date, 21, 45)
        row = (
            f"diary-{index + 1:03d}",
            entry_date.isoformat(),
            entry_time,
            title,
            content,
            mood,
            weather,
            location,
            1 if index % 5 == 0 else 0,
            len(content),
            created_at,
            updated_at,
            deleted_at,
            2 if restored_at else 1,
            restored_at,
            restored_into_id,
        )
        rows.append(row)
        if deleted_at is None:
            search_rows.append(
                (
                    row[0],
                    row[1],
                    row[2],
                    row[3],
                    row[4],
                    row[5],
                    row[6],
                    row[7],
                    "测试 标签",
                )
            )

    conn.executemany(
        """
        INSERT INTO diaries (
            id, entry_date, entry_time, title, content_md, mood, weather, location_name,
            is_favorite, word_count, created_at, updated_at, deleted_at, version,
            restored_at, restored_into_id
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        rows,
    )
    conn.executemany(
        """
        INSERT INTO diary_search_index (
            diary_id, entry_date, entry_time, title, content_md, mood, weather, location_name, tags
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        search_rows,
    )


def seed_todos(conn: sqlite3.Connection) -> None:
    projects = [
        ("todo-project-work", "工作", "#4A90E2", 0),
        ("todo-project-life", "生活", "#34A853", 1),
        ("todo-project-study", "学习", "#F9AB00", 2),
        ("todo-project-health", "健康", "#EA4335", 3),
    ]
    now = "2026-04-01T08:00:00"
    conn.executemany(
        """
        INSERT INTO todo_projects (id, name, color, sort_order, created_at, updated_at, deleted_at, version)
        VALUES (?, ?, ?, ?, ?, ?, NULL, 1)
        """,
        [(project_id, name, color, order, now, now) for project_id, name, color, order in projects],
    )

    base = date(2026, 1, 1)
    todos = []
    subtasks = []
    histories = []
    statuses = ["pending", "in_progress", "completed"]
    priorities = ["normal", "high", "none"]
    for index in range(96):
        start_day = base + timedelta(days=index - 20)
        due_day = base + timedelta(days=index - 18 + (index % 5))
        has_due = index % 9 != 0
        has_start = index % 7 != 0
        completed = statuses[index % len(statuses)] == "completed"
        deleted_at = timestamp(due_day, 22, 10) if index in {12, 25, 38, 51, 64, 77, 90} else None
        todo_id = f"todo-{index + 1:03d}"
        start_at = timestamp(start_day, (index * 2) % 24, (index * 5) % 60) if has_start else None
        due_at = timestamp(due_day, (index * 3) % 24, (index * 11) % 60) if has_due else None
        created_at = timestamp(start_day - timedelta(days=1), 8, 0)
        updated_at = timestamp(due_day, 18, 0)
        project_id = projects[index % len(projects)][0]
        status = statuses[index % len(statuses)]
        priority = priorities[index % len(priorities)]
        todos.append(
            (
                todo_id,
                project_id,
                f"测试待办 {index + 1:03d}",
                f"覆盖导入测试、提醒、开始时间、截止时间、状态和优先级。序号 {index + 1}",
                status,
                priority,
                due_at,
                start_at,
                timestamp(due_day, 20, 0) if completed else None,
                index,
                1 if index % 6 == 0 else 0,
                15 if index % 4 == 0 else (60 if index % 10 == 0 else None),
                1 if priority == "high" else 0,
                created_at,
                updated_at,
                deleted_at,
                1,
            )
        )
        histories.append((f"todo-history-{index + 1:03d}-create", todo_id, "create", "创建测试待办", "{}", created_at))
        histories.append((f"todo-history-{index + 1:03d}-edit", todo_id, "edit", "更新时间字段", "{}", updated_at))
        if completed:
            histories.append((f"todo-history-{index + 1:03d}-complete", todo_id, "status", "标记完成", "{}", timestamp(due_day, 20, 0)))
        for sub_index in range(index % 4):
            subtasks.append(
                (
                    f"todo-subtask-{index + 1:03d}-{sub_index + 1}",
                    todo_id,
                    f"子任务 {sub_index + 1}",
                    1 if sub_index == 0 and completed else 0,
                    sub_index,
                    created_at,
                    updated_at,
                    None,
                    1,
                )
            )

    conn.executemany(
        """
        INSERT INTO todos (
            id, project_id, title, description, status, priority, due_at, start_at,
            completed_at, sort_order, is_my_day, reminder_minutes, is_important,
            created_at, updated_at, deleted_at, version
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        todos,
    )
    conn.executemany(
        """
        INSERT INTO todo_subtasks (
            id, todo_id, title, is_completed, sort_order, created_at, updated_at, deleted_at, version
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        subtasks,
    )
    conn.executemany(
        """
        INSERT INTO todo_history (id, todo_id, action, summary, payload_json, created_at)
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        histories,
    )


def seed_calendar_events(conn: sqlite3.Connection) -> None:
    base = date(2025, 12, 25)
    rows = []
    recurrence_rules = [None, "daily;interval=2;until=2026-01-15", "weekly;until=2026-06-30", "monthly;interval=2"]
    for index in range(84):
        day = base + timedelta(days=index * 4)
        all_day = 1 if index % 6 == 0 else 0
        if all_day:
            start_at = day.isoformat()
            end_at = (day + timedelta(days=1 if index % 12 == 0 else 0)).isoformat()
        else:
            start_hour = 6 + (index % 12)
            start_at = timestamp(day, start_hour, (index * 13) % 60, space=True)
            end_at = timestamp(day + timedelta(days=1 if index % 17 == 0 else 0), min(start_hour + 2, 23), (index * 13) % 60, space=True)
        deleted_at = timestamp(day, 23, 0) if index in {10, 23, 36, 49, 62, 75} else None
        rows.append(
            (
                f"event-{index + 1:03d}",
                f"测试时间事件 {index + 1:03d}",
                "覆盖全天、定时、跨天、提醒、重复、删除和订阅字段为空的本地事件。",
                start_at,
                end_at,
                all_day,
                EVENT_COLORS[index % len(EVENT_COLORS)],
                LOCATIONS[index % len(LOCATIONS)],
                10 if index % 5 == 0 else (30 if index % 8 == 0 else None),
                recurrence_rules[index % len(recurrence_rules)],
                None,
                None,
                None,
                timestamp(day - timedelta(days=2), 9, 0),
                timestamp(day, 18, 0),
                deleted_at,
                1,
            )
        )

    conn.executemany(
        """
        INSERT INTO calendar_events (
            id, title, description, start_at, end_at, all_day, color, location_name,
            reminder_minutes, recurrence_rule, subscription_id, external_uid, external_hash,
            created_at, updated_at, deleted_at, version
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        rows,
    )


def timestamp(value: date, hour: int, minute: int, space: bool = False) -> str:
    dt = datetime(value.year, value.month, value.day, hour, minute, 0)
    return dt.strftime("%Y-%m-%d %H:%M" if space else "%Y-%m-%dT%H:%M:%S")


def create_zip() -> None:
    with zipfile.ZipFile(ZIP_PATH, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        archive.write(DB_PATH, "data/flux.db")


if __name__ == "__main__":
    main()
