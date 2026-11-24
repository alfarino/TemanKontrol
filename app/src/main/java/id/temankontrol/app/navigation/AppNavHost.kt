package id.temankontrol.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import id.temankontrol.app.R
import id.temankontrol.app.feature.about.AboutScreen
import id.temankontrol.app.feature.backup.BackupScreen
import id.temankontrol.app.feature.consultation.detail.ConsultationDetailScreen
import id.temankontrol.app.feature.consultation.form.ConsultationFormScreen
import id.temankontrol.app.feature.consultation.mode.ConsultationModeScreen
import id.temankontrol.app.feature.followup.FollowUpScreen
import id.temankontrol.app.feature.history.HistoryScreen
import id.temankontrol.app.feature.home.HomeScreen
import id.temankontrol.app.feature.note.NoteScreen
import id.temankontrol.app.feature.notice.NoticeScreen
import id.temankontrol.app.feature.question.QuestionScreen
import id.temankontrol.app.feature.reminder.ReminderSettingsScreen
import id.temankontrol.app.feature.settings.SettingsScreen
import id.temankontrol.app.feature.summary.SummaryPreviewScreen

private data class TopLevelDestination<T : Any>(
    val route: T,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val topLevelDestinations = listOf(
    TopLevelDestination(HomeRoute, R.string.nav_home, Icons.Default.Home),
    TopLevelDestination(HistoryRoute, R.string.nav_history, Icons.Default.DateRange),
    TopLevelDestination(AboutRoute, R.string.nav_about, Icons.Default.Info)
)

@Composable
fun AppNavHost(
    isNoticeAccepted: Boolean,
    deepLinkConsultationId: String? = null,
    deepLinkOpenBackup: Boolean = false,
    onDeepLinkConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(isNoticeAccepted, deepLinkConsultationId, deepLinkOpenBackup) {
        if (!isNoticeAccepted) return@LaunchedEffect
        if (!deepLinkConsultationId.isNullOrBlank()) {
            navController.navigate(ConsultationDetailRoute(id = deepLinkConsultationId)) {
                launchSingleTop = true
            }
            onDeepLinkConsumed()
        } else if (deepLinkOpenBackup) {
            navController.navigate(BackupRoute) {
                launchSingleTop = true
            }
            onDeepLinkConsumed()
        }
    }

    val isTopLevelDestination = topLevelDestinations.any { topDest ->
        currentDestination?.hierarchy?.any { it.hasRoute(topDest.route::class) } == true
    }

    Scaffold(
        bottomBar = {
            if (isTopLevelDestination) {
                NavigationBar {
                    topLevelDestinations.forEach { dest ->
                        val isSelected = currentDestination?.hierarchy?.any { it.hasRoute(dest.route::class) } == true
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = null) },
                            label = { Text(stringResource(dest.labelRes)) }
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isNoticeAccepted) HomeRoute else NoticeRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<NoticeRoute> {
                NoticeScreen(
                    onNavigateToHome = {
                        navController.navigate(HomeRoute) {
                            popUpTo(NoticeRoute) { inclusive = true }
                        }
                    }
                )
            }

            composable<HomeRoute> {
                HomeScreen(
                    onNavigateToDetail = { id ->
                        navController.navigate(ConsultationDetailRoute(id = id))
                    },
                    onNavigateToCreate = {
                        navController.navigate(ConsultationFormRoute())
                    },
                    onNavigateToHistory = {
                        navController.navigate(HistoryRoute)
                    },
                    onNavigateToSettings = {
                        navController.navigate(SettingsRoute)
                    }
                )
            }

            composable<HistoryRoute> {
                HistoryScreen(
                    onNavigateToDetail = { id ->
                        navController.navigate(ConsultationDetailRoute(id = id))
                    }
                )
            }

            composable<AboutRoute> {
                AboutScreen(
                    onNavigateToNotice = {
                        navController.navigate(NoticeRoute)
                    },
                    onNavigateToSettings = {
                        navController.navigate(SettingsRoute)
                    },
                    onNavigateToBackup = {
                        navController.navigate(BackupRoute)
                    },
                    onNavigateToReminders = {
                        navController.navigate(ReminderSettingsRoute)
                    }
                )
            }

            composable<SettingsRoute> {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToBackup = { navController.navigate(BackupRoute) },
                    onNavigateToReminders = { navController.navigate(ReminderSettingsRoute) }
                )
            }

            composable<BackupRoute> {
                BackupScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<ReminderSettingsRoute> {
                ReminderSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<ConsultationDetailRoute> {
                ConsultationDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id ->
                        navController.navigate(ConsultationFormRoute(id = id))
                    },
                    onNavigateToCreateConsultation = {
                        navController.navigate(ConsultationFormRoute())
                    },
                    onNavigateToQuestions = { id ->
                        navController.navigate(QuestionRoute(consultationId = id))
                    },
                    onNavigateToNotes = { id ->
                        navController.navigate(NoteRoute(consultationId = id))
                    },
                    onNavigateToFollowUps = { id ->
                        navController.navigate(FollowUpRoute(consultationId = id))
                    },
                    onNavigateToConsultationMode = { id ->
                        navController.navigate(ConsultationModeRoute(consultationId = id))
                    },
                    onNavigateToSummary = { id ->
                        navController.navigate(SummaryPreviewRoute(consultationId = id))
                    }
                )
            }

            composable<ConsultationFormRoute> {
                ConsultationFormScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSuccess = { newId ->
                        navController.navigate(ConsultationDetailRoute(id = newId)) {
                            popUpTo(HomeRoute)
                        }
                    }
                )
            }

            composable<QuestionRoute> {
                QuestionScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<NoteRoute> {
                NoteScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<FollowUpRoute> {
                FollowUpScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<ConsultationModeRoute> {
                ConsultationModeScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNotes = { id ->
                        navController.navigate(NoteRoute(consultationId = id))
                    },
                    onNavigateToQuestions = { id ->
                        navController.navigate(QuestionRoute(consultationId = id))
                    }
                )
            }

            composable<SummaryPreviewRoute> {
                SummaryPreviewScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
