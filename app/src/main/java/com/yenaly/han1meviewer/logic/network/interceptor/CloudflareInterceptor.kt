package com.yenaly.han1meviewer.logic.network.interceptor

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.yenaly.han1meviewer.ui.activity.CloudflareActivity
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class CloudflareInterceptor(
    private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code != 403 || response.header("cf-mitigated") != "challenge") {
            return response
        }
        // 已经重放过一次仍是挑战页：把这次的响应原样交还，不再打开验证页，也不关闭 body。
        if (request.tag(CfRetried::class.java) != null) {
            return response
        }

        val passed = waitForChallenge(request.url.toString())
        if (!passed) {
            return response
        }
        response.close()
        return chain.proceed(
            request.newBuilder()
                .tag(CfRetried::class.java, CfRetried)
                .build()
        )
    }

    private fun waitForChallenge(url: String): Boolean {
        val waiter = ChallengeWaiter()
        val shouldStart = synchronized(gate) {
            val flight = inFlight
            if (flight == null) {
                val created = InFlightChallenge()
                created.waiters.add(waiter)
                inFlight = created
                true
            } else {
                flight.waiters.add(waiter)
                false
            }
        }
        if (shouldStart) {
            startChallenge(url)
        }
        val completed = waiter.latch.await(CF_WAIT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        return completed && waiter.success.get()
    }

    private fun startChallenge(url: String) {
        val listener: (Boolean) -> Unit = { success -> finishChallenge(success) }
        CloudflareActivity.addFinishedListener(listener)
        val intent = Intent(context, CloudflareActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(CloudflareActivity.EXTRA_URL, url)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            CloudflareActivity.removeFinishedListener(listener)
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    context.applicationContext,
                    "cf启动失败错误: ${e.javaClass.simpleName}",
                    Toast.LENGTH_LONG
                ).show()
            }
            finishChallenge(false)
        }
    }

    private fun finishChallenge(success: Boolean) {
        val flight = synchronized(gate) {
            val current = inFlight
            inFlight = null
            current
        } ?: return
        flight.waiters.forEach { waiter ->
            waiter.success.set(success)
            waiter.latch.countDown()
        }
    }

    private class ChallengeWaiter {
        val latch = CountDownLatch(1)
        val success = AtomicBoolean(false)
    }

    private class InFlightChallenge {
        val waiters = CopyOnWriteArrayList<ChallengeWaiter>()
    }

    private object CfRetried

    companion object {
        private const val CF_WAIT_TIMEOUT_MINUTES = 5L
        private val gate = Any()
        private var inFlight: InFlightChallenge? = null
    }
}
