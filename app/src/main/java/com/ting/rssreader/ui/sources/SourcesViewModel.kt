package com.ting.rssreader.ui.sources

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ting.rssreader.RssApp
import com.ting.rssreader.data.local.FeedEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SourcesUiState(
    val feeds: List<FeedEntity> = emptyList(),
    val message: String? = null,
    val working: Boolean = false
)

class SourcesViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as RssApp).repository
    private val message = MutableStateFlow<String?>(null)
    private val working = MutableStateFlow(false)

    private val feeds = repo.observeFeeds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<SourcesUiState> =
        kotlinx.coroutines.flow.combine(feeds, message, working) { f, m, w ->
            SourcesUiState(f, m, w)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SourcesUiState())

    fun addFeed(url: String) {
        viewModelScope.launch {
            working.value = true
            val result = repo.addFeed(url)
            result.onSuccess { message.value = "已添加：${it.title}" }
                .onFailure { message.value = "添加失败：${it.message ?: "未知错误"}" }
            working.value = false
        }
    }

    fun removeFeed(feed: FeedEntity) {
        viewModelScope.launch {
            repo.removeFeed(feed)
            message.value = "已移除：${feed.title}"
        }
    }

    fun refreshFeed(feedId: Long) {
        viewModelScope.launch {
            working.value = true
            repo.refreshFeed(feedId)
            working.value = false
        }
    }

    fun exportBackup() {
        viewModelScope.launch {
            working.value = true
            runCatching { repo.exportBackupFile() }
                .onSuccess { message.value = "已导出备份文件：${it.name}" }
                .onFailure { message.value = "导出失败：${it.message ?: "未知错误"}" }
            working.value = false
        }
    }

    fun clearMessage() {
        message.value = null
    }
}
