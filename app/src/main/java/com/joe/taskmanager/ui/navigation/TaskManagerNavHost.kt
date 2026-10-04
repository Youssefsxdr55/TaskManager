
package com.joe.taskmanager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.joe.taskmanager.ui.screens.ReliabilityScreen
import com.joe.taskmanager.ui.screens.SearchScreen
import com.joe.taskmanager.ui.screens.SettingsScreen
import com.joe.taskmanager.ui.screens.TagsScreen
import com.joe.taskmanager.ui.screens.TaskDetailScreen
import com.joe.taskmanager.ui.screens.TaskListScreen
import com.joe.taskmanager.ui.screens.TodayScreen
import com.joe.taskmanager.ui.screens.TrashScreen
import com.joe.taskmanager.ui.screens.UpcomingScreen

@Composable
fun TaskManagerNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.Today.path,
        modifier = modifier
    ) {
        composable(Route.Onboarding.path) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Route.Today.path) {
                        popUpTo(Route.Onboarding.path) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.Today.path) {
            TodayScreen(
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
                onTaskClick = { navController.navigate(Route.TaskDetail.create(it)) },
                onAddTask = { navController.navigate(Route.AddTask.create()) }
            )
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
                onStartOnboarding = { navController.navigate(Route.Onboarding.path) }
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
