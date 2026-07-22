package com.yenaly.han1meviewer.ui.screen.settings.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R

/**
 * 手动导入 Cloudflare 验证凭据（cf_clearance + 对应 User-Agent）的对话框。
 *
 * 用于老版本 Android/WebView 与鸿蒙设备无法在应用内完成 CF 挑战时的兜底：
 * 用户在桌面/新版浏览器完成验证后，把 cf_clearance 与其 UA 粘贴进来。
 * cf_clearance 与生成它的 UA 绑定，因此二者必须一起保存与发送。
 *
 * @param initialCookie 已保存的 cf cookie 串（形如 `cf_clearance=xxx`）。
 * @param initialUserAgent 已保存的 UA 覆盖值。
 * @param onConfirm 回调 (cookieInput, uaInput)，由调用方负责写入 Preferences 与重建网络。
 * @param onClear 清除已保存的手动凭据。
 */
@Composable
fun CloudflareManualImportDialog(
    initialCookie: String,
    initialUserAgent: String,
    onConfirm: (cookieInput: String, uaInput: String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var cookieText by rememberSaveable(initialCookie) { mutableStateOf(initialCookie) }
    var uaText by rememberSaveable(initialUserAgent) { mutableStateOf(initialUserAgent) }
    val pasteText = remember(clipboard) { { clipboard.getText()?.text?.trim().orEmpty() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cf_manual_import_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.cf_manual_import_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = cookieText,
                    onValueChange = { cookieText = it },
                    label = { Text(stringResource(R.string.cf_manual_cookie_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    supportingText = { Text(stringResource(R.string.cf_manual_cookie_hint)) },
                )
                OutlinedButton(
                    onClick = { pasteText().takeIf { it.isNotEmpty() }?.let { cookieText = it } },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.cf_paste_from_clipboard))
                }

                OutlinedTextField(
                    value = uaText,
                    onValueChange = { uaText = it },
                    label = { Text(stringResource(R.string.cf_manual_ua_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text(stringResource(R.string.cf_manual_ua_hint)) },
                )
                OutlinedButton(
                    onClick = { pasteText().takeIf { it.isNotEmpty() }?.let { uaText = it } },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.cf_paste_from_clipboard))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = {
                        cookieText = ""
                        uaText = ""
                        onClear()
                    }) {
                        Text(stringResource(R.string.cf_manual_clear))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = cookieText.isNotBlank(),
                onClick = { onConfirm(cookieText.trim(), uaText.trim()) },
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/**
 * 把用户输入规整成 `cf_clearance=VALUE` 形式的 cookie 串。
 * - 已含 `cf_clearance=` 的整串：原样返回（去空白）。
 * - 仅粘贴了值：包装成 `cf_clearance=<value>`。
 */
fun normalizeCloudflareCookieInput(input: String): String {
    val trimmed = input.trim()
    return if (trimmed.contains("cf_clearance=")) trimmed else "cf_clearance=$trimmed"
}
