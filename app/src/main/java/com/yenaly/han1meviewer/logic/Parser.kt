package com.yenaly.han1meviewer.logic

import com.yenaly.han1meviewer.logic.model.CreatorUploadingItem
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.HanimePreview
import com.yenaly.han1meviewer.logic.model.HanimeVideo
import com.yenaly.han1meviewer.logic.model.HomePage
import com.yenaly.han1meviewer.logic.model.MyListItems
import com.yenaly.han1meviewer.logic.model.MySubscriptions
import com.yenaly.han1meviewer.logic.model.Playlists
import com.yenaly.han1meviewer.logic.model.UserAccount
import com.yenaly.han1meviewer.logic.model.VideoComments
import com.yenaly.han1meviewer.logic.parser.creatorUploadedItems as parseCreatorUploadedItems
import com.yenaly.han1meviewer.logic.parser.creatorUploadingItems as parseCreatorUploadingItems
import com.yenaly.han1meviewer.logic.parser.hanimeSearch as parseHanimeSearch
import com.yenaly.han1meviewer.logic.parser.myListItems as parseMyListItems
import com.yenaly.han1meviewer.logic.parser.myPlayListItems as parseMyPlayListItems
import com.yenaly.han1meviewer.logic.parser.onlineWatchHistoryItems as parseOnlineWatchHistoryItems
import com.yenaly.han1meviewer.logic.parser.parseCommentReplies
import com.yenaly.han1meviewer.logic.parser.parseComments
import com.yenaly.han1meviewer.logic.parser.parseCsrfToken
import com.yenaly.han1meviewer.logic.parser.parseHanimePreview
import com.yenaly.han1meviewer.logic.parser.parseHanimeVideo
import com.yenaly.han1meviewer.logic.parser.parseHomePage
import com.yenaly.han1meviewer.logic.parser.parseMySubscriptions
import com.yenaly.han1meviewer.logic.parser.playlists as parsePlaylists
import com.yenaly.han1meviewer.logic.parser.reportCommentResponse as parseReportCommentResponse
import com.yenaly.han1meviewer.logic.parser.toHtmlBody
import com.yenaly.han1meviewer.logic.parser.userAccountPage as parseUserAccountPage
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import com.yenaly.han1meviewer.logic.state.VideoLoadingState
import com.yenaly.han1meviewer.logic.state.WebsiteState
import okhttp3.ResponseBody

/**
 * 页面解析入口。HTML 从字符串或响应流解析，评论和回复仍先读 JSON 再解析其中的 HTML。
 */
object Parser {

    fun extractTokenFromLoginPage(body: String): String = parseCsrfToken(body.toHtmlBody())

    fun extractTokenFromLoginPage(body: ResponseBody): String = parseCsrfToken(body.toHtmlBody())

    fun homePageVer2(body: String): WebsiteState<HomePage> = parseHomePage(body.toHtmlBody())

    fun homePageVer2(body: ResponseBody): WebsiteState<HomePage> = parseHomePage(body.toHtmlBody())

    fun hanimeSearch(body: String): PageLoadingState<MutableList<HanimeInfo>> =
        parseHanimeSearch(body.toHtmlBody())

    fun hanimeSearch(body: ResponseBody): PageLoadingState<MutableList<HanimeInfo>> =
        parseHanimeSearch(body.toHtmlBody())

    fun hanimeVideoVer2(body: String): VideoLoadingState<HanimeVideo> =
        parseHanimeVideo(body.toHtmlBody())

    fun hanimeVideoVer2(body: ResponseBody): VideoLoadingState<HanimeVideo> =
        parseHanimeVideo(body.toHtmlBody())

    fun hanimePreview(body: String): WebsiteState<HanimePreview> =
        parseHanimePreview(body.toHtmlBody())

    fun hanimePreview(body: ResponseBody): WebsiteState<HanimePreview> =
        parseHanimePreview(body.toHtmlBody())

    fun myListItems(body: String): PageLoadingState<MyListItems<HanimeInfo>> =
        parseMyListItems(body.toHtmlBody())

    fun myListItems(body: ResponseBody): PageLoadingState<MyListItems<HanimeInfo>> =
        parseMyListItems(body.toHtmlBody())

    fun myPlayListItems(body: String): PageLoadingState<MyListItems<HanimeInfo>> =
        parseMyPlayListItems(body.toHtmlBody())

    fun myPlayListItems(body: ResponseBody): PageLoadingState<MyListItems<HanimeInfo>> =
        parseMyPlayListItems(body.toHtmlBody())

    fun onlineWatchHistoryItems(body: String): PageLoadingState<MyListItems<HanimeInfo>> =
        parseOnlineWatchHistoryItems(body.toHtmlBody())

    fun onlineWatchHistoryItems(body: ResponseBody): PageLoadingState<MyListItems<HanimeInfo>> =
        parseOnlineWatchHistoryItems(body.toHtmlBody())

    fun userAccountPage(body: String): WebsiteState<UserAccount> =
        parseUserAccountPage(body.toHtmlBody())

    fun userAccountPage(body: ResponseBody): WebsiteState<UserAccount> =
        parseUserAccountPage(body.toHtmlBody())

    fun creatorUploadedItems(body: String): PageLoadingState<MyListItems<HanimeInfo>> =
        parseCreatorUploadedItems(body.toHtmlBody())

    fun creatorUploadedItems(body: ResponseBody): PageLoadingState<MyListItems<HanimeInfo>> =
        parseCreatorUploadedItems(body.toHtmlBody())

    fun creatorUploadingItems(body: String): PageLoadingState<MyListItems<CreatorUploadingItem>> =
        parseCreatorUploadingItems(body.toHtmlBody())

    fun creatorUploadingItems(body: ResponseBody): PageLoadingState<MyListItems<CreatorUploadingItem>> =
        parseCreatorUploadingItems(body.toHtmlBody())

    fun playlists(body: String): WebsiteState<Playlists> = parsePlaylists(body.toHtmlBody())

    fun playlists(body: ResponseBody): WebsiteState<Playlists> = parsePlaylists(body.toHtmlBody())

    fun getMySubscriptions(body: String): WebsiteState<MySubscriptions> =
        parseMySubscriptions(body.toHtmlBody())

    fun getMySubscriptions(body: ResponseBody): WebsiteState<MySubscriptions> =
        parseMySubscriptions(body.toHtmlBody())

    fun comments(body: String): WebsiteState<VideoComments> = parseComments(body)

    fun commentReply(body: String): WebsiteState<VideoComments> = parseCommentReplies(body)

    fun reportCommentResponse(body: String): WebsiteState<String> = parseReportCommentResponse(body)
}
