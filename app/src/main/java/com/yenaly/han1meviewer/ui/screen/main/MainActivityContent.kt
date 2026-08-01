package com.yenaly.han1meviewer.ui.screen.main

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.exception.CloudFlareBlockedException
import com.yenaly.han1meviewer.logic.model.Announcement
import com.yenaly.han1meviewer.logic.model.Poll
import com.yenaly.han1meviewer.logic.model.github.Latest
import com.yenaly.han1meviewer.logic.network.PollRepository
import com.yenaly.han1meviewer.logic.state.PageState
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.component.AnalyticsConsentDialog
import com.yenaly.han1meviewer.ui.component.PollDialog
import com.yenaly.han1meviewer.ui.component.UpdateDialog
import com.yenaly.han1meviewer.ui.component.UsageNoticeDialog
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.yenaly.han1meviewer.ui.navigation.main.MainDestinationSpec
import com.yenaly.han1meviewer.ui.navigation.main.MainDrawerDestination
import com.yenaly.han1meviewer.ui.navigation.main.MainNavHost
import com.yenaly.han1meviewer.ui.navigation.main.handleMainIntent
import com.yenaly.han1meviewer.ui.navigation.main.navigateDrawerDestination
import com.yenaly.han1meviewer.ui.screen.home.homepage.component.AnnouncementListDialog
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.viewmodel.AppViewModel
import com.yenaly.han1meviewer.ui.screen.home.homepage.HomePageViewModel
import com.yenaly.han1meviewer.util.getUpdateIfExists
import com.yenaly.han1meviewer.util.installApkPackage
import com.yenaly.han1meviewer.util.requestPostNotificationPermission
import com.yenaly.han1meviewer.worker.HUpdateWorker
import com.yenaly.yenaly_libs.utils.showShortToast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun MainActivityContent(
    activity: MainActivity,
    viewModel: HomePageViewModel,
    pendingNavigationRequests: Flow<Intent>,
    showAuthGuard: Boolean,
    onOpenAccount: () -> Unit,
    onLogoutClick: () -> Unit,
    onRequireLogin: () -> Unit,
    onSwitchSiteClick: () -> Unit,
    onNavigateControllerReady: (NavHostController) -> Unit,
) {
    HanimeTheme {
        val composeNavController = rememberNavController()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var currentMainDestination by remember { mutableStateOf(MainDestinationSpec.Home) }
        var pendingUpdate by remember { mutableStateOf<Latest?>(null) }
        var showUsageNotice by remember { mutableStateOf(!Preferences.usageNoticeAccepted) }
        var analyticsConsentAnswered by remember { mutableStateOf(Preferences.analyticsConsentAsked) }
        var pollQueue by remember { mutableStateOf<List<Poll>>(emptyList()) }
        var pollToShow by remember { mutableStateOf<Poll?>(null) }
        var pollSubmitting by remember { mutableStateOf(false) }
        var pollFetched by remember { mutableStateOf(false) }
        var announcementDialogList by remember { mutableStateOf<List<Announcement>?>(null) }
        val isDrawerOpen =
            drawerState.currentValue == DrawerValue.Open || drawerState.targetValue == DrawerValue.Open

        val homeState by viewModel.homePageFlow.collectAsStateWithLifecycle()
        val isLoggedIn by Preferences.loginStateFlow.collectAsStateWithLifecycle()
        val headerAvatarUrl = if (isLoggedIn) {
            (homeState as? PageState.Success)?.info?.page?.avatarUrl
        } else {
            null
        }
        val headerUsername = if (isLoggedIn) {
            (homeState as? PageState.Success)?.info?.page?.username
        } else {
            null
        }
        val headerIsLoading = isLoggedIn && homeState is PageState.Loading
        val selectedDrawerDestination = currentMainDestination.drawerDestination

        LaunchedEffect(composeNavController) {
            onNavigateControllerReady(composeNavController)
        }
        LaunchedEffect(Unit) {
            pendingNavigationRequests.collect { intent ->
                composeNavController.handleMainIntent(intent)
            }
        }
        LaunchedEffect(Unit) {
            AppViewModel.pendingUpdateDialog.collect { latest ->
                Preferences.lastUpdatePopupTime = kotlin.time.Clock.System.now().epochSeconds
                pendingUpdate = latest
            }
        }
        LaunchedEffect(viewModel) {
            viewModel.sessionExpiredMessage.collect { event ->
                event.message?.let(::showShortToast) ?: showShortToast(event.fallbackResId)
            }
        }
        LaunchedEffect(showUsageNotice, analyticsConsentAnswered) {
            if (!showUsageNotice && analyticsConsentAnswered && !pollFetched) {
                pollFetched = true
                pollQueue = PollRepository.fetchActivePolls()
                pollToShow = pollQueue.firstOrNull { !Preferences.isPollSeen(it.id) }
            }
        }
        LaunchedEffect(homeState) {
            if (homeState is PageState.Error) {
                val throwable = (homeState as PageState.Error).throwable
                if (throwable is CloudFlareBlockedException) {
                    Log.e("error", "被屏蔽时的处理")
                }
            }
        }
        MainActivityScaffold(
            drawerState = drawerState,
            drawerEnabled = currentMainDestination.drawerEnabled,
            selectedDestination = selectedDrawerDestination,
            avatarUrl = headerAvatarUrl,
            username = headerUsername,
            isLoggedIn = isLoggedIn,
            isLoading = headerIsLoading,
            currentSite = Preferences.baseUrl,
            onAvatarClick = {
                if (isLoggedIn) {
                    scope.launch { drawerState.close() }
                    onOpenAccount()
                } else {
                    onRequireLogin()
                }
            },
            onAvatarLongClick = {
                onLogoutClick()
            },
            onSwitchSiteClick = onSwitchSiteClick,
            onDrawerItemSelected = { destination ->
                if (destination == MainDrawerDestination.Announcement) {
                    scope.launch { drawerState.close() }
                    scope.launch {
                        val list = viewModel.fetchAnnouncementsForViewing()
                        if (list.isEmpty()) {
                            showShortToast(R.string.no_announcement_available)
                        } else {
                            announcementDialogList = list
                        }
                    }
                    true
                } else {
                    val handled = composeNavController.navigateDrawerDestination(
                        destination = destination,
                        isLoggedIn = isLoggedIn,
                        onRequireLogin = { showShortToast(R.string.login_first) },
                    )
                    if (handled) {
                        scope.launch { drawerState.close() }
                    }
                    handled
                }
            },
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                MainNavHost(
                    activity = activity,
                    navController = composeNavController,
                    isDrawerOpen = isDrawerOpen,
                    onOpenDrawer = {
                        if (currentMainDestination.drawerEnabled) {
                            scope.launch { drawerState.open() }
                        }
                    },
                    onDestinationChanged = { destination ->
                        currentMainDestination = destination
                    },
                )
                if (showAuthGuard) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f)),
                    )
                }

                pendingUpdate?.let { latest ->
                    UpdateDialog(
                        latest = latest,
                        onDismiss = { pendingUpdate = null },
                        onConfirm = {
                            pendingUpdate = null
                            scope.launch {
                                val file = activity.getUpdateIfExists(latest)
                                if (file != null) {
                                    activity.installApkPackage(file)
                                } else {
                                    if (activity.requestPostNotificationPermission()) {
                                        HUpdateWorker.enqueue(activity.applicationContext, latest)
                                        showShortToast(R.string.update_download_background)
                                    }
                                }
                            }
                        },
                    )
                }
                UsageNoticeDialog(
                    visible = showUsageNotice,
                    onAccepted = {
                        Preferences.usageNoticeAccepted = true
                        showUsageNotice = false
                    },
                    onDeclined = { activity.finish() },
                )
                AnalyticsConsentDialog(
                    visible = !showUsageNotice && !analyticsConsentAnswered,
                    onAgree = {
                        Preferences.isAnalyticsEnabled = true
                        Preferences.analyticsConsentAsked = true
                        Firebase.analytics.setAnalyticsCollectionEnabled(true)
                        analyticsConsentAnswered = true
                    },
                    onDecline = {
                        Preferences.isAnalyticsEnabled = false
                        Preferences.analyticsConsentAsked = true
                        Firebase.analytics.setAnalyticsCollectionEnabled(false)
                        analyticsConsentAnswered = true
                    },
                )
                pollToShow?.let { poll ->
                    PollDialog(
                        poll = poll,
                        isSubmitting = pollSubmitting,
                        onSubmit = { optionId ->
                            pollSubmitting = true
                            scope.launch {
                                val success = PollRepository.submitVote(poll.id, optionId)
                                Preferences.markPollSeen(poll.id)
                                pollSubmitting = false
                                pollToShow = pollQueue.firstOrNull { !Preferences.isPollSeen(it.id) }
                                showShortToast(
                                    if (success) R.string.poll_vote_thanks
                                    else R.string.poll_vote_failed
                                )
                            }
                        },
                        onDismiss = {
                            Preferences.markPollSeen(poll.id)
                            pollToShow = pollQueue.firstOrNull { !Preferences.isPollSeen(it.id) }
                        },
                    )
                }
                announcementDialogList?.let { list ->
                    AnnouncementListDialog(
                        announcements = list,
                        onDismiss = { announcementDialogList = null },
                    )
                }
            }
        }
    }
}
