package com.yenaly.han1meviewer.logic.model

import kotlinx.serialization.Serializable

/**
 * 公告的序列化 DTO，对应自托管的 announcements.json 结构。
 *
 * 与 [Announcement] 分开定义：后者含 Compose 方法，不便直接标注 @Serializable。
 */
@Serializable
data class AnnouncementDto(
    /**
     * 公告的稳定标识，仅用于灰度抽样。不写则退回用「标题|时间戳」当 key（见 [rolloutKey]）——
     * 但那样一旦事后改了标题，抽样结果就会跟着变（原本看不到的设备可能突然看到），
     * 所以要做灰度投放的公告建议显式写一个 id。
     */
    val id: String? = null,
    val title: String = "",
    val content: String = "",
    val positiveText: String? = null,
    val negativeText: String? = null,
    val timestamp: Long = 0,
    val priority: Int = 1,
    val imageUrl: String? = null,
    val isActive: Boolean = false,
    val actionUrl: String? = null,
    /**
     * ⚠️ 临时应急字段：标注这条公告来自主仓库（"primary"）还是临时镜像仓库（"backup"）的
     * announcements.json，由两边仓库各自的文件手动维护。主账号恢复、临时仓库归档后可移除。
     */
    val origin: String? = null,
    /**
     * 投放百分比（0~100），不写默认 100 即全量投放。
     * 由客户端按「设备 ID + [rolloutKey]」确定性抽样决定是否展示，
     * 详见 [com.yenaly.han1meviewer.util.Rollout]。
     */
    val rolloutPercentage: Int = 100,
) {
    /** 灰度抽样用的稳定 key：优先用显式 id，没有就退回「标题|时间戳」。 */
    val rolloutKey: String get() = id ?: "$title|$timestamp"

    fun toAnnouncement(): Announcement = Announcement(
        title = title,
        content = content,
        positiveText = positiveText,
        negativeText = negativeText,
        timestamp = timestamp,
        priority = priority,
        imageUrl = imageUrl,
        isActive = isActive,
        actionUrl = actionUrl,
        origin = origin,
    )
}
