package com.yenaly.han1meviewer.logic.parser

import com.yenaly.han1meviewer.logic.model.CreatorUploadingItem
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.MyListItems
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import org.jsoup.nodes.Element

internal fun myListItems(parseBody: Element): PageLoadingState<MyListItems<HanimeInfo>> {
    val csrfToken = parseBody.selectFirst(HtmlSelectors.CSRF_TOKEN)?.attr("value")
    val desc = parseBody.getElementById("playlist-show-description")?.ownText()
    val allHanimeClass = parseBody.getElementsByClass(HtmlSelectors.HORIZONTAL_ROW).firstOrNull()
    val myListHanimeList = allHanimeClass.extractHanimeInfo(HtmlSelectors.USER_TAB_ITEM)

    return PageLoadingState.Success(
        MyListItems(
            myListHanimeList,
            desc = desc,
            csrfToken = csrfToken
        )
    )
}

internal fun myPlayListItems(parseBody: Element): PageLoadingState<MyListItems<HanimeInfo>> {
    val csrfToken = parseBody.selectFirst(HtmlSelectors.CSRF_TOKEN)?.attr("value")
    val desc = parseBody.select("p.playlist-description").first()?.text()
    val allHanimeClass = parseBody.getElementsByClass(HtmlSelectors.PLAYLIST_VIDEO_LIST).firstOrNull()
    val myListHanimeList = allHanimeClass.extractHanimeInfo(HtmlSelectors.USER_TAB_ITEM)

    return PageLoadingState.Success(
        MyListItems(
            myListHanimeList,
            desc = desc,
            csrfToken = csrfToken
        )
    )
}

internal fun onlineWatchHistoryItems(parseBody: Element): PageLoadingState<MyListItems<HanimeInfo>> {
    val csrfToken = parseBody.selectFirst(HtmlSelectors.CSRF_TOKEN)?.attr("value")
    val allHanimeClass = parseBody.getElementsByClass(HtmlSelectors.HORIZONTAL_ROW).firstOrNull()
    val items = allHanimeClass.extractHanimeInfo(HtmlSelectors.USER_TAB_ITEM)
    return if (items.isEmpty()) {
        PageLoadingState.NoMoreData
    } else {
        PageLoadingState.Success(MyListItems(items, csrfToken = csrfToken))
    }
}


internal fun creatorUploadedItems(parseBody: Element): PageLoadingState<MyListItems<HanimeInfo>> {
    val csrfToken = parseBody.selectFirst(HtmlSelectors.CSRF_TOKEN)?.attr("value")
    val allHanimeClass = parseBody.getElementsByClass(HtmlSelectors.HORIZONTAL_ROW).firstOrNull()
    val items = allHanimeClass.extractHanimeInfo(HtmlSelectors.USER_TAB_ITEM)
    return if (items.isEmpty()) {
        PageLoadingState.NoMoreData
    } else {
        PageLoadingState.Success(MyListItems(items, csrfToken = csrfToken))
    }
}

internal fun creatorUploadingItems(parseBody: Element): PageLoadingState<MyListItems<CreatorUploadingItem>> {
    val csrfToken = parseBody.selectFirst(HtmlSelectors.CSRF_TOKEN)?.attr("value")
    val wrappers = parseBody.select("div.user-tab-item-wrapper")
    if (wrappers.isEmpty()) return PageLoadingState.NoMoreData

    val items = wrappers.mapNotNull { wrapper ->
        val card = wrapper.selectFirst("div.video-item-container") ?: return@mapNotNull null
        val link = card.selectFirst("a.video-link") ?: return@mapNotNull null
        val title = card.selectFirst("div.title")?.text()?.trim() ?: return@mapNotNull null
        val coverUrl = card.selectFirst("img.main-thumb")?.absUrl("src")?.ifBlank {
            card.selectFirst("img.main-thumb")?.attr("src")
        } ?: return@mapNotNull null
        val duration = card.selectFirst("div.duration")?.text()?.trim()
        val subtitleText = card.selectFirst("div.subtitle a")?.text()?.trim().orEmpty()
        val artist = subtitleText.substringBefore("•").trim().ifBlank { null }
        val uploadTime = subtitleText.substringAfter("•", "").trim().ifBlank { null }
        val remoteVideoUrl = link.attr("href")
        val reviewStatus = card.selectFirst("div.stats-container div.stat-item")?.text()?.trim()
            ?: return@mapNotNull null
        val itemId = wrapper.id().substringAfterLast("-").ifBlank { title }

        CreatorUploadingItem(
            title = title,
            coverUrl = coverUrl,
            videoCode = itemId,
            duration = duration,
            currentArtist = artist,
            uploadTime = uploadTime,
            remoteVideoUrl = remoteVideoUrl,
            reviewStatus = reviewStatus,
        )
    }

    return if (items.isEmpty()) {
        PageLoadingState.NoMoreData
    } else {
        PageLoadingState.Success(MyListItems(items, csrfToken = csrfToken))
    }
}
