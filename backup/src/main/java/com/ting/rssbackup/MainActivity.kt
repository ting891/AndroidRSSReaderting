package com.ting.rssbackup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ting.rssbackup.ui.BackupScreen
import com.ting.rssbackup.ui.BackupViewModel
import com.ting.rssbackup.worker.SnapshotWorker
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleSnapshot()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                val viewModel: BackupViewModel = viewModel()
                BackupScreen(viewModel)
            }
        }
    }

    private fun scheduleSnapshot() {
        val request = PeriodicWorkRequestBuilder<SnapshotWorker>(12, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        private const val WORK_NAME = "backup_snapshot_periodic"
    }
}
