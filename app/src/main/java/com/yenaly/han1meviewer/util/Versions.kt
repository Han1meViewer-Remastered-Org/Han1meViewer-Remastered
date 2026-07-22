package com.yenaly.han1meviewer.util

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.itxca.spannablex.spannable
import com.yenaly.han1meviewer.BuildConfig
import com.yenaly.han1meviewer.FILE_PROVIDER_AUTHORITY
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.github.Latest
import com.yenaly.han1meviewer.worker.HUpdateWorker
import com.yenaly.yenaly_libs.utils.dp
import com.yenaly.yenaly_libs.utils.showShortToast
import java.io.File
import java.util.regex.Pattern

val Context.updateFile: File get() = File(applicationContext.cacheDir, "update.apk")

fun checkNeedUpdate(versionName: String): Boolean {
    // 首选：发布 tag 里携带的 `+versionCode`（如 v1.1.0-remastered+26072210），
    // 直接与本地 BuildConfig.VERSION_CODE 比较，最可靠。
    val latestVersionCode = versionName.substringAfter("+", "").toIntOrNull()
    if (latestVersionCode != null) {
        return BuildConfig.VERSION_CODE < latestVersionCode
    }
    // 兜底：tag 不含 `+versionCode` 时（如历史的 v0.26.0-remastered-1），
    // 退回按语义版本号 x.y.z 比较，避免误报“有更新”。
    return compareSemver(extractSemver(versionName), extractSemver(BuildConfig.VERSION_NAME)) > 0
}

/**
 * 从形如 `v1.1.0-remastered+26072210` / `1.1.0-release+...` 的字符串里提取 `x.y.z`。
 */
private fun extractSemver(raw: String): List<Int> {
    val match = Regex("""(\d+)\.(\d+)\.(\d+)""").find(raw) ?: return emptyList()
    return match.destructured.toList().map { it.toIntOrNull() ?: 0 }
}

/**
 * 语义版本比较：a > b 返回正数，a < b 返回负数，相等返回 0。缺失部分按 0 处理。
 */
private fun compareSemver(a: List<Int>, b: List<Int>): Int {
    val size = maxOf(a.size, b.size)
    for (i in 0 until size) {
        val diff = (a.getOrElse(i) { 0 }) - (b.getOrElse(i) { 0 })
        if (diff != 0) return diff
    }
    return 0
}

suspend fun Context.showUpdateDialog(latest: Latest) {
    val spannable = spannable {
//        getString(R.string.new_version_found).span {
//            style(Typeface.BOLD)
//            relativeSize(1.2f)
//        }
//        newline()
        latest.version.span {
            style(Typeface.BOLD)
            relativeSize(1.2f)
        }
        newline()
        newline()
        getString(R.string.update_content).span {
            style(Typeface.BOLD)
            relativeSize(1.2f)
        }
        newline()
        latest.changelog.text()
    }
    val webUrlPattern = Pattern.compile(
        "(https?://[\\w-]+(\\.[\\w-]+)+([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?)"
    )
    val transformFilter = Linkify.TransformFilter { match, _ ->
        var url = match.group()
        while (url.endsWith(")") || url.endsWith("]") || url.endsWith("}") || url.endsWith(",") || url.endsWith(".")) {
            url = url.dropLast(1)
        }
        url
    }

    val messageView = TextView(this).apply {
        text = spannable
        movementMethod = LinkMovementMethod.getInstance()
        setPadding(32.dp, 16.dp, 32.dp, 0)
        setTextIsSelectable(true)
        Linkify.addLinks(this, webUrlPattern, null, null, transformFilter)
    }

    val dialog = createAlertDialog {
        setTitle(R.string.new_version_found)
        setView(messageView)
        setCancelable(false)
    }
    val res = dialog.await(
        positiveText = getString(R.string.update),
        negativeText = getString(R.string.cancel),
    )
    if (res == AlertDialog.BUTTON_POSITIVE) {
        val update = this.getUpdateIfExists(latest)
        if (update != null) {
            installApkPackage(update)
        } else {
            if (requestPostNotificationPermission()) {
                HUpdateWorker.enqueue(this.applicationContext, latest)
                showShortToast(R.string.update_download_background)
            }
        }
    }
    dialog.dismiss()
}

internal fun Context.getUpdateIfExists(latest: Latest): File? {
    val nodeId = Preferences.updateNodeId
    return updateFile.takeIf { file ->
        !BuildConfig.DEBUG && file.exists() && nodeId.isNotEmpty() && nodeId == latest.nodeId
    }
}

suspend fun Context.installApkPackage(file: File) {
    val canInstall = requestInstallPermission()
    if (canInstall) {
        val uri = FileProvider.getUriForFile(this.applicationContext, FILE_PROVIDER_AUTHORITY, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setDataAndType(uri, "application/vnd.android.package-archive")
        }
        startActivity(intent)
    }
}
