package com.yenaly.han1meviewer.logic.exception

/**
 * 服务器返回了非成功的 HTTP 状态码。
 */
class HttpResponseException(
    override val httpStatusCode: Int,
    message: String,
) : IllegalStateException(message), HttpStatusProvider
