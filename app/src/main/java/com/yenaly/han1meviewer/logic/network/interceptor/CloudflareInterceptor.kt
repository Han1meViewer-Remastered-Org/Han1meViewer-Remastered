package com.yenaly.han1meviewer.logic.network.interceptor

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.yenaly.han1meviewer.ui.activity.CloudflareActivity
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class CloudflareInterceptor(
    private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code == 403 && response.header("cf-mitigated") == "challenge") {
            response.close()
            val url = request.url.toString()

            // 用 CountDownLatch 代替同步锁
            val latch = CountDownLatch(1)
            val succeeded = AtomicBoolean(false)

            // 注册回调，用户验证完成（或取消/关闭）后调用。用列表而不是单个静态字段，
            // 这样多个并发请求各自触发挑战时（CloudflareActivity 是 singleTask，实际只会
            // 有一个实例），全部等待者都能收到通知，而不是被后触发的请求覆盖导致永久卡死。
            val listener: (Boolean) -> Unit = { success ->
                succeeded.set(success)
                latch.countDown()
            }
            CloudflareActivity.addFinishedListener(listener)

            val intent = Intent(context, CloudflareActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra(CloudflareActivity.EXTRA_URL, url)
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context.applicationContext,
                        "cf启动失败错误: ${e.javaClass.simpleName}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                CloudflareActivity.removeFinishedListener(listener)
                return response
            }

            // 等待 WebView 验证完成，带超时兜底：Activity 一直卡在后台/被系统杀掉时
            // 不会永久占用 OkHttp 调度线程。
            val completed = latch.await(CF_WAIT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            if (!completed) {
                CloudflareActivity.removeFinishedListener(listener)
                return response
            }
            if (!succeeded.get()) {
                // 用户主动关闭/取消了验证，不重试请求，直接把原始 403 交还给调用方。
                return response
            }
            return chain.proceed(request)
        }
        return response
    }

    companion object {
        private const val CF_WAIT_TIMEOUT_MINUTES = 5L
    }
}


