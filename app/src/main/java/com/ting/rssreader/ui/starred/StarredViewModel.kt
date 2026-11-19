package com.ting.rssreader.ui.starred

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ting.rssreader.RssApp
import com.ting.rssreader.data.local.ArticleEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StarredViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as RssApp).repository

    val articles: StateFlow<List<ArticleEntity>> =
        repo.observeStarred()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markRead(articleId: Long) {
        viewModelScope.launch { repo.markRead(articleId) }
    }

    fun toggleStar(articleId: Long, starred: Boolean) {
        viewModelScope.launch { repo.toggleStar(articleId, starred) }
    }
}
