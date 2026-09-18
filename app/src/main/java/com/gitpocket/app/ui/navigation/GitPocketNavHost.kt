package com.gitpocket.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gitpocket.app.ui.addrepo.AddRepoScreen
import com.gitpocket.app.ui.editor.EditorScreen
import com.gitpocket.app.ui.files.FileBrowserScreen
import com.gitpocket.app.ui.home.HomeScreen
import com.gitpocket.app.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val ADD_REPO = "add_repo"
    const val SETTINGS = "settings"
    const val REPO = "repo/{repoId}"
    const val FILE = "repo/{repoId}/file?path={path}"
    const val ARG_REPO_ID = "repoId"
    const val ARG_PATH = "path"

    fun repo(repoId: Long) = "repo/$repoId"
    fun file(repoId: Long, path: String) = "repo/$repoId/file?path=${Uri.encode(path)}"
}

@Composable
fun GitPocketNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onAddRepo = { navController.navigate(Routes.ADD_REPO) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenRepo = { id -> navController.navigate(Routes.repo(id)) },
            )
        }
        composable(Routes.ADD_REPO) {
            AddRepoScreen(
                onBack = { navController.popBackStack() },
                onDone = {
                    navController.popBackStack()
                },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.REPO,
            arguments = listOf(navArgument(Routes.ARG_REPO_ID) { type = NavType.LongType }),
        ) { entry ->
            val repoId = entry.arguments?.getLong(Routes.ARG_REPO_ID) ?: return@composable
            FileBrowserScreen(
                repoId = repoId,
                onBack = { navController.popBackStack() },
                onOpenFile = { path -> navController.navigate(Routes.file(repoId, path)) },
            )
        }
        composable(
            route = Routes.FILE,
            arguments = listOf(
                navArgument(Routes.ARG_REPO_ID) { type = NavType.LongType },
                navArgument(Routes.ARG_PATH) {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { entry ->
            val repoId = entry.arguments?.getLong(Routes.ARG_REPO_ID) ?: return@composable
            val path = entry.arguments?.getString(Routes.ARG_PATH) ?: ""
            EditorScreen(repoId = repoId, path = path, onBack = { navController.popBackStack() })
        }
    }
}