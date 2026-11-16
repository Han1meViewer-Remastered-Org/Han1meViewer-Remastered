package com.yenaly.han1meviewer.logic.network

import com.yenaly.han1meviewer.Preferences
import okhttp3.internal.proxy.NullProxySelector
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

/**
 * 受 [EhViewer_CN_SXJ 中 EhProxySelector](https://github.com/xiaojieonly/Ehviewer_CN_SXJ/blob/BiLi_PC_Gamer/app/src/main/java/com/hippo/ehviewer/EhProxySelector.java)
 * 的启发，Han1meViewer 也将使用 [HProxySelector] 来实现代理功能。
 *
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2023/10/07 007 17:32
 */
// #issue-15: 添加系统代理功能
class HProxySelector : ProxySelector() {

    private var delegation: ProxySelector? = null
    private val alternative: ProxySelector = getDefault() ?: NullProxySelector
    private var cachedKey: String? = null
    private var cachedProxies: List<Proxy>? = null

    init {
        updateProxy()
    }

    companion object {
        const val TYPE_DIRECT = 0
        const val TYPE_SYSTEM = 1
        const val TYPE_HTTP = 2
        const val TYPE_SOCKS = 3

        private val ipv4Regex =
            Regex("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")

        fun validateIp(ip: String): Boolean {
            return ipv4Regex.matches(ip)
        }

        fun validatePort(port: Int): Boolean {
            return port in 1..65535
        }

        // WebView 不走 OkHttp 的 ProxySelector，靠这些系统属性。HTTP 和 SOCKS 要用各自的键。
        fun rebuildNetwork() {
            val properties = System.getProperties()
            listOf(
                "proxySet",
                "proxyHost",
                "proxyPort",
                "http.proxyHost",
                "http.proxyPort",
                "https.proxyHost",
                "https.proxyPort",
                "socksProxyHost",
                "socksProxyPort",
            ).forEach { properties.remove(it) }
            val ip = Preferences.proxyIp
            val port = Preferences.proxyPort.toString()
            when (Preferences.proxyType) {
                TYPE_HTTP -> {
                    properties["proxySet"] = true.toString()
                    properties["proxyHost"] = ip
                    properties["proxyPort"] = port
                    properties["http.proxyHost"] = ip
                    properties["http.proxyPort"] = port
                    properties["https.proxyHost"] = ip
                    properties["https.proxyPort"] = port
                }

                TYPE_SOCKS -> {
                    properties["socksProxyHost"] = ip
                    properties["socksProxyPort"] = port
                }
            }
        }
    }

    private fun updateProxy() {
        delegation = when (Preferences.proxyType) {
            TYPE_DIRECT -> NullProxySelector
            TYPE_SYSTEM -> alternative
            TYPE_HTTP, TYPE_SOCKS -> null
            else -> NullProxySelector
        }
    }

    override fun select(uri: URI?): MutableList<Proxy> {
        val type = Preferences.proxyType
        val ip = Preferences.proxyIp
        val port = Preferences.proxyPort
        if (type == TYPE_HTTP || type == TYPE_SOCKS) {
            val key = "$type|$ip|$port"
            cachedProxies?.takeIf { cachedKey == key }?.let { return it.toMutableList() }
            if (ip.isNotBlank() && port > 0) {
                val socketAddress = InetSocketAddress(InetAddress.getByName(ip), port)
                val proxies = listOf(
                    Proxy(
                        if (type == TYPE_HTTP) Proxy.Type.HTTP else Proxy.Type.SOCKS,
                        socketAddress
                    )
                )
                cachedKey = key
                cachedProxies = proxies
                return proxies.toMutableList()
            }
        }

        return delegation?.select(uri) ?: alternative.select(uri)
    }

    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
        delegation?.connectFailed(uri, sa, ioe)
    }
}