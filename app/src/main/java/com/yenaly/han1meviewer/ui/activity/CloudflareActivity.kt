package com.yenaly.han1meviewer.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.Preferences.cloudFlareCookie
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.network.ServiceCreator
import com.yenaly.han1meviewer.ui.screen.settings.dialog.CloudflareManualImportDialog
import com.yenaly.han1meviewer.ui.screen.settings.dialog.normalizeCloudflareCookieInput
import com.yenaly.han1meviewer.ui.screen.web.CloudflareScreen
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.util.CookieString
import com.yenaly.han1meviewer.util.extractChromeMajorVersion
import com.yenaly.han1meviewer.util.normalizeWebViewUserAgent
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

class CloudflareActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "request_url"
        private const val LONG_WAIT_THRESHOLD_MS = 20_000L
        private const val CLEARANCE_COOKIE_NAME = "cf_clearance"

        /**
         * 等待验证结果的监听者列表。CloudflareActivity 是 singleTask，同一时间只会有一个实例，
         * 但可能有多个并发网络请求各自触发挑战并注册监听——用列表而不是单个字段，
         * 确保验证完成/取消时所有等待者都能收到通知，不会被后注册的覆盖导致永久卡死。
         */
        private val finishedListeners = CopyOnWriteArrayList<(Boolean) -> Unit>()

        fun addFinishedListener(listener: (Boolean) -> Unit) {
            finishedListeners.add(listener)
        }

        fun removeFinishedListener(listener: (Boolean) -> Unit) {
            finishedListeners.remove(listener)
        }

        /** @param success 验证是否成功（true=拿到 cf_clearance 或手动导入完成，false=用户取消/关闭） */
        private fun notifyFinished(success: Boolean) {
            val toNotify = finishedListeners.toList()
            finishedListeners.clear()
            toNotify.forEach { it(success) }
        }
    }

    private val progressState = mutableIntStateOf(0)
    private val tipTextState = mutableStateOf("")
    private val showManualImportState = mutableStateOf(false)

    private var webView: WebView? = null
    private var requestUrl: String = ""
    private val cookieMgr by lazy { CookieManager.getInstance() }
    private val handler = Handler(Looper.getMainLooper())
    private val pollStartTime = android.os.SystemClock.elapsedRealtime()
    private var longWaitHintShown = false

    /**
     * 进入本页时本地已有的 cf_clearance 值。只有轮询到**与它不同**的新值才算验证通过，
     * 避免拿着一个已经失效的旧 clearance 秒判成功。详见 [clearStaleClearanceCookie]。
     */
    private var baselineClearance: String? = null

    /**
     * 本次挑战实际使用的 UA。与内核真实版本一致（见 [normalizeWebViewUserAgent]），
     * 验证成功后会连同 cookie 一起持久化，保证后续网络请求带的 UA 与 clearance 配对。
     */
    private var challengeUserAgent: String = Preferences.effectiveUserAgent

    /**
     * 定时轮询 cf_clearance cookie：一旦出现**新的**值即视为验证通过。
     * 比读取 document.head 的启发式更稳，对老内核/鸿蒙 WebView 更友好。
     */
    private val pollRunnable = object : Runnable {
        override fun run() {
            if (isFinishing || isDestroyed) return
            if (!trySucceed()) {
                if (!longWaitHintShown &&
                    android.os.SystemClock.elapsedRealtime() - pollStartTime > LONG_WAIT_THRESHOLD_MS
                ) {
                    longWaitHintShown = true
                    tipTextState.value += getString(R.string.cf_verification_taking_long_hint)
                }
                handler.postDelayed(this, 800)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra(EXTRA_URL) ?: run {
            finish()
            return
        }
        requestUrl = url

        // 顺序很重要：先清掉已失效的旧 clearance，再记录基线，最后才创建 WebView 开始加载。
        clearStaleClearanceCookie(url)
        baselineClearance = currentClearance()

        tipTextState.value = getString(R.string.complete_cloudflare_verification_with_warning)

        val composeView = ComposeView(this)
        setContentView(composeView)
        composeView.setContent {
            HanimeTheme {
                CloudflareScreen(
                    progress = progressState.intValue,
                    tipText = tipTextState.value,
                    onClose = { finish() },
                    onReload = { webView?.reload() },
                    onManualImport = { showManualImportState.value = true },
                    webViewFactory = { createWebView(url) },
                )
                if (showManualImportState.value) {
                    CloudflareManualImportDialog(
                        initialCookie = cloudFlareCookie.cookie,
                        initialUserAgent = Preferences.cloudFlareUserAgentOverride,
                        onConfirm = { cookieInput, uaInput ->
                            cloudFlareCookie = CookieString(normalizeCloudflareCookieInput(cookieInput))
                            Preferences.cloudFlareUserAgentOverride = uaInput
                            ServiceCreator.rebuildOkHttpClient()
                            showManualImportState.value = false
                            notifyFinished(true)
                            finish()
                        },
                        onClear = {
                            cloudFlareCookie = CookieString("")
                            Preferences.cloudFlareUserAgentOverride = ""
                            ServiceCreator.rebuildOkHttpClient()
                            showManualImportState.value = false
                        },
                        onDismiss = { showManualImportState.value = false },
                    )
                }
            }
        }

        handler.postDelayed(pollRunnable, 800)
    }

    /**
     * 取出当前 WebView cookie store 里的 cf_clearance 值，没有则返回 null。
     */
    private fun currentClearance(): String? {
        val cookies = cookieMgr.getCookie(requestUrl) ?: return null
        return cookies.split(';')
            .asSequence()
            .map(String::trim)
            .firstOrNull { it.startsWith("$CLEARANCE_COOKIE_NAME=") }
            ?.substringAfter('=')
            ?.takeIf { it.isNotBlank() }
    }

    /**
     * 清掉本地已失效的 cf_clearance。
     *
     * 能走到这个页面，就说明服务端刚刚判定当前 clearance 无效（有效的话根本不会下发挑战）。
     * 而 clearance 是与出口 IP 绑定的：换了 IP（或代理节点漂移）之后旧值必然失效，
     * 但它仍然静静躺在 WebView 的 cookie store 里。
     *
     * 这正是「验证页面加载出来了、验证按钮还没出现，就莫名其妙退回错误页」的成因：
     * 轮询启动后第一次就读到这个残留的旧值，误判为「验证已完成」→ 立刻关页面 →
     * 上层带着同一个失效 cookie 重放请求 → 再次 403 → 用户看到的就是加载失败。
     * 换个 IP 之所以「不用验证就能正常浏览」，也是因为那个 IP 本来就没触发挑战。
     */
    private fun clearStaleClearanceCookie(url: String) {
        runCatching {
            val host = url.toUri().host ?: return
            // 同一个 clearance 可能挂在精确域名或父域上，两种写法都覆盖一遍。
            val domains = buildList {
                add(host)
                add(".$host")
                val parent = host.substringAfter('.', "")
                if (parent.contains('.')) {
                    add(parent)
                    add(".$parent")
                }
            }
            domains.forEach { domain ->
                cookieMgr.setCookie(
                    url,
                    "$CLEARANCE_COOKIE_NAME=; Max-Age=0; Path=/; Domain=$domain",
                )
            }
            cookieMgr.setCookie(url, "$CLEARANCE_COOKIE_NAME=; Max-Age=0; Path=/")
            cookieMgr.flush()
        }
    }

    /**
     * 检查是否已下发**新的** cf_clearance；下发则保存 cookie + 配对 UA 并结束验证。
     *
     * 判定条件是「拿到的值与进入本页时的 [baselineClearance] 不同」而不是「存在 cf_clearance」，
     * 因为后者会被残留的失效旧值直接触发（[clearStaleClearanceCookie] 是尽力而为，
     * 个别老内核上删除可能不生效，这里做第二道保险）。
     */
    private fun trySucceed(): Boolean {
        val clearance = currentClearance() ?: return false
        if (clearance == baselineClearance) return false

        val cookies = cookieMgr.getCookie(requestUrl) ?: return false
        cloudFlareCookie = CookieString(cookies)
        // clearance 与生成它的 UA 绑定，必须让后续网络请求用同一个 UA，否则刚拿到就作废。
        Preferences.cloudFlareUserAgentOverride = challengeUserAgent
        cookieMgr.flush()
        ServiceCreator.rebuildOkHttpClient()
        notifyFinished(true)
        finish()
        return true
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(url: String): WebView {
        return WebView(this).apply {
            webView = this
            val wv = this
            // 取覆盖之前的原始 UA，它带着内核的真实 Chrome 版本号。
            val nativeUserAgent = settings.userAgentString
            challengeUserAgent = normalizeWebViewUserAgent(nativeUserAgent)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                javaScriptCanOpenWindowsAutomatically = true
                useWideViewPort = true
                loadWithOverviewMode = true
                // 不要沿用应用默认的 UA：它把版本号硬写成 Chrome/149，
                // 而 WebView 自动附带的 sec-ch-ua 头报的是内核真实版本，两者对不上会被
                // Cloudflare 判定为可疑客户端，表现为挑战页面加载出来但验证组件永远不渲染。
                userAgentString = challengeUserAgent
            }

            cookieMgr.apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(wv, true)
            }

            updateWebViewVersionTip(nativeUserAgent)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): Boolean = false
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progressState.intValue = newProgress
                }
            }
            loadUrl(url)
        }
    }

    /**
     * 根据内核真实 UA 更新提示文案：版本过低或识别不出时，引导用户走手动导入兜底。
     */
    private fun updateWebViewVersionTip(nativeUserAgent: String?) {
        // 只信任正则实际匹配到的 Chrome 主版本号；匹配不到时不要把整段原始 UA 字符串
        // 当版本号硬切（可能凑巧切出几段数字，误判为"版本达标"而漏掉过低警告），
        // 一律按"无法识别"处理。
        val majorVersion = extractChromeMajorVersion(nativeUserAgent)
        var tip = getString(R.string.complete_cloudflare_verification_with_warning)
        tip += getString(
            R.string.current_webview_version,
            majorVersion?.toString() ?: nativeUserAgent.orEmpty(),
        )
        tip += when {
            majorVersion == null -> getString(R.string.webview_version_unknown)
            majorVersion < 120 -> getString(R.string.webview_version_too_low)
            else -> ""
        }
        tipTextState.value = tip
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        webView?.let { wv ->
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.destroy()
        }
        webView = null
        // 验证成功/手动导入完成时监听者列表已经在 notifyFinished(true) 里清空，这里是空操作；
        // 但如果用户直接关闭/返回或系统杀掉了 Activity 而验证还没完成，
        // 必须以失败通知释放所有等待中的 OkHttp 线程，否则会永久卡死。
        //
        // 注意：只有真的在结束（isFinishing）时才通知失败。配置变更（旋转、深浅色切换、
        // 分屏尺寸变化）导致的重建也会走 onDestroy，此时验证其实还在继续，
        // 一律通知失败会让上层提前拿到 403 退回错误页——这也是"验证中途莫名退回提示页"的成因之一。
        if (isFinishing) {
            notifyFinished(false)
        }
    }

    private fun applyAppLocale(context: Context): Context {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val lang = prefs.getString("app_language", "system") ?: "system"
        val newLocale = when (lang) {
            "zh-rCN" -> Locale.SIMPLIFIED_CHINESE
            "zh" -> Locale.TRADITIONAL_CHINESE
            "en" -> Locale.ENGLISH
            "ja" -> Locale.JAPANESE
            else -> Resources.getSystem().configuration.locales.get(0)
        }
        Locale.setDefault(newLocale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(newLocale)
        return context.createConfigurationContext(config)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(applyAppLocale(newBase))
    }
}
