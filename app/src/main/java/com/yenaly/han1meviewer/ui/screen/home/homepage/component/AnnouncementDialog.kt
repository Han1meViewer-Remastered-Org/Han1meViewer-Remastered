package com.yenaly.han1meviewer.ui.screen.home.homepage.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.Announcement
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.preview.fakeAnnouncements
import com.yenaly.han1meviewer.ui.screen.home.homepage.saveImageToGallery
import com.yenaly.yenaly_libs.utils.browse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnnouncementDialog(
    announcementData: Announcement,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showFullScreenImage by remember { mutableStateOf(false) }
    var showSaveImageConfirm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_alert_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = announcementData.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))

                    if (announcementData.timestamp > 0) {
                        Text(
                            text = announcementData.getFormattedDate(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    Text(
                        text = announcementData.getFormatedContent(),
                        style = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    )

                    if (!announcementData.imageUrl.isNullOrBlank()) {
                        Spacer(Modifier.height(16.dp))
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(announcementData.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = { showFullScreenImage = true },
                                    onLongClick = { showSaveImageConfirm = true },
                                ),
                        )
                    }
                }

                if (!announcementData.negativeText.isNullOrBlank() &&
                    !announcementData.positiveText.isNullOrBlank()
                ) {
                    Spacer(Modifier.height(24.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!announcementData.negativeText.isNullOrBlank()) {
                        TextButton(onClick = onDismiss) {
                            Text(text = announcementData.negativeText)
                        }
                        Spacer(Modifier.width(8.dp))
                    }

                    TextButton(onClick = {
                        val url = announcementData.actionUrl
                        if (!url.isNullOrBlank()) context.browse(url)
                        onDismiss()
                    }) {
                        Text(
                            text = announcementData.positiveText
                                ?: stringResource(R.string.i_understand)
                        )
                    }
                }
            }
        }
    }

    if (showFullScreenImage && !announcementData.imageUrl.isNullOrBlank()) {
        Dialog(
            onDismissRequest = { showFullScreenImage = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            AsyncImage(
                model = announcementData.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        onClick = { showFullScreenImage = false },
                        onLongClick = { showSaveImageConfirm = true },
                    ),
            )
        }
    }

    if (showSaveImageConfirm && !announcementData.imageUrl.isNullOrBlank()) {
        val imageUrl = announcementData.imageUrl
        ConfirmDialog(
            visible = true,
            title = stringResource(R.string.save_image_confirm),
            message = "",
            confirmText = stringResource(R.string.sure),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                showSaveImageConfirm = false
                scope.launch(Dispatchers.IO) {
                    saveImageToGallery(context, imageUrl)
                }
            },
            onDismiss = { showSaveImageConfirm = false },
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AnnouncementDialogPreview(){
    ComponentPreview {
        AnnouncementDialog(
            announcementData = fakeAnnouncements[1],
            onDismiss = { }
        )
    }
}
