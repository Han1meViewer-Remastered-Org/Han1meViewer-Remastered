package com.yenaly.han1meviewer.ui.component

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.preview.ComponentPreview

@Composable
fun UsageNoticeDialog(
    visible: Boolean,
    onAccepted: () -> Unit,
    onDeclined: () -> Unit,
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.usage_notice_title)) },
        text = {
            Text(
                text = stringResource(R.string.usage_notice_content),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                textAlign = TextAlign.Start,
            )
        },
        confirmButton = {
            TextButton(onClick = onAccepted) {
                Text(text = stringResource(R.string.usage_notice_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDeclined) {
                Text(stringResource(R.string.usage_notice_decline))
            }
        },
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun UsageNoticeDialogPreview(){
    ComponentPreview {
        UsageNoticeDialog(
            visible = true,
            onAccepted = { },
            onDeclined = { }
        )
    }
}