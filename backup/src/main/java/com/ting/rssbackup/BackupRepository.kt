package com.ting.rssbackup

import android.content.Context
import com.google.gson.Gson
import com.ting.rssbackup.model.BackupDto
import java.io.File

/** 备份/恢复仓库：读写约定的 JSON 备份文件与历史快照 */
class BackupRepository(context: Context) {

    private val gson = Gson()
    private val workDir = File(context.getExternalFilesDir(null), "backup").apply { mkdirs() }
    private val backupFile = File(workDir, "backup.json")
    private val snapshotDir = File(workDir, "snapshots").apply { mkdirs() }

    fun load(): BackupDto? {
        if (!backupFile.exists()) return null
        return runCatching {
            gson.fromJson(backupFile.readText(Charsets.UTF_8), BackupDto::class.java)
        }.getOrNull()
    }

    fun save(dto: BackupDto) {
        backupFile.writeText(gson.toJson(dto), Charsets.UTF_8)
    }

    /** 生成一份带时间戳的历史快照 */
    fun createSnapshot(): File {
        val dto = load() ?: BackupDto()
        val snapshot = File(snapshotDir, "snapshot_${System.currentTimeMillis()}.json")
        snapshot.writeText(gson.toJson(dto), Charsets.UTF_8)
        return snapshot
    }

    /** 从导入的 JSON 文本恢复并覆盖当前备份 */
    fun importJson(text: String): Result<BackupDto> = runCatching {
        val dto = gson.fromJson(text, BackupDto::class.java)
        save(dto)
        dto
    }

    fun currentBackupFile(): File = backupFile

    fun listSnapshots(): List<File> =
        snapshotDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
}
