package com.yenaly.han1meviewer.ui.screen.home.homepage.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.ui.component.VideoCardItem
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsPreferenceKeys
import com.yenaly.han1meviewer.ui.component.lazy.LazyRow
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.preview.fakeHomePageVideos
import com.yenaly.han1meviewer.ui.screen.rememberCardResponsiveWidth
import com.yenaly.han1meviewer.ui.theme.SpacingLarge
import com.yenaly.han1meviewer.ui.theme.SpacingNormal

/**
 * 显示横向滚动的视频分类行。
 *
 * @param title 分类标题。
 * @param videos 当前分类下的视频列表。
 * @param onMoreClick 点击更多按钮时调用。
 * @param onVideoClick 点击视频卡片时调用，参数为视频编号。
 * @param onVideoLongClick 长按视频卡片时调用，参数为视频编号和标题。
 * @param modifier 应用于分类行根布局的修饰符。
 */
@Composable
fun CategoryRow(
    title: String,
    videos: List<HanimeInfo>,
    onMoreClick: () -> Unit,
    onVideoClick: (String) -> Unit,
    onVideoLongClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.more),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onMoreClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        val uniqueVideos = videos.distinctBy { it.videoCode }
        val (cardWidth, _) = rememberCardResponsiveWidth()
        val showDateAndViews = Preferences.rememberBooleanPreference(
            SettingsPreferenceKeys.SHOW_DATE_AND_VIEWS_IN_CARD, true
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingNormal),
            contentPadding = PaddingValues(horizontal = SpacingLarge)
        ) {
            items(uniqueVideos, key = { it.videoCode }) { video ->
                VideoCardItem(
                    modifier = Modifier.width(cardWidth),
                    videoItem = video,
                    isHorizontalCard = true,
                    showDateAndViews = showDateAndViews,
                    onClickVideosItem = onVideoClick,
                    onLongClickVideosItem = onVideoLongClick
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "视频分类行", showSystemUi = false)
@Composable
private fun CategoryRowPreview() {
    ComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            CategoryRow(
                title = "最新裏番",
                videos = fakeHomePageVideos,
                onMoreClick = {},
                onVideoClick = {},
                onVideoLongClick = { _, _ -> }
            )
        }
    }
}
