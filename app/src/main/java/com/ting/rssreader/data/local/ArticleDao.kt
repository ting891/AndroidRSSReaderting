package com.ting.rssreader.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    /** feedId = 0 表示全部 */
    @Query("SELECT * FROM articles WHERE (:feedId = 0 OR feedId = :feedId) ORDER BY pubDate DESC, id DESC")
    fun observeByFeed(feedId: Long): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :id")
    fun observeById(id: Long): Flow<ArticleEntity?>

    @Query("SELECT * FROM articles WHERE title LIKE '%' || :q || '%' OR summary LIKE '%' || :q || '%' OR content LIKE '%' || :q || '%' ORDER BY pubDate DESC")
    fun search(q: String): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE isStarred = 1 ORDER BY pubDate DESC")
    fun observeStarred(): Flow<List<ArticleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(articles: List<ArticleEntity>)

    @Query("UPDATE articles SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE articles SET isStarred = :starred WHERE id = :id")
    suspend fun toggleStar(id: Long, starred: Boolean)

    @Query("SELECT COUNT(*) FROM articles WHERE feedId = :feedId AND isRead = 0")
    suspend fun unreadCount(feedId: Long): Int

    @Query("SELECT * FROM articles")
    suspend fun getAll(): List<ArticleEntity>

    @Query("DELETE FROM articles WHERE feedId = :feedId")
    suspend fun deleteByFeed(feedId: Long)
}
