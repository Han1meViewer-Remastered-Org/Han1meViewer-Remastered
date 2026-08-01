package com.yenaly.han1meviewer.logic.model.github

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2024/03/21 021 08:56
 */
data class Latest(
    val version: String,
    val changelog: String,
    val downloadLink: String,
    /**
     * Node ID for the download link
     *
     * 其实就是个变相的 md5 验证
     */
    val nodeId: String,
    /**
     * ⚠️ 临时应急字段：更新来自哪个源，"primary"（主仓库）或 "backup"（临时镜像仓库）。
     * 主账号恢复、临时仓库归档后可移除。
     */
    val source: String = SOURCE_PRIMARY,
) {
    companion object {
        const val SOURCE_PRIMARY = "primary"
        const val SOURCE_BACKUP = "backup"
    }
}
