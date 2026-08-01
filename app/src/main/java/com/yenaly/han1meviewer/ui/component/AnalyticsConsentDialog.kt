package com.yenaly.han1meviewer.ui.component

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.parseAsHtml
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.preview.ComponentPreview

/**
 * 首次启动时展示的统计授权弹窗，内容与设置里关闭统计时的说明弹窗一致。
 * 用户在明确同意之前，[com.yenaly.han1meviewer.Preferences.isAnalyticsEnabled] 保持关闭。
 */
@Composable
fun AnalyticsConsentDialog(
    visible: Boolean,
    onAgree: () -> Unit,
    onDecline: () -> Unit,
) {
    if (!visible) return

    val message = stringResource(R.string.about_analytics_summary).parseAsHtml()
    val analyticsMessage = remember { message }

    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.about_analytics)) },
        text = {
            AndroidView(
                factory = { ctx ->
                    TextView(ctx).apply {
                        text = analyticsMessage
                        movementMethod = LinkMovementMethod.getInstance()
                        setPadding(0, 0, 0, 0)
                    }
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onAgree) {
                Text(stringResource(R.string.analytics_consent_agree))
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text(stringResource(R.string.deny))
            }
        },
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AnalyticsConsentDialogPreview() {
    ComponentPreview {
        AnalyticsConsentDialog(
            visible = true,
            onAgree = {},
            onDecline = {},
        )
    }
}
