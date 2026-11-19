package com.ting.rssreader

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ting.rssreader.data.repository.RssRepository
import com.ting.rssreader.worker.RefreshWorker
import java.util.concurrent.TimeUnit

class RssApp : Application() {

    lateinit var repository: RssRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = RssRepository(this)
        schedulePeriodicRefresh()
    }

    private fun schedulePeriodicRefresh() {
        val request = PeriodicWorkRequestBuilder<RefreshWorker>(6, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        private const val WORK_NAME = "rss_periodic_refresh"
    }
}
