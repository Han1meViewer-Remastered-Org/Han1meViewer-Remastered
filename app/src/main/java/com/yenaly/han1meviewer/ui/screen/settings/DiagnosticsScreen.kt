package com.yenaly.han1meviewer.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SettingInfoItem
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import com.yenaly.han1meviewer.ui.preview.ComponentPreview

/**
 * 一条诊断信息（如"Android 版本" -> "15 (API 35)"）。
 */
data class DiagnosticsEntry(
    val label: String,
    val value: String,
)

/**
 * 一组诊断信息（如"装置与系统"）。
 */
data class DiagnosticsSection(
    val title: String,
    val entries: List<DiagnosticsEntry>,
)

/**
 * 诊断信息页面。纯 UI，不持有 ViewModel。
 *
 * 展示系统/装置/内核/WebView/网络代理等信息，方便用户在回报问题时截图或复制附上。
 *
 * @param sections 分组后的诊断信息
 * @param onCopyAll 点击"复制全部信息"时调用，由调用方负责写入剪贴板
 */
@Composable
fun DiagnosticsScreen(
    sections: List<DiagnosticsSection>,
    onCopyAll: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item {
            OutlinedButton(
                onClick = onCopyAll,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(stringResource(R.string.diagnostics_copy_all))
            }
        }
        sections.forEach { section ->
            item { DiagnosticsGroupTitle(section.title) }
            items(section.entries) { entry ->
                SettingInfoItem(
                    title = entry.label,
                    summary = entry.value,
                )
            }
        }
    }
}

@Composable
private fun DiagnosticsGroupTitle(title: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        HorizontalDivider()
    }
}

@Preview(showBackground = true)
@Composable
private fun DiagnosticsScreenPreview() {
    ComponentPreview {
        DiagnosticsScreen(
            sections = listOf(
                DiagnosticsSection(
                    title = "应用程序",
                    entries = listOf(
                        DiagnosticsEntry("版本名称", "1.1.3-release+26080606"),
                        DiagnosticsEntry("套件名称", "com.han1meviewer.remastered"),
                    ),
                ),
                DiagnosticsSection(
                    title = "装置与系统",
                    entries = listOf(
                        DiagnosticsEntry("品牌／型号", "Google / Pixel 8"),
                        DiagnosticsEntry("Android 版本", "15 (API 35)"),
                    ),
                ),
            ),
            onCopyAll = {},
        )
    }
}
