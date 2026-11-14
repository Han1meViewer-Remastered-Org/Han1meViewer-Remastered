package com.yenaly.han1meviewer.logic.network

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.yenaly.han1meviewer.BuildConfig
import com.yenaly.han1meviewer.FirebaseConstants
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.logic.model.github.CommitComparison
import com.yenaly.han1meviewer.logic.model.github.Latest
import com.yenaly.han1meviewer.logic.network.service.HGitHubService
import com.yenaly.han1meviewer.util.checkNeedUpdate
import com.yenaly.han1meviewer.util.copyTo
import com.yenaly.han1meviewer.util.runSuspendCatching
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import okio.use
import java.io.File
import java.util.zip.ZipFile

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2024/03/21 021 08:28
 */
object HUpdater {

    const val TAG = "HUpdater"

    const val DEFAULT_BRANCH = "main"

    /**
     * Regex to match multiple line feeds to a single line feed
     */
    private val linefeedRegex = Regex("\\n{2,}")

    /**
     * Check for update
     *
     * @param forceCheck force check
     */
    suspend fun checkForUpdate(forceCheck: Boolean = false): Latest? {
        if (forceCheck || Preferences.isUpdateDialogVisible) {
            if (Preferences.useCIUpdateChannel && Firebase.remoteConfig.getBoolean(FirebaseConstants.ENABLE_CI_UPDATE)) {
                return checkForUpdateViaCi()
            } else {
                return checkForUpdateViaReleases()
            }
        }
        return null
    }

    /**
     * CI 通道同样先查主仓库。主账号暂停时 Actions 接口会直接 404，这时改查临时镜像仓库。
     * 两个源都失败才把最后一次异常抛出去，设置页才会显示检查失败，而不是当成“已是最新”。
     */
    private suspend fun checkForUpdateViaCi(): Latest? {
        val primary = runSuspendCatching { loadCiLatest(HanimeNetwork.githubService) }
        if (primary.isSuccess) return primary.getOrThrow()
        val backup = runSuspendCatching { loadCiLatest(HanimeNetwork.githubServiceBackup) }
        if (backup.isSuccess) return backup.getOrThrow()
        throw backup.exceptionOrNull() ?: primary.exceptionOrNull() ?: return null
    }

    private suspend fun loadCiLatest(service: HGitHubService): Latest? {
        val curSha = BuildConfig.COMMIT_SHA
        val workflowRun = service.getWorkflowRuns().workflowRuns.firstOrNull() ?: return null
        val shortSha = workflowRun.headSha.take(7)
        if (shortSha == curSha) return null
        val artifacts = service.getArtifacts(workflowRun.artifactsUrl)
        val changelog = runSuspendCatching {
            service.getCommitComparison(
                curSha = curSha,
                latestSha = shortSha,
            ).commits.toChangelogPrettyString()
        }.getOrNull() ?: workflowRun.title
        return Latest("$shortSha (CI)", changelog, artifacts.downloadLink, artifacts.nodeId)
    }

    /**
     * ⚠️ 临时应急逻辑：优先查主仓库最新发行版，主仓库不可达（账号被暂停期间通常直接
     * 请求失败）时才查临时镜像仓库。每次调用都从主仓库开始尝试，不缓存"上次哪个源可用"，
     * 主账号一旦恢复，下一次检测即可自动优先命中主仓库。
     * 主账号恢复、临时仓库归档后，应移除 catch 分支与 [HanimeNetwork.githubServiceBackup]。
     */
    private suspend fun checkForUpdateViaReleases(): Latest? {
        val ver = withGithubFallback { it.getLatestVersion() } ?: return null

        val (release, isPrimary) = ver
        val source = if (isPrimary) Latest.SOURCE_PRIMARY else Latest.SOURCE_BACKUP
        val isNeeded = checkNeedUpdate(release.tagName)
        if (!isNeeded) return null
        val asset = release.pickBestApkAsset() ?: return null
        return Latest(
            release.tagName, release.body,
            asset.browserDownloadURL,
            asset.nodeID,
            source,
        )
    }

    /**
     * 从 Release 资源里挑选最合适的 APK：
     * 优先匹配当前设备 ABI 的分架构包（体积更小、正确），
     * 找不到则用 universal 通用包，再不行退回第一个 .apk / 第一个资源。
     */
    private fun com.yenaly.han1meviewer.logic.model.github.Release.pickBestApkAsset():
            com.yenaly.han1meviewer.logic.model.github.Release.Asset? {
        if (assets.isEmpty()) return null
        val apkAssets = assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
        if (apkAssets.isEmpty()) return assets.first()
        // 1) 按设备支持的 ABI 顺序匹配分架构包
        for (abi in android.os.Build.SUPPORTED_ABIS) {
            apkAssets.firstOrNull { it.name.contains("-$abi.apk", ignoreCase = true) }
                ?.let { return it }
        }
        // 2) universal 通用包
        apkAssets.firstOrNull { it.name.contains("universal", ignoreCase = true) }
            ?.let { return it }
        // 3) 兜底：第一个 apk
        return apkAssets.first()
    }

    /**
     * Inject update to file
     *
     * @param url update url
     */
    @OptIn(InternalCoroutinesApi::class)
    suspend fun File.injectUpdate(url: String, progress: (suspend (Int, Long, Long) -> Unit)? = null) {
        val res = HanimeNetwork.githubService.request(url)
        // 响应头到了之后，读包体是阻塞的。任务一取消就关掉流，read 才能马上返回。
        currentCoroutineContext()[Job]?.invokeOnCompletion(onCancelling = true) { cause ->
            if (cause != null) res.body()?.close()
        }
        if (url.endsWith("zip")) {
            Log.d(TAG, "Injecting update from zip ($url)")
            val zipFile = File(parentFile, "update.zip")
            parentFile?.mkdirs()
            zipFile.delete()
            res.body()?.use { body ->
                body.byteStream().use { stream ->
                    zipFile.outputStream().use { output ->
                        Log.i(TAG, "content length: ${body.contentLength()}")
                        stream.copyTo(output, body.contentLength(), progress = progress)
                    }
                }
            }
            try {
                zipFile.extractBestApk(this)
            } finally {
                zipFile.delete()
            }
        } else {
            Log.d(TAG, "Injecting update from release ($url)")
            parentFile?.mkdirs()
            this.outputStream().use {
                res.body()?.use { body ->
                    Log.i(TAG, "content length: ${body.contentLength()}")
                    body.byteStream().copyTo(it, body.contentLength(), progress = progress)
                }
            }
        }
    }

    /**
     * CI 产物是包含全部分架构包的 zip。按本机 ABI 挑一个，没有再退回 universal。
     */
    private fun File.extractBestApk(dest: File) {
        ZipFile(this).use { zip ->
            val entries = zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true) }
                .toList()
            val chosenName = selectApkEntry(entries.map { it.name })
                ?: throw java.io.IOException("Update archive has no apk")
            val entry = entries.first { it.name == chosenName }
            Log.i(TAG, "Extracting ${entry.name}")
            zip.getInputStream(entry).use { input ->
                dest.outputStream().use { output -> input.transferTo(output) }
            }
        }
    }

    private fun selectApkEntry(names: List<String>): String? {
        for (abi in android.os.Build.SUPPORTED_ABIS) {
            names.firstOrNull {
                it.substringAfterLast('/').contains("-$abi.apk", ignoreCase = true)
            }?.let { return it }
        }
        return names.firstOrNull { it.contains("universal", ignoreCase = true) } ?: names.firstOrNull()
    }

    /**
     * This function is used to filter out commits that are not authored by the user.
     */
    private val CommitComparison.Commit.CommitDetail.CommitAuthor.isAuthorShouldIgnore: Boolean
        get() = name.contains("dependabot")

    private fun List<CommitComparison.Commit>.toChangelogPrettyString(): String {
        return filterNot { commit ->
            commit.commit.author.isAuthorShouldIgnore
        }.distinct().reversed().joinToString("\n\n") { commit ->
            val message = commit.commit.message.replace(linefeedRegex, "\n")
            "↓ (@${commit.commit.author.name})\n$message"
        }
    }
}