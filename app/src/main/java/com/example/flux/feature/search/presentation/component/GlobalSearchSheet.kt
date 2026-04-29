package com.example.flux.feature.search.presentation.component

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
import com.example.flux.feature.search.presentation.GlobalSearchResult
import com.example.flux.feature.search.presentation.GlobalSearchResultType
import com.example.flux.feature.search.presentation.GlobalSearchScope
import com.example.flux.ui.component.UnifiedSearchResultRow
import com.example.flux.ui.component.UnifiedSearchSheet

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
        title = "全局搜索",
        query = query,
        placeholder = "搜索日记、待办、事件或附件",
        onQueryChange = onQueryChange,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScopeChips(selected = scope, onSelected = onScopeChange)
            when {
                query.isBlank() -> Unit

                results.isEmpty() -> {
                    Text(
                        text = "没有找到匹配内容",
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
                                items(typedResults, key = { "${it.type.name}:${it.id}" }) { result ->
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
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(GlobalSearchScope.entries) { scope ->
            FilterChip(
                selected = scope == selected,
                onClick = { onSelected(scope) },
                label = { Text(scope.label) }
            )
        }
    }
}
