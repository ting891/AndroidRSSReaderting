package com.ting.rssreader.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ting.rssreader.data.repository.RssRepository

/** 后台周期刷新所有订阅源，结果落库形成离线缓存 */
class RefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            RssRepository(applicationContext).refreshAll()
            Result.success()
        }.getOrElse { Result.retry() }
    }
}
