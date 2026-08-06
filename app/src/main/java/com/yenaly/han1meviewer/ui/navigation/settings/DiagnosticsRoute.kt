package com.yenaly.han1meviewer.ui.navigation.settings

import android.content.Context
import android.os.Build
import android.os.StatFs
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.yenaly.han1meviewer.BuildConfig
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.ui.screen.settings.DiagnosticsEntry
import com.yenaly.han1meviewer.ui.screen.settings.DiagnosticsScreen
import com.yenaly.han1meviewer.ui.screen.settings.DiagnosticsSection
import com.yenaly.yenaly_libs.utils.formatFileSizeV2
import com.yenaly.yenaly_libs.utils.showShortToast
import java.io.File

@Composable
fun DiagnosticsRouteScreen() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val copiedText = stringResource(R.string.diagnostics_copied)
    val enabledText = stringResource(R.string.state_enabled)
    val disabledText = stringResource(R.string.state_disabled)
    val unknownText = stringResource(R.string.unknow)

    val sections = remember(context) {
        buildDiagnosticsSections(context, enabledText, disabledText, unknownText)
    }

    DiagnosticsScreen(
        sections = sections,
        onCopyAll = {
            clipboard.setText(AnnotatedString(sections.toPlainText()))
            showShortToast(copiedText)
        },
    )
}

private fun List<DiagnosticsSection>.toPlainText(): String = joinToString("\n\n") { section ->
    buildString {
        appendLine("【${section.title}】")
        section.entries.forEach { entry ->
            appendLine("${entry.label}: ${entry.value}")
        }
    }.trimEnd()
}

private fun buildDiagnosticsSections(
    context: Context,
    enabledText: String,
    disabledText: String,
    unknownText: String,
): List<DiagnosticsSection> {
    fun Boolean.asState() = if (this) enabledText else disabledText

    // 包名/版本号优先用 PackageManager 运行时查询实际安装信息（能反映 applicationIdSuffix
    // 等清单层面的改动），查询失败（理论上不该发生）才退回编译期写死的 BuildConfig 常量。
    val runtimePackageInfo = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }.getOrNull()
    val runtimePackageName = runtimePackageInfo?.packageName ?: BuildConfig.APPLICATION_ID
    val runtimeVersionName = runtimePackageInfo?.versionName ?: BuildConfig.VERSION_NAME
    val runtimeVersionCode = runtimePackageInfo?.let {
        androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(it)
    } ?: BuildConfig.VERSION_CODE.toLong()

    val appSection = DiagnosticsSection(
        title = context.getString(R.string.diagnostics_section_app),
        entries = listOf(
            DiagnosticsEntry(context.getString(R.string.diagnostics_app_version), runtimeVersionName),
            DiagnosticsEntry(
                context.getString(R.string.diagnostics_app_version_code),
                runtimeVersionCode.toString()
            ),
            DiagnosticsEntry(context.getString(R.string.diagnostics_app_commit), BuildConfig.COMMIT_SHA),
            DiagnosticsEntry(context.getString(R.string.diagnostics_app_package), runtimePackageName),
            DiagnosticsEntry(context.getString(R.string.diagnostics_app_build_type), BuildConfig.BUILD_TYPE),
            DiagnosticsEntry(context.getString(R.string.diagnostics_app_build_source), BuildConfig.VERSION_SOURCE),
            DiagnosticsEntry(
                context.getString(R.string.diagnostics_app_update_channel),
                if (Preferences.useCIUpdateChannel) "CI" else "Release",
            ),
        ),
    )

    val deviceSection = DiagnosticsSection(
        title = context.getString(R.string.diagnostics_section_device),
        entries = listOf(
            DiagnosticsEntry(
                context.getString(R.string.diagnostics_device_brand_model),
                "${Build.BRAND} / ${Build.MODEL}"
            ),
            DiagnosticsEntry(context.getString(R.string.diagnostics_device_manufacturer), Build.MANUFACTURER),
            DiagnosticsEntry(
                context.getString(R.string.diagnostics_android_version),
                "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            ),
            DiagnosticsEntry(
                context.getString(R.string.diagnostics_security_patch),
                Build.VERSION.SECURITY_PATCH ?: unknownText,
            ),
            DiagnosticsEntry(context.getString(R.string.diagnostics_cpu_abi), Build.SUPPORTED_ABIS.joinToString()),
            DiagnosticsEntry(context.getString(R.string.diagnostics_device_board), "${Build.BOARD} / ${Build.DEVICE}"),
        ),
    )

    val kernelSection = DiagnosticsSection(
        title = context.getString(R.string.diagnostics_section_kernel),
        entries = listOf(
            DiagnosticsEntry(context.getString(R.string.diagnostics_kernel_version), readKernelVersion(unknownText)),
            DiagnosticsEntry(
                context.getString(R.string.diagnostics_kernel_arch),
                System.getProperty("os.arch") ?: unknownText,
            ),
        ),
    )

    val webViewPackage = runCatching { WebView.getCurrentWebViewPackage() }.getOrNull()
    val webViewUa = runCatching { WebSettings.getDefaultUserAgent(context) }.getOrNull() ?: unknownText
    val webViewSection = DiagnosticsSection(
        title = context.getString(R.string.diagnostics_section_webview),
        entries = listOf(
            DiagnosticsEntry(
                context.getString(R.string.diagnostics_webview_package),
                webViewPackage?.packageName ?: unknownText,
            ),
            DiagnosticsEntry(
                context.getString(R.string.diagnostics_webview_version),
                webViewPackage?.versionName ?: unknownText,
            ),
            DiagnosticsEntry(context.getString(R.string.diagnostics_webview_ua), webViewUa),
            DiagnosticsEntry(context.getString(R.string.diagnostics_effective_ua), Preferences.effectiveUserAgent),
        ),
    )

    val proxySummary = when (Preferences.proxyType) {
        HProxySelector.TYPE_DIRECT -> context.getString(R.string.direct)
        HProxySelector.TYPE_SYSTEM -> context.getString(R.string.system_proxy)
        HProxySelector.TYPE_HTTP -> context.getString(
            R.string.http_proxy, Preferences.proxyIp, Preferences.proxyPort
        )

        HProxySelector.TYPE_SOCKS -> context.getString(
            R.string.socks_proxy, Preferences.proxyIp, Preferences.proxyPort
        )

        else -> context.getString(R.string.direct)
    }
    val networkSection = DiagnosticsSection(
        title = context.getString(R.string.diagnostics_section_network),
        entries = listOf(
            DiagnosticsEntry(context.getString(R.string.diagnostics_current_site), Preferences.baseUrl),
            DiagnosticsEntry(context.getString(R.string.diagnostics_proxy_type), proxySummary),
            DiagnosticsEntry(context.getString(R.string.diagnostics_doh), Preferences.useDoH.asState()),
            DiagnosticsEntry(
                context.getString(R.string.diagnostics_built_in_hosts),
                Preferences.useBuiltInHosts.asState()
            ),
            DiagnosticsEntry(
                context.getString(R.string.diagnostics_custom_mirror),
                if (Preferences.useCustomMirrorSite) Preferences.customMirrorSite else disabledText,
            ),
            DiagnosticsEntry(
                context.getString(R.string.diagnostics_cf_override),
                Preferences.cloudFlareUserAgentOverride.isNotBlank().asState(),
            ),
        ),
    )

    val storageSection = DiagnosticsSection(
        title = context.getString(R.string.diagnostics_section_storage),
        entries = listOf(
            DiagnosticsEntry(
                context.getString(R.string.diagnostics_storage_free),
                runCatching {
                    val stat = StatFs(context.filesDir.path)
                    stat.availableBytes.formatFileSizeV2()
                }.getOrDefault(unknownText),
            ),
        ),
    )

    return listOf(appSection, deviceSection, kernelSection, webViewSection, networkSection, storageSection)
}

/**
 * 读取 Linux 内核版本。优先读 `/proc/version`（多数设备可读，含完整编译信息），
 * 读不到时退回 `System.getProperty("os.version")`（通常只有版本号，无编译信息）。
 */
private fun readKernelVersion(unknownText: String): String {
    val procVersion = runCatching {
        File("/proc/version").readText().trim()
    }.getOrNull()
    if (!procVersion.isNullOrBlank()) return procVersion
    return System.getProperty("os.version") ?: unknownText
}
