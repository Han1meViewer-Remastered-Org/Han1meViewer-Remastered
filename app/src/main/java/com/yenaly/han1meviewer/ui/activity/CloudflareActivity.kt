package com.yenaly.han1meviewer.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import java.util.Locale

class CloudflareActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "request_url"
        var onFinished: (() -> Unit)? = null
    }

    private val progressState = mutableIntStateOf(0)
    private val tipTextState = mutableStateOf("")
    private val showManualImportState = mutableStateOf(false)

    private var webView: WebView? = null
    private var requestUrl: String = ""
    private val cookieMgr by lazy { CookieManager.getInstance() }
    private val handler = Handler(Looper.getMainLooper())

    /**
     * 定时轮询 cf_clearance cookie：一旦出现即视为验证通过。
     * 比读取 document.head 的启发式更稳，对老内核/鸿蒙 WebView 更友好。
     */
    private val pollRunnable = object : Runnable {
        override fun run() {
            if (isFinishing || isDestroyed) return
            if (!trySucceed()) {
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
                            onFinished?.invoke()
                            onFinished = null
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
     * 检查 cf_clearance 是否已下发；下发则保存 cookie 并结束验证。
     */
    private fun trySucceed(): Boolean {
        val cookies = cookieMgr.getCookie(requestUrl) ?: return false
        if (cookies.contains("cf_clearance")) {
            cloudFlareCookie = CookieString(cookies)
            cookieMgr.flush()
            onFinished?.invoke()
            onFinished = null
            finish()
            return true
        }
        return false
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(url: String): WebView {
        return WebView(this).apply {
            webView = this
            val wv = this
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                javaScriptCanOpenWindowsAutomatically = true
                // 与网络请求保持一致的 UA（可能被手动导入的 UA 覆盖）。
                userAgentString = Preferences.effectiveUserAgent
            }

            cookieMgr.apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(wv, true)
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): Boolean = false
            }

            evaluateJavascript("navigator.userAgent") { output ->
                val userAgent = output
                    .removeSurrounding("\"")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                val chromePattern = "Chrome/(\\d+\\.\\d+\\.\\d+\\.\\d+)".toRegex()
                val versionCode =
                    chromePattern.find(userAgent)?.groupValues?.getOrNull(1) ?: userAgent
                runOnUiThread {
                    var t = getString(R.string.complete_cloudflare_verification_with_warning)
                    t += getString(R.string.current_webview_version, versionCode)
                    try {
                        val parts = versionCode.split(".").map { it.toIntOrNull() ?: 0 }
                        if (parts.size >= 4) {
                            if (parts[0] < 120) {
                                t += getString(R.string.webview_version_too_low)
                            }
                        } else {
                            t += getString(R.string.webview_version_unknown)
                        }
                    } catch (_: Exception) {
                        t += getString(R.string.version_check_failed)
                    }
                    tipTextState.value = t
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progressState.intValue = newProgress
                }
            }
            loadUrl(url)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        webView = null
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
