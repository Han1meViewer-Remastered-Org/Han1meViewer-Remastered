package com.yenaly.han1meviewer.logic.parser

import android.util.Log
import com.yenaly.han1meviewer.Preferences.isAlreadyLogin
import com.yenaly.han1meviewer.logic.exception.ParseException
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import org.jsoup.nodes.Element

internal fun isLoginStateExpired(userHomePageLink: String, username: String?): Boolean {
    return userHomePageLink.contains("/login") || username.isNullOrBlank()
}

internal fun getString(resId: Int) = com.yenaly.yenaly_libs.utils.applicationContext.getString(resId)

internal fun Element.childOrNull(index: Int): Element? {
    return try {
        child(index)
    } catch (_: IndexOutOfBoundsException) {
        null
    }
}

internal fun <T> T?.throwIfParseNull(funcName: String, varName: String): T = this
    ?: throw ParseException(funcName, varName)

internal fun <T> T?.logIfParseNull(
    funcName: String, varName: String, loginNeeded: Boolean = false,
): T? = also {
    if (it == null) {
        if (loginNeeded) {
            if (isAlreadyLogin) {
                Log.d("Parse::$funcName", "[$varName] is null. 而且處於登入狀態，這有點不正常")
            }
        } else {
            Log.d("Parse::$funcName", "[$varName] is null. 這有點不正常")
        }
    }
}

internal fun parseMaxPage(parseBody: Element): Int {
    return parseBody
        .select(HtmlSelectors.PAGINATION)
        .lastOrNull()
        ?.select(HtmlSelectors.PAGE_LINK)
        ?.mapNotNull {
            Regex("""\?page=(\d+)""").find(it.attr("href"))?.groupValues?.get(1)?.toIntOrNull()
        }
        ?.maxOrNull() ?: 1
}

internal fun extractFormError(parseBody: Element): String? {
    val selectors = listOf(
        ".alert-danger",
        ".invalid-feedback",
        ".text-danger",
        ".help-block",
        ".error-message",
    )
    selectors.forEach { selector ->
        parseBody.select(selector)
            .map { it.text().trim() }
            .firstOrNull { it.isNotBlank() }
            ?.let { return it }
    }
    return null
}

internal fun Element?.extractHanimeInfo(selector: String = HtmlSelectors.HORIZONTAL_CARD): MutableList<HanimeInfo> {
    val resultList = mutableListOf<HanimeInfo>()
    this?.select(selector)?.forEach { item ->
        hanimeNormalItemVer2(item)?.let { hanimeInfo ->
            resultList.add(hanimeInfo)
        }
    }
    return resultList
}

