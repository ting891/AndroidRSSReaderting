package com.ting.rssbackup.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ting.rssbackup.BackupRepository

/** 后台定时生成备份快照 */
class SnapshotWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            BackupRepository(applicationContext).createSnapshot()
            Result.success()
        }.getOrElse { Result.retry() }
    }
}
