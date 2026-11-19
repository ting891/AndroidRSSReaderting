package com.ting.rssbackup.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(viewModel: BackupViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()
                ?.use { it.readText() }
        }.onSuccess { text -> if (text != null) viewModel.importText(text) }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("备份与恢复") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    val dto = uiState.dto
                    if (dto == null) {
                        Text("尚无备份数据", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("可从主 App 导出 RSS 备份 JSON，再通过下方“导入 JSON”带入本模块。")
                    } else {
                        Text("最近备份", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("时间：${formatDate(dto.exportTime)}")
                        Text("订阅源：${dto.feeds.size} 个")
                        Text("文章：${dto.articles.size} 篇")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("历史快照：${uiState.snapshotCount} 份")
                }
            }

            Button(
                onClick = { viewModel.snapshot() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("立即生成备份快照")
            }

            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("导入 JSON 备份文件")
            }

            OutlinedButton(
                onClick = {
                    val file = viewModel.exportFile()
                    if (file.exists()) {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(share, "分享备份文件"))
                    } else {
                        viewModel.note("暂无备份文件，请先生成快照或导入 JSON")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("分享当前备份文件")
            }

            uiState.message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun formatDate(time: Long): String {
    if (time <= 0) return "无记录"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(time))
}
