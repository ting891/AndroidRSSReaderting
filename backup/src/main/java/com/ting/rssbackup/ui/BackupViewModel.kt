package com.ting.rssbackup.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ting.rssbackup.BackupRepository
import com.ting.rssbackup.model.BackupDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class BackupUiState(
    val dto: BackupDto? = null,
    val snapshotCount: Int = 0,
    val message: String? = null,
    val working: Boolean = false
)

class BackupViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = BackupRepository(app)
    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update {
            it.copy(dto = repo.load(), snapshotCount = repo.listSnapshots().size)
        }
    }

    fun snapshot() {
        viewModelScope.launch {
            _uiState.update { it.copy(working = true) }
            repo.createSnapshot()
            refresh()
            _uiState.update { it.copy(working = false, message = "已生成一份历史快照") }
        }
    }

    fun importText(text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(working = true) }
            repo.importJson(text)
                .onSuccess { dto ->
                    _uiState.update { it.copy(message = "已导入：${dto.feeds.size} 个订阅源，${dto.articles.size} 篇文章") }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(message = "导入失败：${e.message ?: "文件格式错误"}") }
                }
            refresh()
            _uiState.update { it.copy(working = false) }
        }
    }

    fun exportFile(): File = repo.currentBackupFile()

    fun listSnapshots(): List<File> = repo.listSnapshots()

    fun note(msg: String) {
        _uiState.update { it.copy(message = msg) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
