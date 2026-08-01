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
