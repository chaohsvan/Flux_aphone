package com.example.flux.app.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.flux.feature.calendar.presentation.CalendarScreen
import com.example.flux.feature.diary.presentation.DiaryEditorScreen
import com.example.flux.feature.diary.presentation.DiaryScreen
import com.example.flux.feature.search.presentation.GlobalSearchResultType
import com.example.flux.feature.search.presentation.GlobalSearchViewModel
import com.example.flux.feature.search.presentation.component.GlobalSearchSheet
import com.example.flux.feature.settings.presentation.SettingsScreen
import com.example.flux.feature.todo.presentation.TodoDetailScreen
import com.example.flux.feature.todo.presentation.TodoScreen
import com.example.flux.feature.trash.presentation.AttachmentManagerScreen
import com.example.flux.feature.trash.presentation.TrashScreen

@Composable
fun FluxAppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.MAIN
    ) {
        composable(AppRoutes.MAIN) {
            FluxMainScaffold(
                onNavigateToEditor = { diaryId ->
                    navController.navigate(AppRoutes.editor(diaryId))
                },
                onNavigateToDiaryFromCalendar = { diaryId, date ->
                    if (diaryId != null) {
                        navController.navigate(AppRoutes.editor(diaryId))
                    } else {
                        navController.navigate(AppRoutes.newEditor(date))
                    }
                },
                onNavigateToTrash = {
                    navController.navigate(AppRoutes.TRASH)
                },
                onNavigateToAttachmentManager = { query ->
                    navController.navigate(AppRoutes.attachmentManager(query))
                },
                onNavigateToTodoDetail = { todoId ->
                    navController.navigate(AppRoutes.todoDetail(todoId))
                }
            )
        }
        composable(
            route = AppRoutes.EDITOR_PATTERN,
            arguments = listOf(
                navArgument(AppRoutes.EDITOR_ARG_DIARY_ID) { type = NavType.StringType },
                navArgument(AppRoutes.EDITOR_ARG_ENTRY_DATE) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) {
            DiaryEditorScreen(onNavigateUp = { navController.popBackStack() })
        }
        composable(AppRoutes.TRASH) {
            TrashScreen(
                onNavigateUp = { navController.popBackStack() },
                onNavigateToAttachmentManager = {
                    navController.navigate(AppRoutes.attachmentManager())
                }
            )
        }
        composable(
            route = AppRoutes.ATTACHMENT_MANAGER_PATTERN,
            arguments = listOf(
                navArgument(AppRoutes.ATTACHMENT_QUERY_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            AttachmentManagerScreen(
                onNavigateUp = { navController.popBackStack() },
                onOpenDiary = { diaryId ->
                    navController.navigate(AppRoutes.editor(diaryId))
                },
                initialQuery = backStackEntry.arguments?.getString(AppRoutes.ATTACHMENT_QUERY_ARG).orEmpty()
            )
        }
        composable(
            route = AppRoutes.TODO_PATTERN,
            arguments = listOf(navArgument(AppRoutes.TODO_ARG_ID) { type = NavType.StringType })
        ) {
            TodoDetailScreen(onNavigateUp = { navController.popBackStack() })
        }
    }
}

@Composable
private fun FluxMainScaffold(
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
            AppDestinations.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            destination.icon,
                            contentDescription = destination.label
                        )
                    },
                    label = { Text(destination.label) },
                    selected = destination == currentDestination,
                    onClick = { currentDestination = destination }
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
