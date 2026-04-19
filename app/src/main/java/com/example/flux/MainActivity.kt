package com.example.flux

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.flux.feature.calendar.ui.CalendarScreen
import com.example.flux.feature.diary.ui.DiaryEditorScreen
import com.example.flux.feature.diary.ui.DiaryScreen
import com.example.flux.feature.todo.ui.TodoDetailScreen
import com.example.flux.feature.todo.ui.TodoScreen
import com.example.flux.feature.trash.ui.TrashScreen
import com.example.flux.feature.trash.ui.AttachmentManagerScreen
import com.example.flux.ui.theme.FluxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FluxTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        FluxApp(
                            onNavigateToEditor = { diaryId ->
                                navController.navigate("editor/$diaryId")
                            },
                            onNavigateToTrash = {
                                navController.navigate("trash")
                            },
                            onNavigateToTodoDetail = { todoId ->
                                navController.navigate("todo/$todoId")
                            }
                        )
                    }
                    composable(
                        route = "editor/{diaryId}",
                        arguments = listOf(navArgument("diaryId") { type = NavType.StringType })
                    ) {
                        DiaryEditorScreen(onNavigateUp = { navController.popBackStack() })
                    }
                    composable("trash") {
                        TrashScreen(
                            onNavigateUp = { navController.popBackStack() },
                            onNavigateToAttachmentManager = { navController.navigate("attachment_manager") }
                        )
                    }
                    composable("attachment_manager") {
                        AttachmentManagerScreen(
                            onNavigateUp = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "todo/{todoId}",
                        arguments = listOf(navArgument("todoId") { type = NavType.StringType })
                    ) {
                        TodoDetailScreen(onNavigateUp = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

@Composable
fun FluxApp(
    onNavigateToEditor: (String) -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToTodoDetail: (String) -> Unit
) {
    // 默认启动项设置为 TODO，以便验证 Phase 4
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.TODO) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        when (currentDestination) {
            AppDestinations.TODO -> TodoScreen(onNavigateToDetail = onNavigateToTodoDetail)
            AppDestinations.DIARY -> DiaryScreen(
                onNavigateToEditor = onNavigateToEditor,
                onNavigateToTrash = onNavigateToTrash
            )
            AppDestinations.CALENDAR -> CalendarScreen()
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    DIARY("日记", Icons.Default.Edit),
    CALENDAR("日历", Icons.Default.DateRange),
    TODO("待办", Icons.Default.List),
}