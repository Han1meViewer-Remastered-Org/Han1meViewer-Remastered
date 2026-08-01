package com.yenaly.han1meviewer.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.Poll
import com.yenaly.han1meviewer.logic.model.PollOption
import com.yenaly.han1meviewer.ui.preview.ComponentPreview

/**
 * 开发路线投票弹窗。纯展示组件，提交/取消的实际处理（写 Firebase、标记已读）由调用方负责。
 */
@Composable
fun PollDialog(
    poll: Poll,
    isSubmitting: Boolean,
    onSubmit: (optionId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedOptionId by remember(poll.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(poll.title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (poll.description.isNotBlank()) {
                    Text(
                        text = poll.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                poll.options.forEach { option: PollOption ->
                    PollOptionRow(
                        text = option.text,
                        selected = selectedOptionId == option.id,
                        onClick = { selectedOptionId = option.id },
                    )
                }
            }
        },
        confirmButton = {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                TextButton(
                    enabled = selectedOptionId != null,
                    onClick = { selectedOptionId?.let(onSubmit) },
                ) {
                    Text(stringResource(R.string.submit))
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !isSubmitting, onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/**
 * 带边框的投票选项行：未选中为 outlineVariant 细边框 + surface 底色，
 * 选中态边框加粗并变为 primary 色，底色带一层浅浅的 primary 容器色，与应用 Material 3 主题呼应。
 */
@Composable
private fun PollOptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        label = "pollOptionBorderColor",
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "pollOptionContainerColor",
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
    ) {
        SettingChoiceItem(
            title = text,
            selected = selected,
            onClick = onClick,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PollDialogPreview() {
    ComponentPreview {
        PollDialog(
            poll = Poll(
                id = "roadmap-2026-08",
                title = "下个版本你最想要哪个新功能？",
                description = "选一个你最想优先看到的方向，我们会参考投票结果安排开发优先级。",
                options = listOf(
                    PollOption("a", "画中画增强"),
                    PollOption("b", "更多播放器内核选项"),
                    PollOption("c", "离线字幕支持"),
                ),
            ),
            isSubmitting = false,
            onSubmit = {},
            onDismiss = {},
        )
    }
}
