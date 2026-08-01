package com.yenaly.han1meviewer.logic.model

import kotlinx.serialization.Serializable

/**
 * 公告的序列化 DTO，对应自托管的 announcements.json 结构。
 *
 * 与 [Announcement] 分开定义：后者含 Compose 方法，不便直接标注 @Serializable。
 */
@Serializable
data class AnnouncementDto(
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
) {
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
