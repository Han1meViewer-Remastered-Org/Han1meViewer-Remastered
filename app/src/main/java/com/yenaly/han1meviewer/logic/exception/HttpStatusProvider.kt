package com.yenaly.han1meviewer.logic.exception

/**
 * 这次 HTTP 请求拿到了响应状态码。
 * 界面上的说明文案可以另写，状态码仍要从这里读出来显示。
 */
interface HttpStatusProvider {
    val httpStatusCode: Int
}
