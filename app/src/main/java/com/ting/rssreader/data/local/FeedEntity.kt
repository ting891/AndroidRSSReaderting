package com.ting.rssreader.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 订阅源 */
@Entity(
    tableName = "feeds",
    indices = [Index(value = ["url"], unique = true)]
)
data class FeedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val siteLink: String = "",
    val description: String = "",
    val lastFetched: Long = 0L
)
