package com.ting.rssbackup.model

/** 与主 App 约定的备份文件结构 */
data class BackupDto(
    val exportTime: Long = 0,
    val feeds: List<FeedBackup> = emptyList(),
    val articles: List<ArticleBackup> = emptyList()
)

data class FeedBackup(
    val id: Long = 0,
    val title: String = "",
    val url: String = "",
    val siteLink: String = "",
    val description: String = ""
)

data class ArticleBackup(
    val id: Long = 0,
    val feedId: Long = 0,
    val guid: String = "",
    val title: String = "",
    val link: String = "",
    val author: String = "",
    val pubDate: Long = 0,
    val summary: String = "",
    val content: String = "",
    val isRead: Boolean = false,
    val isStarred: Boolean = false
)
