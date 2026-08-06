package com.yenaly.han1meviewer.logic.network

import com.yenaly.han1meviewer.logic.network.service.HGitHubService
import com.yenaly.han1meviewer.util.runSuspendCatching

/**
 * ⚠️ 临时应急逻辑的公共封装：优先请求主仓库资源，主仓库不可达（账号被暂停期间通常
 * 直接请求失败）时才请求临时镜像仓库。每次调用都从主仓库开始尝试，不缓存"上次哪个
 * 源可用"，主账号一旦恢复，下一次调用即可自动优先命中主仓库。
 * 用于更新检测（[HUpdater]）与投票配置拉取（[PollRepository]）。
 * 主账号恢复、临时仓库归档后，应移除本文件以及调用处对
 * [HanimeNetwork.githubServiceBackup] 的引用。
 */

/**
 * 适用于直接抛异常的 Retrofit 挂起方法（如 [HGitHubService.getLatestVersion]），
 * 也适用于返回 [retrofit2.Response] 需要在 [block] 内自行判断
 * [retrofit2.Response.isSuccessful] 的方法（如 [HGitHubService.getPolls]，失败时
 * [block] 应返回 null）。先尝试 [HanimeNetwork.githubService]，失败（抛异常或返回
 * null）再尝试 [HanimeNetwork.githubServiceBackup]。
 *
 * @return 成功结果与是否来自主仓库（true=主仓库，false=镜像仓库）；两者都失败则为 null。
 */
suspend fun <T : Any> withGithubFallback(
    block: suspend (githubService: HGitHubService) -> T?,
): Pair<T, Boolean>? {
    runSuspendCatching { block(HanimeNetwork.githubService) }
        .getOrNull()
        ?.let { return it to true }
    runSuspendCatching { block(HanimeNetwork.githubServiceBackup) }
        .getOrNull()
        ?.let { return it to false }
    return null
}
