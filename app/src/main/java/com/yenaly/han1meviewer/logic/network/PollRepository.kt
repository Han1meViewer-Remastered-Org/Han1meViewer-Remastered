package com.yenaly.han1meviewer.logic.network

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.database
import com.yenaly.han1meviewer.HJson
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.logic.model.Poll
import com.yenaly.han1meviewer.logic.model.PollDto
import com.yenaly.han1meviewer.logic.network.service.HGitHubService
import com.yenaly.han1meviewer.util.Rollout
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.decodeFromString

/**
 * 开发路线投票：配置读取（polls.json，走双源）+ 投票提交（Firebase Realtime Database）。
 */
object PollRepository {

    private const val TAG = "Poll"

    /**
     * ⚠️ 临时应急逻辑：优先查主仓库的 polls.json，失败了才查临时镜像仓库的，
     * 与更新检测/公告拉取保持一致。主账号恢复、临时仓库归档后可移除对
     * [HanimeNetwork.githubServiceBackup] 的回退。
     */
    suspend fun fetchActivePolls(): List<Poll> =
        withGithubFallback { service -> fetchPollsFrom(service) }?.first ?: emptyList()

    private suspend fun fetchPollsFrom(service: HGitHubService): List<Poll>? = runCatching {
        val response = service.getPolls()
        if (!response.isSuccessful) return@runCatching null
        val body = response.body()?.string().orEmpty()
        HJson.decodeFromString<List<PollDto>>(body)
            .filter { it.isActive && Rollout.isInRollout(it.id, it.rolloutPercentage) }
            .map { it.toPoll() }
    }.getOrElse { e ->
        Log.e(TAG, "读取投票配置失败: ${e.message}")
        null
    }

    /**
     * 提交投票。同一设备（[Preferences.pollDeviceId]）对同一 [pollId] 只能成功一次，
     * 重复提交会被 Realtime Database 安全规则（`pollVotes/$pollId/$deviceId` 要求
     * `!data.exists()`）拒绝，返回 false。
     */
    suspend fun submitVote(pollId: String, optionId: String): Boolean {
        val deviceId = Preferences.pollDeviceId
        val db = Firebase.database
        val voteRef = db.getReference("pollVotes/$pollId/$deviceId")
        val resultRef = db.getReference("pollResults/$pollId/$optionId")

        val voteWritten = suspendCancellableCoroutine { cont ->
            voteRef.setValue(optionId)
                .addOnSuccessListener { if (cont.isActive) cont.resumeWith(Result.success(true)) }
                .addOnFailureListener { e ->
                    Log.e(TAG, "写入投票记录失败（可能已投过票）: ${e.message}")
                    if (cont.isActive) cont.resumeWith(Result.success(false))
                }
        }
        if (!voteWritten) return false

        val resultCommitted = suspendCancellableCoroutine { cont ->
            resultRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val current = currentData.getValue(Long::class.java) ?: 0L
                    currentData.value = current + 1
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?,
                ) {
                    if (error != null) {
                        Log.e(TAG, "投票计数更新失败: ${error.message}")
                    }
                    if (cont.isActive) cont.resumeWith(Result.success(committed))
                }
            })
        }
        return resultCommitted
    }
}
