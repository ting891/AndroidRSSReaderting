package com.ting.rssreader.data.repository

import android.content.Context
import com.google.gson.Gson
import com.ting.rssreader.data.local.AppDatabase
import com.ting.rssreader.data.local.ArticleEntity
import com.ting.rssreader.data.local.FeedEntity
import com.ting.rssreader.data.model.ArticleBackup
import com.ting.rssreader.data.model.BackupDto
import com.ting.rssreader.data.model.FeedBackup
import com.ting.rssreader.data.remote.RssFetcher
import kotlinx.coroutines.flow.Flow

class RssRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.getInstance(appContext)
    private val fetcher = RssFetcher()
    private val gson = Gson()

    fun observeFeeds(): Flow<List<FeedEntity>> = db.feedDao().observeAll()

    fun observeArticles(feedId: Long): Flow<List<ArticleEntity>> =
        db.articleDao().observeByFeed(feedId)

    fun observeArticle(id: Long): Flow<ArticleEntity?> = db.articleDao().observeById(id)

    fun searchArticles(query: String): Flow<List<ArticleEntity>> =
        db.articleDao().search(query)

    /** 刷新指定订阅源：网络拉取 -> XML 解析 -> 落库（离线缓存） */
    suspend fun refreshFeed(feedId: Long): Result<Unit> {
        val feed = db.feedDao().getById(feedId) ?: return Result.failure(IllegalStateException("订阅源不存在"))
        val xml = fetcher.fetchXml(feed.url) ?: return Result.failure(Exception("网络请求失败"))
        val parsed = fetcher.parse(xml) ?: return Result.failure(Exception("RSS 解析失败"))

        db.feedDao().upsert(
            feed.copy(
                title = parsed.title,
                siteLink = parsed.siteLink,
                description = parsed.description,
                lastFetched = System.currentTimeMillis()
            )
        )
        db.articleDao().upsertAll(parsed.articles.map {
            ArticleEntity(
                feedId = feed.id,
                guid = it.guid,
                title = it.title,
                link = it.link,
                author = it.author,
                pubDate = it.pubDate,
                summary = it.summary,
                content = it.content
            )
        })
        return Result.success(Unit)
    }

    /** 添加订阅源并立即拉取一次 */
    suspend fun addFeed(url: String): Result<FeedEntity> {
        val clean = url.trim()
        if (clean.isEmpty()) return Result.failure(IllegalArgumentException("链接不能为空"))
        if (db.feedDao().countByUrl(clean) > 0) return Result.failure(IllegalStateException("已订阅该源"))

        val xml = fetcher.fetchXml(clean) ?: return Result.failure(Exception("网络请求失败"))
        val parsed = fetcher.parse(xml) ?: return Result.failure(Exception("RSS 解析失败"))

        val feedId = db.feedDao().upsert(
            FeedEntity(
                title = parsed.title,
                url = clean,
                siteLink = parsed.siteLink,
                description = parsed.description,
                lastFetched = System.currentTimeMillis()
            )
        )
        db.articleDao().upsertAll(parsed.articles.map {
            ArticleEntity(
                feedId = feedId,
                guid = it.guid,
                title = it.title,
                link = it.link,
                author = it.author,
                pubDate = it.pubDate,
                summary = it.summary,
                content = it.content
            )
        })
        return db.feedDao().getById(feedId)
            ?.let { Result.success(it) }
            ?: Result.failure(Exception("保存失败"))
    }

    suspend fun removeFeed(feed: FeedEntity) = db.feedDao().delete(feed)

    suspend fun markRead(articleId: Long) = db.articleDao().markRead(articleId)

    suspend fun toggleStar(articleId: Long, starred: Boolean) =
        db.articleDao().toggleStar(articleId, starred)

    /** 后台刷新全部源，单源失败不影响其它源 */
    suspend fun refreshAll() {
        val feeds = db.feedDao().getAll()
        feeds.forEach { refreshFeed(it.id) }
    }

    /** 首次启动写入默认订阅源 */
    suspend fun seedDefaultsIfEmpty() {
        if (db.feedDao().getAll().isEmpty()) {
            DEFAULT_FEEDS.forEach { url ->
                runCatching { addFeed(url) }
            }
        }
    }

    /** 导出当前订阅源与文章为 JSON 字符串（供备份/恢复模块交换） */
    suspend fun exportBackupJson(): String {
        val feeds = db.feedDao().getAll().map {
            FeedBackup(it.id, it.title, it.url, it.siteLink, it.description)
        }
        val articles = db.articleDao().getAll().map {
            ArticleBackup(
                it.id, it.feedId, it.guid, it.title, it.link, it.author,
                it.pubDate, it.summary, it.content, it.isRead, it.isStarred
            )
        }
        return gson.toJson(BackupDto(System.currentTimeMillis(), feeds, articles))
    }

    /** 把当前数据导出为备份 JSON 文件，供伴生备份模块读取/交换 */
    suspend fun exportBackupFile(): java.io.File {
        val dir = java.io.File(appContext.getExternalFilesDir(null), "backup").apply { mkdirs() }
        val file = java.io.File(dir, "backup.json")
        file.writeText(exportBackupJson(), Charsets.UTF_8)
        return file
    }

    /** 从备份 JSON 恢复，返回导入文章数 */
    suspend fun importBackupJson(json: String): Result<Int> = runCatching {
        val dto = gson.fromJson(json, BackupDto::class.java)
        val idMap = mutableMapOf<Long, Long>()
        dto.feeds.forEach { fb ->
            val existing = db.feedDao().getByUrl(fb.url)
            val newId = existing?.id ?: db.feedDao().upsert(
                FeedEntity(
                    title = fb.title,
                    url = fb.url,
                    siteLink = fb.siteLink,
                    description = fb.description
                )
            )
            idMap[fb.id] = newId
        }
        val articles = dto.articles.map { ab ->
            ArticleEntity(
                feedId = idMap[ab.feedId] ?: ab.feedId,
                guid = ab.guid,
                title = ab.title,
                link = ab.link,
                author = ab.author,
                pubDate = ab.pubDate,
                summary = ab.summary,
                content = ab.content,
                isRead = ab.isRead,
                isStarred = ab.isStarred
            )
        }
        db.articleDao().upsertAll(articles)
        articles.size
    }

    companion object {
        private val DEFAULT_FEEDS = listOf(
            "https://rss.sina.com.cn/news/mil.xml"
        )
    }
}
