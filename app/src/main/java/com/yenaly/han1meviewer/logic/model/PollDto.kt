package com.yenaly.han1meviewer.logic.model

import kotlinx.serialization.Serializable

/**
 * 开发路线投票的序列化 DTO，对应自托管的 polls.json 结构。
 */
@Serializable
data class PollDto(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val options: List<PollOptionDto> = emptyList(),
    val isActive: Boolean = false,
    /**
     * 投放百分比（0~100），不写默认 100 即全量投放。
     * 由客户端按「设备 ID + 投票 id」确定性抽样决定是否展示，详见 [com.yenaly.han1meviewer.util.Rollout]。
     */
    val rolloutPercentage: Int = 100,
) {
    fun toPoll(): Poll = Poll(
        id = id,
        title = title,
        description = description,
        options = options.map { it.toPollOption() },
    )
}

@Serializable
data class PollOptionDto(
    val id: String = "",
    val text: String = "",
) {
    fun toPollOption(): PollOption = PollOption(id = id, text = text)
}
