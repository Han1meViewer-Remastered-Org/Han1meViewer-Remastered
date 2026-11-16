package com.yenaly.han1meviewer.logic.network

import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.util.toLoginCookieList
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * 用于管理 Cookie。
 *
 * 偏好设置里的登录 Cookie 和 cf_clearance 作为底。服务器随后下发的同名 Cookie 覆盖它们，
 * 否则关注请求会带着已经换掉的旧会话。
 */
class HCookieJar : CookieJar {

    companion object {
        @JvmStatic
        val cookieMap: MutableMap<String, MutableList<Cookie>> = java.util.concurrent.ConcurrentHashMap()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val merged = LinkedHashMap<String, Cookie>()
        Preferences.loginCookieStateFlow.value.toLoginCookieList(host).forEach { cookie ->
            merged[cookie.name] = cookie
        }
        Preferences.cloudFlareCookieStateFlow.value.toLoginCookieList(host).forEach { cookie ->
            merged[cookie.name] = cookie
        }
        cookieMap[host]?.forEach { cookie -> merged[cookie.name] = cookie }
        return merged.values.toList()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val merged = LinkedHashMap<String, Cookie>()
        cookieMap[url.host]?.forEach { cookie -> merged[cookie.name] = cookie }
        cookies.forEach { cookie -> merged[cookie.name] = cookie }
        cookieMap[url.host] = merged.values.toMutableList()
    }
}
