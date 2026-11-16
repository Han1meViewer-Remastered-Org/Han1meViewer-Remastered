package com.yenaly.han1meviewer.logic.network

import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.util.toLoginCookieList
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * 用於管理 Cookie。
 *
 * 响应里带回的 Cookie 只按主机暂存。登录 Cookie 和 cf_clearance 每次请求从偏好设置合并，
 * 同名时以偏好设置为准，避免和暂存表叠成两份。
 */
class HCookieJar : CookieJar {

    companion object {
        @JvmStatic
        val cookieMap: MutableMap<String, MutableList<Cookie>> = java.util.concurrent.ConcurrentHashMap()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val merged = LinkedHashMap<String, Cookie>()
        cookieMap[host]?.forEach { cookie -> merged[cookie.name] = cookie }
        Preferences.loginCookieStateFlow.value.toLoginCookieList(host).forEach { cookie ->
            merged[cookie.name] = cookie
        }
        Preferences.cloudFlareCookieStateFlow.value.toLoginCookieList(host).forEach { cookie ->
            merged[cookie.name] = cookie
        }
        return merged.values.toList()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieMap[url.host] = cookies.toMutableList()
    }
}
