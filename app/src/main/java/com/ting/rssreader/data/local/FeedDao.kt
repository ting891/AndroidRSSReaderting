package com.ting.rssreader.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {

    @Query("SELECT * FROM feeds ORDER BY title ASC")
    fun observeAll(): Flow<List<FeedEntity>>

    @Query("SELECT * FROM feeds WHERE id = :id")
    suspend fun getById(id: Long): FeedEntity?

    @Query("SELECT * FROM feeds WHERE url = :url")
    suspend fun getByUrl(url: String): FeedEntity?

    @Query("SELECT COUNT(*) FROM feeds WHERE url = :url")
    suspend fun countByUrl(url: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(feed: FeedEntity): Long

    @Delete
    suspend fun delete(feed: FeedEntity)

    @Query("SELECT * FROM feeds")
    suspend fun getAll(): List<FeedEntity>
}
