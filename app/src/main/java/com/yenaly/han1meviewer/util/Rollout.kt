package com.yenaly.han1meviewer.util

import com.yenaly.han1meviewer.Preferences
import java.util.zip.CRC32

/**
 * 公告 / 投票的按百分比灰度投放抽样。
 *
 * `announcements.json` 与 `polls.json` 是放在 GitHub 上的静态文件，所有设备拉到的是同一份内容，
 * 没有服务端可以按设备分发。所以「一部分设备可见、另一部分不可见」只能由客户端自己决定要不要展示：
 * 用本机已有的匿名设备 ID（[Preferences.pollDeviceId]）拼上条目自身的 key 算一个哈希，
 * 映射到 0~99 的桶里，落在投放百分比区间内的设备才展示。
 *
 * 这个抽样是**设备级**而非「每次随机」的：同一台设备对同一条目算出来的桶号永远相同，
 * 不会出现这次看得到、下次看不到的闪烁；换一条新公告/投票则会重新抽一次，天然做到「每条独立抽样」。
 */
object Rollout {

    /**
     * @param itemKey 条目的稳定标识（投票用 id，公告用 id 或「标题|时间戳」）
     * @param percentage 投放百分比，null 或 >=100 视为全量，<=0 视为不投放
     */
    fun isInRollout(itemKey: String, percentage: Int?): Boolean {
        val pct = percentage ?: FULL_ROLLOUT
        if (pct >= FULL_ROLLOUT) return true
        if (pct <= 0) return false
        return bucketOf(itemKey) < pct
    }

    /** 设备 + 条目 对应的桶号（0~99）。同一组输入结果恒定。 */
    fun bucketOf(itemKey: String): Int {
        val crc = CRC32()
        crc.update("${Preferences.pollDeviceId}:$itemKey".toByteArray())
        return (crc.value % FULL_ROLLOUT).toInt()
    }

    private const val FULL_ROLLOUT = 100
}
