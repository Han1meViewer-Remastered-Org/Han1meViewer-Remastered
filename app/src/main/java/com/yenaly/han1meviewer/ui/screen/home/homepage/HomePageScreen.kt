package com.yenaly.han1meviewer.ui.screen.home.homepage

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.state.PageState
import com.yenaly.han1meviewer.logic.state.dataOrNull
import com.yenaly.han1meviewer.ui.component.PageContent
import com.yenaly.han1meviewer.ui.component.PullRefreshOverlay
import com.yenaly.han1meviewer.ui.component.content.ErrorContent
import com.yenaly.han1meviewer.ui.component.isFirstPageEmpty
import com.yenaly.han1meviewer.ui.component.isFirstPageError
import com.yenaly.han1meviewer.ui.component.isFirstPageLoading
import com.yenaly.han1meviewer.ui.screen.home.homepage.component.HomePageTopBar
import com.yenaly.han1meviewer.ui.screen.rememberRandomLoadingHint
import com.yenaly.han1meviewer.util.isConnectionLevelFailure
import com.yenaly.han1meviewer.util.toNetworkErrorMessage

/**
 * 首页容器屏幕，负责连接 ViewModel 状态与导航回调。
 *
 * @param viewModel 提供首页数据与公告数据的 ViewModel。
 * @param isDrawerOpen 侧边抽屉是否已打开。
 * @param modifier 作用于屏幕根布局的修饰符。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomePageScreen(
    viewModel: HomePageViewModel,
    isDrawerOpen: Boolean,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pageState by viewModel.homePageFlow.collectAsStateWithLifecycle()
    val refreshState = rememberPullToRefreshState()
    var wasRefreshing by remember { mutableStateOf(false) }
    val loadingHint = rememberRandomLoadingHint()
    LaunchedEffect(Unit) {
        if (pageState !is PageState.Success) {
            viewModel.getHomePage(isRefresh = false)
        }
    }

    BackHandler(enabled = !isDrawerOpen) {
        onEvent(HomeUiEvent.ShowExitDialog)
    }

    val isCurrentlyRefreshing = (pageState as? PageState.Success)?.isRefreshing == true

    LaunchedEffect(pageState) {
        val errorState = pageState as? PageState.Error
        if (wasRefreshing && errorState?.cachedInfo != null) {
            Toast.makeText(
                context,
                errorState.throwable.toNetworkErrorMessage(context),
                Toast.LENGTH_SHORT
            ).show()
        }
        wasRefreshing = isCurrentlyRefreshing
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HomePageTopBar(
                onOpenDrawer = { onEvent(HomeUiEvent.OpenDrawer) },
                onSearchClick = { onEvent(HomeUiEvent.OpenSearchPage()) },
                onNavigateToPreview = { onEvent(HomeUiEvent.NavigateToPreview) }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullToRefresh(
                        state = refreshState,
                        isRefreshing = isCurrentlyRefreshing,
                        onRefresh = {
                            viewModel.getHomePage(isRefresh = true)
                        }
                    )
            ) {
                val errorThrowable = (pageState as? PageState.Error)?.throwable
                PageContent(
                    isLoading = pageState.isFirstPageLoading,
                    isError = pageState.isFirstPageError,
                    isEmpty = pageState.isFirstPageError || pageState.isFirstPageEmpty,
                    errorMessage = errorThrowable?.toNetworkErrorMessage(context) ?: "",
                    onRetry = { viewModel.getHomePage(isRefresh = false) },
                    loadingMessage = loadingHint,
                    error = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            ErrorContent(
                                message = errorThrowable?.toNetworkErrorMessage(context).orEmpty(),
                                onRetry = { viewModel.getHomePage(isRefresh = false) },
                                // 只在「压根连不上」时才给这个入口：内置 Hosts / 镜像站点是针对
                                // DNS 污染、主域名被墙这类问题的，站点已经能应答的错误给了反而是噪音。
                                secondaryActionText = if (errorThrowable?.isConnectionLevelFailure() == true) {
                                    stringResource(R.string.go_to_network_settings)
                                } else null,
                                onSecondaryAction = if (errorThrowable?.isConnectionLevelFailure() == true) {
                                    { onEvent(HomeUiEvent.NavigateToNetworkSettings) }
                                } else null,
                            )
                        }
                    },
                ) {
                    val homeData = pageState.dataOrNull

                    if (homeData != null) {
                        AnimatedContent(
                            targetState = homeData,
                            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                            label = "HomeContentAnimation"
                        ) { data ->
                            HomePageContent(
                                data = data,
                                onEvent = onEvent,
                                onCloseAnnouncement = viewModel::dismissAnnouncements,
                            )
                        }
                    }
                }

                PullRefreshOverlay(
                    state = refreshState,
                    isRefreshing = isCurrentlyRefreshing,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            }
        }
    }
}
