package com.yenaly.han1meviewer.util

import com.yenaly.han1meviewer.USER_AGENT

/**
 * 把 Android WebView 自带的 UA 规整成「看起来像普通 Chrome 移动版」的形式：
 * 去掉 `; wv`、`Build/xxx`、`Version/4.0` 这几个 WebView 专属标记，
 * 其余部分（**尤其是 Chrome 主版本号**）原样保留。
 *
 * 为什么必须保留真实版本号：WebView 发出的请求会自动带上 `sec-ch-ua` 系列请求头，
 * 里面的 Chrome 版本取自内核实际版本，**无法**被 [android.webkit.WebSettings.setUserAgentString] 覆盖。
 * 如果 UA 字符串里硬写一个远新于内核的版本（本应用默认 [USER_AGENT] 写的是 Chrome/149），
 * Cloudflare 交叉校验时会发现两者对不上，判定为可疑客户端——表现就是挑战页面能正常加载，
 * 但交互式验证组件迟迟不渲染，用户永远等不到那个勾选框。
 *
 * @param rawUserAgent WebView 未被覆盖时自带的 UA（`settings.userAgentString` 的初始值）
 * @return 规整后的 UA；识别不出 Chrome 内核时退回内置的 [USER_AGENT]
 */
fun normalizeWebViewUserAgent(rawUserAgent: String?): String {
    if (rawUserAgent.isNullOrBlank()) return USER_AGENT
    val normalized = rawUserAgent
        .replace("; wv)", ")")
        .replace(BUILD_TAG_REGEX, "")
        .replace(WEBVIEW_VERSION_TAG_REGEX, "")
        .replace(MULTI_SPACE_REGEX, " ")
        .replace(" )", ")")
        .trim()
    return if (normalized.contains("Chrome/")) normalized else USER_AGENT
}

/**
 * 从 UA 字符串里取出 Chrome 主版本号，取不到返回 null。
 * 只信任正则实际匹配到的结果，匹配不上一律按「无法识别」处理。
 */
fun extractChromeMajorVersion(userAgent: String?): Int? =
    userAgent?.let { CHROME_VERSION_REGEX.find(it)?.groupValues?.getOrNull(1)?.toIntOrNull() }

private val BUILD_TAG_REGEX = Regex("""\sBuild/[^;)]*""")
private val WEBVIEW_VERSION_TAG_REGEX = Regex("""\sVersion/\d+(\.\d+)*""")
private val MULTI_SPACE_REGEX = Regex("""\s+""")
private val CHROME_VERSION_REGEX = Regex("""Chrome/(\d+)[\d.]*""")
