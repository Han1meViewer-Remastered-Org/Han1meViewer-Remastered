package com.yenaly.han1meviewer.logic.parser

import com.yenaly.han1meviewer.logic.model.Playlists
import com.yenaly.han1meviewer.logic.state.WebsiteState
import org.jsoup.nodes.Element

internal fun playlists(parseBody: Element): WebsiteState<Playlists> {
    val csrfToken = parseBody.selectFirst(HtmlSelectors.CSRF_TOKEN)?.attr("value")
    val lists = parseBody.getElementsByClass("user-tab-item-wrapper")
    val playlists = mutableListOf<Playlists.Playlist>()
    lists.forEach {
        val listCode = it.selectFirst("a[class=video-link]")?.absUrl("href")?.substringAfter('=')
            .throwIfParseNull("playlists", "listCode")
        val listTitle = it.selectFirst("div[class=title]")?.ownText()
            .throwIfParseNull("playlists", "listTitle")
        val listTotal = it.selectFirst("div[class=stat-item]")?.text()
        val formatedTotal = listTotal?.filter { char -> char.isDigit() }?.toIntOrNull() ?: -1
        val coverUrl = it.select("img[class=main-thumb]").first()?.attr("src")
        playlists += Playlists.Playlist(
            listCode = listCode, title = listTitle, total = formatedTotal, coverUrl = coverUrl
        )
    }
    return WebsiteState.Success(Playlists(playlists = playlists, csrfToken = csrfToken))
}
