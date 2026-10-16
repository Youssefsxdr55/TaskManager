
package com.joe.taskmanager.ui.navigation
import androidx.compose.runtime.Composable
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.joe.taskmanager.R
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.joe.taskmanager.ui.screens.AddTaskScreen
import com.joe.taskmanager.ui.screens.BackupScreen
import com.joe.taskmanager.ui.screens.AllTasksScreen
import com.joe.taskmanager.ui.screens.CompletedScreen
import com.joe.taskmanager.ui.screens.OnboardingScreen
import com.joe.taskmanager.ui.screens.OrganizationScreen
import com.joe.taskmanager.ui.screens.ReliabilityScreen
import com.joe.taskmanager.ui.screens.SearchScreen
import com.joe.taskmanager.ui.screens.SettingsScreen
import com.joe.taskmanager.ui.screens.TagsScreen
import com.joe.taskmanager.ui.screens.HabitsScreen
import com.joe.taskmanager.ui.screens.RewardsScreen
import com.joe.taskmanager.ui.screens.RecurrenceScreen
import com.joe.taskmanager.ui.screens.CalendarScreen
import com.joe.taskmanager.ui.screens.InsightsScreen
import com.joe.taskmanager.ui.screens.FocusScreen
import com.joe.taskmanager.ui.screens.TaskDetailScreen
import com.joe.taskmanager.ui.screens.TaskListScreen
import com.joe.taskmanager.ui.screens.TodayScreen
import com.joe.taskmanager.ui.screens.TrashScreen
import com.joe.taskmanager.ui.screens.UpcomingScreen
@Composable
fun TaskManagerNavHost(showOnboarding: Boolean = false, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    fun openDrawerRoute(route: Route) {
        navController.navigate(route.path) {
            popUpTo(Route.Today.path) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        scope.launch { drawerState.close() }
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp))
                NavigationDrawerItem(label = { Text(stringResource(R.string.nav_inbox)) }, selected = false,
                    onClick = { openDrawerRoute(Route.Inbox) })
                NavigationDrawerItem(label = { Text(stringResource(R.string.nav_all_tasks)) }, selected = false,
                    onClick = { openDrawerRoute(Route.AllTasks) })
                NavigationDrawerItem(label = { Text(stringResource(R.string.nav_completed)) }, selected = false,
                    onClick = { openDrawerRoute(Route.Completed) })
                NavigationDrawerItem(label = { Text(stringResource(R.string.nav_organization)) }, selected = false,
                    onClick = { openDrawerRoute(Route.Organization) })
                NavigationDrawerItem(label = { Text(stringResource(R.string.nav_tags)) }, selected = false,
                    onClick = { openDrawerRoute(Route.Tags) })
                NavigationDrawerItem(label = { Text(stringResource(R.string.nav_habits)) }, selected = false,
                    onClick = { openDrawerRoute(Route.Habits) })
                NavigationDrawerItem(label = { Text(stringResource(R.string.nav_rewards)) }, selected = false,
                    onClick = { openDrawerRoute(Route.Rewards) })
                NavigationDrawerItem(label = { Text(stringResource(R.string.nav_recurrence)) }, selected = false,
                    onClick = { openDrawerRoute(Route.Recurrence) })
                NavigationDrawerItem(label = { Text(stringResource(R.string.nav_calendar)) }, selected = false,
                    onClick = { openDrawerRoute(Route.Calendar) })
                NavigationDrawerItem(label = { Text(stringResource(R.string.nav_insights)) }, selected = false,
                    onClick = { openDrawerRoute(Route.Insights) })
                NavigationDrawerItem(label = { Text(stringResource(R.string.nav_focus)) }, selected = false,
                    onClick = { openDrawerRoute(Route.Focus) })
                NavigationDrawerItem(label = { Text(stringResource(R.string.nav_settings)) }, selected = false,
                    onClick = { openDrawerRoute(Route.Settings) })
            }
        }
    ) {
    NavHost(
        navController = navController,
        startDestination = if (showOnboarding) Route.Onboarding.path else Route.Today.path,
        modifier = modifier
    ) {
        composable(Route.Onboarding.path) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Route.Today.path) {
                        popUpTo(Route.Onboarding.path) { inclusive = true }
                    }
                },
                onOpenReliability = { navController.navigate(Route.Reliability.path) }
            )
        }
        composable(Route.Today.path) {
            TodayScreen(
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onOpenToday = {
                    navController.navigate(Route.Today.path) {
                        popUpTo(Route.Today.path) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenUpcoming = {
                    navController.navigate(Route.Upcoming.path) {
                        popUpTo(Route.Today.path) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenCalendar = { openDrawerRoute(Route.Calendar) },
                onAddTask = { navController.navigate(Route.AddTask.create()) },
                onTaskClick = { navController.navigate(Route.TaskDetail.create(it)) },
                onSeeOverdue = { navController.navigate(Route.AllTasks.path) },
                onOpenList = { navController.navigate(Route.TaskListView.create(it)) },
                onOpenSettings = { navController.navigate(Route.Settings.path) },
                onSearch = { navController.navigate(Route.Search.path) }
            )
        }
        composable(Route.Upcoming.path) {
            UpcomingScreen(
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onOpenToday = {
                    navController.navigate(Route.Today.path) {
                        popUpTo(Route.Today.path) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenUpcoming = {
                    navController.navigate(Route.Upcoming.path) {
                        popUpTo(Route.Today.path) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenCalendar = { openDrawerRoute(Route.Calendar) },
                onTaskClick = { navController.navigate(Route.TaskDetail.create(it)) },
                onAddTask = { navController.navigate(Route.AddTask.create()) }
            )
        }
        composable(Route.Inbox.path, arguments = listOf(navArgument("inboxOnly") { type = NavType.BoolType; defaultValue = true })) {
            TaskListScreen(listId = null, title = stringResource(R.string.nav_inbox),
                onTaskClick = { navController.navigate(Route.TaskDetail.create(it)) },
                onAddTask = { navController.navigate(Route.AddTask.create()) })
        }
        composable(Route.AllTasks.path) {
            AllTasksScreen(
                onTaskClick = { navController.navigate(Route.TaskDetail.create(it)) },
                onAddTask = { navController.navigate(Route.AddTask.create()) }
            )
        }
        composable(Route.Completed.path) {
            CompletedScreen(onTaskClick = { navController.navigate(Route.TaskDetail.create(it)) })
        }
        composable(Route.Trash.path) {
            TrashScreen(onTaskClick = { navController.navigate(Route.TaskDetail.create(it)) })
        }
        composable(Route.Tags.path) {
            TagsScreen(onTagClick = { navController.navigate(Route.TagView.create(it)) })
        }
        composable(Route.Habits.path) { HabitsScreen() }
        composable(Route.Rewards.path) { RewardsScreen() }
        composable(Route.Recurrence.path) { RecurrenceScreen() }
        composable(Route.Calendar.path) {
            CalendarScreen(onTaskClick = { navController.navigate(Route.TaskDetail.create(it)) },
                onAddTask = { navController.navigate(Route.AddTask.create(dueDate = it)) },
                onOpenToday = { navController.navigate(Route.Today.path) { launchSingleTop = true } },
                onOpenUpcoming = { navController.navigate(Route.Upcoming.path) { launchSingleTop = true } })
        }
        composable(Route.Insights.path) {
            InsightsScreen(onBack = { navController.popBackStack() },
                onTaskClick = { navController.navigate(Route.TaskDetail.create(it)) })
        }
        composable(Route.Focus.path) { FocusScreen(onBack = { navController.popBackStack() }) }
        composable(Route.Organization.path) {
            OrganizationScreen(
                onBack = { navController.popBackStack() },
                onOpenList = { navController.navigate(Route.TaskListView.create(it)) }
            )
        }
        composable(Route.Search.path) {
            SearchScreen(
                onTaskClick = {
                    navController.navigate(Route.TaskDetail.create(it))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Route.Settings.path) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenReliability = { navController.navigate(Route.Reliability.path) },
                onOpenBackup = { navController.navigate(Route.Backup.path) },
                onOpenCompleted = { navController.navigate(Route.Completed.path) },
                onOpenTrash = { navController.navigate(Route.Trash.path) },
                onOpenTags = { navController.navigate(Route.Tags.path) },
                onStartOnboarding = { navController.navigate(Route.Onboarding.path) },
                onOpenOrganization = { navController.navigate(Route.Organization.path) }
            )
        }
        composable(Route.Reliability.path) {
            ReliabilityScreen(onBack = { navController.popBackStack() })
        }
        composable(Route.Backup.path) {
            BackupScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Route.AddTask.path,
            arguments = listOf(
                navArgument(Route.AddTask.ARG_TASK_ID) {
                    type = NavType.LongType; defaultValue = -1L
                },
                navArgument(Route.AddTask.ARG_LIST_ID) {
                    type = NavType.LongType; defaultValue = -1L
                },
                navArgument(Route.AddTask.ARG_DUE_DATE) {
                    type = NavType.LongType; defaultValue = -1L
                }
            )
        ) { entry ->
            val taskId = entry.arguments?.getLong(Route.AddTask.ARG_TASK_ID) ?: -1L
            val listId = entry.arguments?.getLong(Route.AddTask.ARG_LIST_ID) ?: -1L
            AddTaskScreen(
                taskId = taskId.takeIf { it > 0 },
                initialListId = listId.takeIf { it > 0 },
                onClose = { navController.popBackStack() }
            )
        }
        composable(
            route = Route.TaskDetail.path,
            arguments = listOf(navArgument(Route.TaskDetail.ARG_TASK_ID) { type = NavType.LongType })
        ) { entry ->
            val taskId = entry.arguments?.getLong(Route.TaskDetail.ARG_TASK_ID) ?: -1L
            TaskDetailScreen(
                taskId = taskId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Route.AddTask.create(taskId)) }
            )
        }
        composable(
            route = Route.TaskListView.path,
            arguments = listOf(navArgument("listId") { type = NavType.LongType })
        ) { entry ->
            val listId = entry.arguments?.getLong("listId") ?: -1L
            TaskListScreen(
                listId = listId,
                onTaskClick = { navController.navigate(Route.TaskDetail.create(it)) },
                onAddTask = { navController.navigate(Route.AddTask.create(listId = listId)) }
            )
        }
        composable(
            route = Route.TagView.path,
            arguments = listOf(navArgument("tagId") { type = NavType.LongType })
        ) { entry ->
            val tagId = entry.arguments?.getLong("tagId") ?: -1L
            TaskListScreen(
                listId = null,
                tagId = tagId,
                onTaskClick = { navController.navigate(Route.TaskDetail.create(it)) },
                onAddTask = { navController.navigate(Route.AddTask.create()) }
            )
        }
    }
    }
}
