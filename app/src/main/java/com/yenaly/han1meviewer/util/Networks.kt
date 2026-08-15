package com.yenaly.han1meviewer.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.TelephonyManager
import com.google.common.util.concurrent.ListenableFuture
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.exception.CloudFlareBlockedException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import javax.net.ssl.SSLHandshakeException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <R> ListenableFuture<R>.await(): R {
    // Fast path
    if (isDone) {
        try {
            return get()
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        }
    }
    return suspendCancellableCoroutine { cancellableContinuation ->
        addListener(
            {
                try {
                    cancellableContinuation.resume(get())
                } catch (throwable: Throwable) {
                    val cause = throwable.cause ?: throwable
                    when (throwable) {
                        is java.util.concurrent.CancellationException ->
                            cancellableContinuation.cancel(cause)

                        else -> cancellableContinuation.resumeWithException(cause)
                    }
                }
            },
            DirectExecutor
        )

        cancellableContinuation.invokeOnCancellation {
            cancel(false)
        }
    }
}

/**
 * Suspend extension that allows suspend [Call] inside coroutine.
 */
suspend fun Call.await(): Response {
    return suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }
        })
        continuation.invokeOnCancellation { cancel() }
    }
}

/**
 * Run suspend catching
 *
 * @param block suspend block
 */
inline fun <R> runSuspendCatching(block: () -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (c: CancellationException) {
        throw c
    } catch (e: Throwable) {
        Result.failure(e)
    }
}

private data object DirectExecutor : Executor {

    override fun execute(command: Runnable) {
        command.run()
    }
}

/**
 * 将首页加载异常映射为对应的错误提示字符串资源。
 *
 * 优先根据异常类型判断常见网络问题，必要时回退到异常信息中的关键字匹配。
 *
 * @receiver 首页加载过程中抛出的异常
 * @return 错误提示的字符串资源 ID
 */
fun Throwable.toNetworkErrorMessageRes(): Int {
    val rawMessage = message.orEmpty().lowercase()
    return when {
        this is UnknownHostException ||
                rawMessage.contains("unable to resolve host") ||
                rawMessage.contains("no address associated with hostname") -> {
            R.string.home_error_dns
        }

        this is SocketTimeoutException || rawMessage.contains("timeout") -> {
            R.string.home_error_timeout
        }

        this is SSLHandshakeException ||
                rawMessage.contains("ssl") ||
                rawMessage.contains("certificate") -> {
            R.string.home_error_ssl
        }

        this is ConnectException || rawMessage.contains("failed to connect") -> {
            R.string.home_error_connect
        }

        this is SocketException && rawMessage.contains("connection reset") -> {
            R.string.home_error_connection_interrupted
        }

        rawMessage.contains("connection reset") -> {
            R.string.home_error_connection_reset
        }

        rawMessage.contains("403") -> {
            R.string.home_error_forbidden
        }

        rawMessage.contains("404") -> {
            R.string.home_error_not_found
        }

        rawMessage.contains("500") || rawMessage.contains("502") ||
                rawMessage.contains("503") || rawMessage.contains("504") -> {
            R.string.home_error_server_unavailable
        }

        else -> {
            R.string.home_error_generic
        }
    }
}

/**
 * 判断这个异常是否属于"连接层面直接失败"（DNS 解析失败/连接超时/连接被拒/连接被重置），
 * 而不是"请求送达了目标站点，但站点返回了错误状态码"（403/404/5xx）。
 *
 * 国内大陆网络在无代理/VPN 直连时对被墙站点通常表现为前者（DNS 污染、连接超时、连接重置），
 * 不会真的收到站点返回的 HTTP 响应，因此可以用这个区分作为"是否可能是被墙"的信号之一。
 */
fun Throwable.isConnectionLevelFailure(): Boolean {
    val rawMessage = message.orEmpty().lowercase()
    return this is UnknownHostException || this is SocketTimeoutException ||
            this is ConnectException || this is SocketException ||
            rawMessage.contains("unable to resolve host") ||
            rawMessage.contains("no address associated with hostname") ||
            rawMessage.contains("timeout") ||
            rawMessage.contains("failed to connect") ||
            rawMessage.contains("connection reset")
}

/**
 * 当前设备是否已经启用了代理或 VPN。
 *
 * 覆盖两种情况：应用内手动配置了 HTTP/SOCKS 代理（[Preferences.proxyType]），
 * 或系统当前活跃网络本身就是通过 VPN 接口路由的（不论是本应用的代理还是外部 VPN App）。
 */
private fun isProxyOrVpnActive(context: Context): Boolean {
    val manualProxyConfigured = Preferences.proxyType == HProxySelector.TYPE_HTTP ||
            Preferences.proxyType == HProxySelector.TYPE_SOCKS
    if (manualProxyConfigured) return true

    return runCatching {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetwork ?: return@runCatching false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
    }.getOrDefault(false)
}

/**
 * 粗略判断设备当前是否处于中国大陆网络环境。
 *
 * 优先读取蜂窝网络的注册地信号（[TelephonyManager.getNetworkCountryIso]，仅在有效蜂窝连接时可靠），
 * 拿不到时回退到系统 Locale 的国家代码。两者都只是弱信号，仅用于是否展示这条软性提示，不作为强判断。
 */
private fun isLikelyMainlandChinaNetwork(context: Context): Boolean {
    val networkCountryIso = runCatching {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        telephonyManager?.networkCountryIso.orEmpty()
    }.getOrDefault("")
    if (networkCountryIso.isNotBlank()) {
        return networkCountryIso.equals("cn", ignoreCase = true)
    }
    return Locale.getDefault().country.equals("CN", ignoreCase = true)
}

/**
 * 首页加载异常对应的可展示错误文案。
 *
 * 优先级：
 * 1. [CloudFlareBlockedException]（含 [com.yenaly.han1meviewer.logic.exception.IPBlockedException]）
 *    自带的 [Throwable.message] 已经包含了具体成因（例如日本 IP 被 Cloudflare 拦截的提示），
 *    直接展示，不走后续兜底逻辑。
 * 2. 连接层面直接失败（[isConnectionLevelFailure]）+ 当前既没有开代理/VPN（[isProxyOrVpnActive]）
 *    也大概率处于中国大陆网络（[isLikelyMainlandChinaNetwork]）时，提示用户目标网站在国内大陆
 *    网络下通常需要代理/VPN 才能访问，而不是笼统地说"加载失败请重试"。
 * 3. 其余情况回退到通用的字符串资源映射（[toNetworkErrorMessageRes]）。
 *
 * 另外，只要属于连接层面失败（不管有没有开代理），都会再追加一句可操作的排查建议：
 * 引导用户去「设置 → 网络」开启内置 Hosts 或更换镜像站点。这两项设置正是为了应对
 * DNS 污染 / 主域名被墙这类连不上的情况，但默认关闭，很多用户并不知道它们的存在。
 *
 * @receiver 首页加载过程中抛出的异常
 * @return 可直接展示给用户的错误文案
 */
fun Throwable.toNetworkErrorMessage(context: Context): String {
    val ownMessage = (this as? CloudFlareBlockedException)?.message
    if (!ownMessage.isNullOrBlank()) return ownMessage

    val connectionLevelFailure = isConnectionLevelFailure()
    val primary = when {
        connectionLevelFailure &&
                !isProxyOrVpnActive(context) &&
                isLikelyMainlandChinaNetwork(context) -> {
            context.getString(R.string.china_network_no_proxy_hint)
        }

        else -> context.getString(toNetworkErrorMessageRes())
    }
    if (!connectionLevelFailure) return primary
    return primary + "\n\n" + context.getString(R.string.network_unreachable_settings_hint)
}
