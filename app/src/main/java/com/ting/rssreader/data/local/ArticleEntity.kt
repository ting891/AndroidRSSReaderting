package com.ting.rssreader.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 文章（离线缓存正文与摘要） */
@Entity(
    tableName = "articles",
    foreignKeys = [ForeignKey(
        entity = FeedEntity::class,
        parentColumns = ["id"],
        childColumns = ["feedId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index(value = ["feedId"]),
        Index(value = ["guid"], unique = true)
    ]
)
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val feedId: Long,
    val guid: String,
    val title: String,
    val link: String,
    val author: String = "",
    val pubDate: Long = 0L,
    val summary: String = "",
    val content: String = "",
    val isRead: Boolean = false,
    val isStarred: Boolean = false
)
