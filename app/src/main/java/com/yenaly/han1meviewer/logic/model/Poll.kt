package com.yenaly.han1meviewer.logic.model

/**
 * 开发路线投票的领域模型。
 */
data class Poll(
    val id: String,
    val title: String,
    val description: String,
    val options: List<PollOption>,
)

data class PollOption(
    val id: String,
    val text: String,
)
