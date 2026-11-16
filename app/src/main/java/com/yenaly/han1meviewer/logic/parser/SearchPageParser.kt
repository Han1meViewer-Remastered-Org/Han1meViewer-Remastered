package com.yenaly.han1meviewer.logic.parser

import android.util.Log
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.toVideoCode
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import org.jsoup.nodes.Element

internal fun hanimeSearch(parseBody: Element): PageLoadingState<MutableList<HanimeInfo>> {
    val allContentsClass =
        parseBody.getElementsByClass(HtmlSelectors.CONTENT_PADDING).firstOrNull()
    val allSimplifiedContentsClass =
        parseBody.getElementsByClass(HtmlSelectors.SIMPLIFIED_WRAPPER).firstOrNull()

    // emit!
    if (allContentsClass != null) {
        return hanimeSearchNormalVer2(allContentsClass)
    } else if (allSimplifiedContentsClass != null) {
        return hanimeSearchSimplified(allSimplifiedContentsClass)
    }
    return PageLoadingState.Success(mutableListOf())
}

internal fun hanimeNormalItemVer2(hanimeSearchItem: Element): HanimeInfo? {
    val title =
        hanimeSearchItem.selectFirst(HtmlSelectors.CARD_TITLE)?.text()?.trim()
            .logIfParseNull("hanimeNormalItemVer2", "title")
    val coverUrl =
        hanimeSearchItem.select("img").getOrNull(0)?.absUrl("src")
            .logIfParseNull("hanimeNormalItemVer2", "coverUrl")
    val videoCode =
        hanimeSearchItem.select("a").getOrNull(0)?.absUrl("href")?.toVideoCode()
            .logIfParseNull("hanimeNormalItemVer2", "videoCode")
    if (title == null || coverUrl == null || videoCode == null) return null
    val durationAndViews = hanimeSearchItem.select(HtmlSelectors.THUMB_CONTAINER)
    val duration = durationAndViews.select(HtmlSelectors.DURATION).text()
    val views = durationAndViews.select(HtmlSelectors.STAT_ITEM).getOrNull(1)?.text()
    val artistAndUploadTime = hanimeSearchItem.selectFirst(HtmlSelectors.CARD_SUBTITLE)
        ?.text()?.trim().orEmpty()
    val artistTimeParts = artistAndUploadTime.split("•").map { it.trim() }
    val artist = artistTimeParts.getOrNull(0).orEmpty()
    val uploadTime = artistTimeParts.getOrNull(1).orEmpty()
    val infoBoxes = hanimeSearchItem.selectFirst(HtmlSelectors.CARD_REVIEWS)
    val reviews = infoBoxes?.ownText()?.trim() ?: ""
    return HanimeInfo(
        title = title,
        coverUrl = coverUrl,
        videoCode = videoCode,
        duration = duration.logIfParseNull("hanimeNormalItemVer2", "duration"),
        currentArtist = artist,
        views = views.logIfParseNull("hanimeNormalItemVer2", "views"),
        uploadTime = uploadTime,
        genre = null,
        itemType = HanimeInfo.NORMAL,
        reviews = reviews
    )
}

// 每一个简化版视频单元
internal fun hanimeSimplifiedItem(hanimeSearchItem: Element): HanimeInfo? {
    val videoCode = hanimeSearchItem.attr("href").toVideoCode()
        .logIfParseNull("hanimeSimplifiedItem", "videoCode")
    val coverUrl = hanimeSearchItem.selectFirst("img")?.attr("src")
        .logIfParseNull("hanimeSimplifiedItem", "coverUrl")
    val title = hanimeSearchItem.selectFirst(HtmlSelectors.SIMPLIFIED_TITLE)?.text()
        .logIfParseNull("hanimeSimplifiedItem", "title")
    if (videoCode == null || coverUrl == null || title == null) return null
    return HanimeInfo(
        title = title,
        coverUrl = coverUrl,
        videoCode = videoCode,
        itemType = HanimeInfo.SIMPLIFIED
    )
}

// 出来后是正常视频单元的页面用这个
internal fun hanimeSearchNormalVer2(
    allContentsClass: Element,
): PageLoadingState<MutableList<HanimeInfo>> {
    val hanimeSearchList = mutableListOf<HanimeInfo>()
    val hanimeSearchItems =
        allContentsClass.select(HtmlSelectors.HORIZONTAL_CARD)
    if (hanimeSearchItems.isEmpty()) {
        return PageLoadingState.NoMoreData
    } else {
        hanimeSearchItems.forEach { hanimeSearchItem ->
            hanimeNormalItemVer2(hanimeSearchItem)?.let(hanimeSearchList::add)
        }
    }
    Log.d("search_result", "$hanimeSearchList")
    return PageLoadingState.Success(hanimeSearchList)
}

// 出来后是简化版视频单元的页面用这个
internal fun hanimeSearchSimplified(
    allSimplifiedContentsClass: Element,
): PageLoadingState<MutableList<HanimeInfo>> {
    val hanimeSearchList = mutableListOf<HanimeInfo>()
    val hanimeSearchItems = allSimplifiedContentsClass.children()
    if (hanimeSearchItems.isEmpty()) {
        return PageLoadingState.NoMoreData
    } else hanimeSearchItems.forEach { hanimeSearchItem ->
        hanimeSimplifiedItem(hanimeSearchItem)?.let(hanimeSearchList::add)
    }
    return PageLoadingState.Success(hanimeSearchList)
}
