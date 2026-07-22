package com.yenaly.han1meviewer.logic.network.interceptor

import com.yenaly.han1meviewer.Preferences
import okhttp3.Interceptor
import okhttp3.Response

object UserAgentInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // 使用有效 UA：手动导入 Cloudflare Cookie 时会带上与之绑定的 UA 覆盖值，
        // 否则为默认 USER_AGENT。
        val request = chain.request().newBuilder().header(
            "User-Agent", Preferences.effectiveUserAgent
        ).build()
        return chain.proceed(request)
    }
}