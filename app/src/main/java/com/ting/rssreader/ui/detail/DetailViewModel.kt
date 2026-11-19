package com.ting.rssreader.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ting.rssreader.RssApp
import com.ting.rssreader.data.local.ArticleEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetailViewModel(
    app: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(app) {

    private val repo = (app as RssApp).repository
    private val articleId: Long = savedStateHandle.get<Long>("articleId") ?: -1L

    val article: StateFlow<ArticleEntity?> =
        repo.observeArticle(articleId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        if (articleId > 0) {
            viewModelScope.launch { repo.markRead(articleId) }
        }
    }

    fun toggleStar(starred: Boolean) {
        if (articleId > 0) viewModelScope.launch { repo.toggleStar(articleId, starred) }
    }
}
