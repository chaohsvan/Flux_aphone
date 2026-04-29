package com.example.flux.feature.settings.presentation

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flux.core.settings.WeatherAppBinding

private data class WeatherAppCandidate(
    val label: String,
    val packageName: String,
    val activityName: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherAppBindingScreen(
    onNavigateUp: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val candidates = remember {
        context.packageManager.findWeatherAppCandidates()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("天气 App 绑定") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (uiState.weatherAppBinding != null) {
                        TextButton(onClick = viewModel::clearWeatherAppBinding) {
                            Text("清除")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(vertical = 8.dp)
        ) {
            val binding = uiState.weatherAppBinding
            ListItem(
                headlineContent = { Text("当前绑定") },
                supportingContent = {
                    Text(binding?.displayName ?: "尚未绑定，日历顶部天气按钮会提示先绑定")
                },
                leadingContent = {
                    Icon(
                        Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )

            Text(
                text = "可选天气应用",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium
            )

            if (candidates.isEmpty()) {
                Text(
                    text = "没有识别到名称或包名包含“天气 / weather / tianqi”的应用。",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                candidates.forEach { candidate ->
                    val selected = binding?.packageName == candidate.packageName &&
                        (binding.activityName == null || binding.activityName == candidate.activityName)
                    ListItem(
                        headlineContent = { Text(candidate.label) },
                        supportingContent = { Text(candidate.packageName) },
                        leadingContent = {
                            Icon(
                                Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        },
                        modifier = Modifier.clickable {
                            viewModel.setWeatherAppBinding(
                                WeatherAppBinding(
                                    packageName = candidate.packageName,
                                    activityName = candidate.activityName,
                                    displayName = candidate.label
                                )
                            )
                            onNavigateUp()
                        }
                    )
                }
            }
        }
    }
}

private fun PackageManager.findWeatherAppCandidates(): List<WeatherAppCandidate> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return queryIntentActivities(intent, 0)
        .mapNotNull { info -> info.toWeatherAppCandidate(this) }
        .filter { candidate ->
            val searchable = "${candidate.label} ${candidate.packageName}".lowercase()
            searchable.contains("天气") ||
                searchable.contains("weather") ||
                searchable.contains("tianqi")
        }
        .distinctBy { it.packageName to it.activityName }
        .sortedWith(compareBy<WeatherAppCandidate> { it.label }.thenBy { it.packageName })
}

private fun ResolveInfo.toWeatherAppCandidate(packageManager: PackageManager): WeatherAppCandidate? {
    val activity = activityInfo ?: return null
    val label = loadLabel(packageManager)?.toString()?.takeIf { it.isNotBlank() }
        ?: activity.packageName
    val component = ComponentName(activity.packageName, activity.name)
    return WeatherAppCandidate(
        label = label,
        packageName = component.packageName,
        activityName = component.className
    )
}
