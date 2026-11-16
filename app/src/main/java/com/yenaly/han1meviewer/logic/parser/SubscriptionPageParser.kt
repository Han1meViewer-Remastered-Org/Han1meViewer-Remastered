package com.yenaly.han1meviewer.logic.parser

import android.util.Log
import com.yenaly.han1meviewer.logic.model.MySubscriptions
import com.yenaly.han1meviewer.logic.model.SubscriptionItem
import com.yenaly.han1meviewer.logic.model.SubscriptionVideosItem
import com.yenaly.han1meviewer.logic.state.WebsiteState
import org.jsoup.nodes.Element

internal fun parseMySubscriptions(parseBody: Element): WebsiteState<MySubscriptions> {
val maxPage = parseMaxPage(parseBody)
Log.i("getMySubscriptions", "MaxPageList=$maxPage")
val subscriptionsRoot = parseBody.selectFirst(HtmlSelectors.SUBSCRIPTIONS_NAV)
    ?: return WebsiteState.Error(IllegalStateException("找不到 subscriptions-nav"))
val subscriptionsVideosRoot = parseBody.selectFirst(HtmlSelectors.SUBSCRIPTIONS_CONTENT)
    ?: return WebsiteState.Error(IllegalStateException("找不到 subscriptionsVideosRoot"))

// 解析订阅作者
val artists = subscriptionsRoot.select(HtmlSelectors.ARTIST_CARD).mapNotNull { card ->
    try {
        val imgs = card.select("img")
        val avatarSrc = imgs.getOrNull(1)?.absUrl("src") ?: return@mapNotNull null
        val artistName = card.selectFirst("div.card-mobile-title")?.text()?.trim()
            ?: return@mapNotNull null

        SubscriptionItem(
            artistName = artistName,
            avatar = avatarSrc
        )
    } catch (_: Exception) {
        null
    }
}

// 解析订阅视频
val videos = subscriptionsVideosRoot.select(HtmlSelectors.VIDEO_ITEM)
    .mapNotNull { videoCard ->
        try {
            val link =
                videoCard.selectFirst("a[class^=video-link]")?.absUrl("href") ?: return@mapNotNull null
            val videoCode = Regex("""watch\?v=(\d+)""").find(link)?.groupValues?.get(1)
                ?: return@mapNotNull null
            val coverUrl = videoCard.select("img[class^=main-thumb]").getOrNull(0)?.absUrl("src") ?: return@mapNotNull null
            val title = videoCard.attr("title").trim()
            val durationAndViews = videoCard.select(HtmlSelectors.THUMB_CONTAINER)
            val duration = durationAndViews.select(HtmlSelectors.DURATION).text()
            val views = durationAndViews.select(HtmlSelectors.STAT_ITEM).getOrNull(1)?.text()
            val artistAndUploadTime = videoCard.selectFirst("div.subtitle")
                ?.text()?.trim().orEmpty()
            val artistTimeParts = artistAndUploadTime.split("•").map { it.trim() }
            val artist = artistTimeParts.getOrNull(0).orEmpty()
            val uploadTime = artistTimeParts.getOrNull(1).orEmpty()
            val infoBoxes = videoCard.selectFirst(HtmlSelectors.CARD_REVIEWS)
            val reviews = infoBoxes?.ownText()?.trim() ?: ""

            SubscriptionVideosItem(
                title = title,
                coverUrl = coverUrl,
                videoCode = videoCode,
                duration = duration,
                views = views,
                reviews = reviews,
                currentArtist = artist,
                uploadTime = uploadTime
            )
        } catch (_: Exception) {
            null
        }
    }

return WebsiteState.Success(
    MySubscriptions(
        subscriptions = artists,
        subscriptionsVideos = videos,
        maxPage = maxPage
    )
)
}
