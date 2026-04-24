package com.example.flux.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.flux.ui.search.GlobalSearchResult
import com.example.flux.ui.search.GlobalSearchResultType
import com.example.flux.ui.search.GlobalSearchScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchSheet(
    query: String,
    scope: GlobalSearchScope,
    results: List<GlobalSearchResult>,
    onQueryChange: (String) -> Unit,
    onScopeChange: (GlobalSearchScope) -> Unit,
    onDismiss: () -> Unit,
    onResultClick: (GlobalSearchResult) -> Unit
) {
    UnifiedSearchSheet(
        title = "\u7edf\u4e00\u5165\u53e3",
        query = query,
        placeholder = "\u641c\u7d22\u65e5\u8bb0\u3001\u5f85\u529e\u3001\u4e8b\u4ef6\u6216\u9644\u4ef6",
        infoText = when {
            query.isBlank() -> "\u8f93\u5165\u5173\u952e\u8bcd\u540e\uff0c\u8fd9\u91cc\u4f1a\u7edf\u4e00\u5217\u51fa\u56db\u7c7b\u7ed3\u679c"
            results.isEmpty() -> "\u6ca1\u6709\u627e\u5230\u5339\u914d\u5185\u5bb9"
            else -> "\u627e\u5230 ${results.size} \u6761\u7ed3\u679c"
        },
        onQueryChange = onQueryChange,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScopeChips(
                selected = scope,
                onSelected = onScopeChange
            )
            when {
                query.isBlank() -> {
                    Text(
                        text = "\u8bd5\u8bd5\u8f93\u5165\u6807\u9898\u3001\u65e5\u671f\u3001\u5730\u70b9\u3001\u8def\u5f84\u6216\u6587\u4ef6\u540d",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                results.isEmpty() -> {
                    Text(
                        text = "\u6ca1\u6709\u627e\u5230\u5339\u914d\u5185\u5bb9",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyColumn {
                        GlobalSearchResultType.entries.forEach { type ->
                            val typedResults = results.filter { it.type == type }
                            if (typedResults.isNotEmpty()) {
                                item(key = "header-${type.name}") {
                                    Text(
                                        text = "${type.label} ${typedResults.size}",
                                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                items(
                                    items = typedResults,
                                    key = { "${it.type.name}:${it.id}" }
                                ) { result ->
                                    UnifiedSearchResultRow(
                                        title = result.title,
                                        subtitle = result.subtitle,
                                        overline = result.overline,
                                        onClick = { onResultClick(result) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScopeChips(
    selected: GlobalSearchScope,
    onSelected: (GlobalSearchScope) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(GlobalSearchScope.entries) { scope ->
            FilterChip(
                selected = scope == selected,
                onClick = { onSelected(scope) },
                label = { Text(scope.label) }
            )
        }
    }
}
