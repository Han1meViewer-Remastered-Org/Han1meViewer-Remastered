package com.yenaly.han1meviewer.logic.parser

import okhttp3.ResponseBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

internal fun String.toHtmlBody(): Element = Jsoup.parse(this).body()

internal fun ResponseBody.toHtmlBody(): Element {
    val charsetName = contentType()?.charset()?.name() ?: "UTF-8"
    return byteStream().use { input ->
        Jsoup.parse(input, charsetName, "").body()
    }
}
