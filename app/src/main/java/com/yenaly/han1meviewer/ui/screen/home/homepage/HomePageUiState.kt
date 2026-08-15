package com.yenaly.han1meviewer.ui.screen.home.homepage

import com.yenaly.han1meviewer.logic.model.Announcement

/**
 * 主页 UI 事件集合，用于在主界面（Home）中处理用户交互行为，功能如函数名所写。
 *
 */

sealed interface HomeUiEvent {
    data object OpenDrawer : HomeUiEvent
    data object NavigateToPreview : HomeUiEvent
    data class OpenSearchPage(val query: String = "") : HomeUiEvent
    data class NavigateToSearchAdvanced(val params: Map<String, String>) : HomeUiEvent
    data class OpenVideo(val videoCode: String) : HomeUiEvent
    data class LongPressVideoCopy(val videoCode: String, val videoTitle: String) : HomeUiEvent
    data object ShowExitDialog : HomeUiEvent

    /** 首页因网络连不上而加载失败时，从错误页直接跳到网络设置（内置 Hosts / 镜像站点在那里）。 */
    data object NavigateToNetworkSettings : HomeUiEvent

    data class ShowAnnouncementDialog(val announcement: Announcement) : HomeUiEvent
}