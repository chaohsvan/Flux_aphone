package com.example.flux

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.flux.feature.calendar.ui.CalendarScreen
import com.example.flux.feature.diary.ui.DiaryEditorScreen
import com.example.flux.feature.diary.ui.DiaryScreen
import com.example.flux.feature.settings.ui.SettingsScreen
import com.example.flux.feature.todo.ui.TodoDetailScreen
import com.example.flux.feature.todo.ui.TodoScreen
import com.example.flux.feature.trash.ui.AttachmentManagerScreen
import com.example.flux.feature.trash.ui.TrashScreen
import com.example.flux.ui.component.GlobalSearchSheet
import com.example.flux.ui.search.GlobalSearchResultType
import com.example.flux.ui.search.GlobalSearchViewModel
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
                            onNavigateToDiaryFromCalendar = { diaryId, date ->
                                if (diaryId != null) {
                                    navController.navigate("editor/$diaryId")
                                } else {
                                    navController.navigate("editor/new?date=$date")
                                }
                            },
                            onNavigateToTrash = {
                                navController.navigate("trash")
                            },
                            onNavigateToAttachmentManager = { query ->
                                val route = if (query.isBlank()) {
                                    "attachment_manager"
                                } else {
                                    "attachment_manager?query=${Uri.encode(query)}"
                                }
                                navController.navigate(route)
                            },
                            onNavigateToTodoDetail = { todoId ->
                                navController.navigate("todo/$todoId")
                            }
                        )
                    }
                    composable(
                        route = "editor/{diaryId}?date={entryDate}",
                        arguments = listOf(
                            navArgument("diaryId") { type = NavType.StringType },
                            navArgument("entryDate") {
                                type = NavType.StringType
                                defaultValue = ""
                            }
                        )
                    ) {
                        DiaryEditorScreen(onNavigateUp = { navController.popBackStack() })
                    }
                    composable("trash") {
                        TrashScreen(
                            onNavigateUp = { navController.popBackStack() },
                            onNavigateToAttachmentManager = { navController.navigate("attachment_manager") }
                        )
                    }
                    composable(
                        route = "attachment_manager?query={query}",
                        arguments = listOf(
                            navArgument("query") {
                                type = NavType.StringType
                                defaultValue = ""
                            }
                        )
                    ) { backStackEntry ->
                        AttachmentManagerScreen(
                            onNavigateUp = { navController.popBackStack() },
                            onOpenDiary = { diaryId -> navController.navigate("editor/$diaryId") },
                            initialQuery = backStackEntry.arguments?.getString("query").orEmpty()
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
    onNavigateToDiaryFromCalendar: (String?, String) -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToAttachmentManager: (String) -> Unit,
    onNavigateToTodoDetail: (String) -> Unit
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.DIARY) }
    var showGlobalSearch by rememberSaveable { mutableStateOf(false) }
    var calendarFocusDate by rememberSaveable { mutableStateOf<String?>(null) }

    val globalSearchViewModel: GlobalSearchViewModel = hiltViewModel()
    val searchQuery by globalSearchViewModel.query.collectAsState()
    val searchScope by globalSearchViewModel.scope.collectAsState()
    val searchResults by globalSearchViewModel.results.collectAsState()

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
            AppDestinations.DIARY -> DiaryScreen(
                onNavigateToEditor = onNavigateToEditor,
                onNavigateToTrash = onNavigateToTrash,
                onOpenGlobalSearch = { showGlobalSearch = true }
            )
            AppDestinations.CALENDAR -> CalendarScreen(
                onNavigateToDiary = onNavigateToDiaryFromCalendar,
                onNavigateToTrash = onNavigateToTrash,
                onOpenGlobalSearch = { showGlobalSearch = true },
                focusDateRequest = calendarFocusDate,
                onFocusDateHandled = { calendarFocusDate = null }
            )
            AppDestinations.TODO -> TodoScreen(
                onNavigateToDetail = onNavigateToTodoDetail,
                onNavigateToTrash = onNavigateToTrash,
                onOpenGlobalSearch = { showGlobalSearch = true }
            )
            AppDestinations.SETTINGS -> SettingsScreen(
                onNavigateToAttachmentManager = { onNavigateToAttachmentManager("") },
                onNavigateToTrash = onNavigateToTrash,
                onOpenGlobalSearch = { showGlobalSearch = true }
            )
        }
    }

    if (showGlobalSearch) {
        GlobalSearchSheet(
            query = searchQuery,
            scope = searchScope,
            results = searchResults,
            onQueryChange = globalSearchViewModel::updateQuery,
            onScopeChange = globalSearchViewModel::updateScope,
            onDismiss = { showGlobalSearch = false },
            onResultClick = { result ->
                showGlobalSearch = false
                when (result.type) {
                    GlobalSearchResultType.DIARY -> onNavigateToEditor(result.id)
                    GlobalSearchResultType.TODO -> onNavigateToTodoDetail(result.id)
                    GlobalSearchResultType.EVENT -> {
                        currentDestination = AppDestinations.CALENDAR
                        calendarFocusDate = result.dateSortKey
                    }
                    GlobalSearchResultType.ATTACHMENT -> {
                        onNavigateToAttachmentManager(result.attachmentQuery ?: result.id)
                    }
                }
            }
        )
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    DIARY("日记", Icons.Default.Edit),
    CALENDAR("日历", Icons.Default.DateRange),
    TODO("待办", Icons.AutoMirrored.Filled.List),
    SETTINGS("设置", Icons.Default.Settings),
}
