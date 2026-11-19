package com.ting.rssreader.ui.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ting.rssreader.RssApp
import com.ting.rssreader.data.local.ArticleEntity
import com.ting.rssreader.data.local.FeedEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FeedListUiState(
    val feeds: List<FeedEntity> = emptyList(),
    val articles: List<ArticleEntity> = emptyList(),
    val selectedFeedId: Long = 0,
    val query: String = "",
    val refreshing: Boolean = false,
    val error: String? = null
)

class FeedListViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as RssApp).repository

    private val selectedFeedId = MutableStateFlow(0L)
    private val query = MutableStateFlow("")
    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    private val feeds: StateFlow<List<FeedEntity>> =
        repo.observeFeeds().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val articles: StateFlow<List<ArticleEntity>> =
        combine(selectedFeedId, query) { feedId, q -> feedId to q }
            .flatMapLatest { (feedId, q) ->
                if (q.isBlank()) repo.observeArticles(feedId) else repo.searchArticles(q.trim())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<FeedListUiState> = combine(
        feeds, articles, selectedFeedId, query, refreshing, error
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        FeedListUiState(
            feeds = values[0] as List<FeedEntity>,
            articles = values[1] as List<ArticleEntity>,
            selectedFeedId = values[2] as Long,
            query = values[3] as String,
            refreshing = values[4] as Boolean,
            error = values[5] as String?
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeedListUiState())

    init {
        viewModelScope.launch {
            repo.seedDefaultsIfEmpty()
            refresh()
        }
    }

    fun selectFeed(id: Long) {
        selectedFeedId.value = id
    }

    fun setQuery(q: String) {
        query.value = q
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            runCatching { repo.refreshAll() }
                .onFailure { error.value = it.message ?: "刷新失败" }
            refreshing.value = false
        }
    }

    fun markRead(articleId: Long) {
        viewModelScope.launch { repo.markRead(articleId) }
    }

    fun toggleStar(articleId: Long, starred: Boolean) {
        viewModelScope.launch { repo.toggleStar(articleId, starred) }
    }
}
