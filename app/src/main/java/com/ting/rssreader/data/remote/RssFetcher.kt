package com.ting.rssreader.data.remote

import android.util.Log
import android.util.Xml
import com.ting.rssreader.data.model.ParsedArticle
import com.ting.rssreader.data.model.ParsedFeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 网络拉取 + XML 解析（兼容 RSS 2.0 与 Atom 1.0）。
 */
class RssFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetchXml(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else {
                    Log.e("RssFetcher", "HTTP ${resp.code} for $url")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("RssFetcher", "fetch failed $url", e)
            null
        }
    }

    fun parse(xml: String): ParsedFeed? {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(xml.reader())

        var channelTitle = ""
        var channelLink = ""
        var channelDesc = ""
        val articles = mutableListOf<ParsedArticle>()

        var inItem = false
        var t = ""; var l = ""; var a = ""; var dateText = ""
        var summary = ""; var content = ""; var guid = ""

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (localName(parser.name)) {
                        "item", "entry" -> {
                            inItem = true
                            t = ""; l = ""; a = ""; dateText = ""
                            summary = ""; content = ""; guid = ""
                        }
                        "title" -> {
                            val v = parser.nextText()
                            if (inItem) t = v else channelTitle = v
                        }
                        "link" -> {
                            val href = parser.getAttributeValue(null, "href")
                            if (href != null) {
                                if (inItem) l = href else channelLink = href
                            } else {
                                val v = parser.nextText()
                                if (inItem) l = v else channelLink = v
                            }
                        }
                        "description", "subtitle", "summary" -> {
                            if (inItem) summary = parser.nextText() else channelDesc = parser.nextText()
                        }
                        "encoded", "content" -> {
                            if (inItem) content = parser.nextText()
                        }
                        "author", "name" -> {
                            if (inItem) a = parser.nextText()
                        }
                        "guid", "id" -> {
                            if (inItem) guid = parser.nextText()
                        }
                        "pubDate", "published", "updated" -> {
                            if (inItem) dateText = parser.nextText()
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (localName(parser.name) == "item" || localName(parser.name) == "entry") {
                        inItem = false
                        val g = guid.ifBlank { l.ifBlank { t } }
                        articles.add(
                            ParsedArticle(
                                guid = g,
                                title = t.ifBlank { "(无标题)" },
                                link = l,
                                author = a,
                                pubDate = parseDate(dateText),
                                summary = summary.stripHtml(),
                                content = (content.ifBlank { summary }).stripHtml()
                            )
                        )
                    }
                }
            }
            event = parser.next()
        }

        if (articles.isEmpty() && channelTitle.isBlank()) return null
        return ParsedFeed(
            title = channelTitle.ifBlank { "未命名订阅源" },
            siteLink = channelLink,
            description = channelDesc.stripHtml(),
            articles = articles
        )
    }

    private fun localName(name: String?): String =
        name?.substringAfter(':') ?: ""

    private fun parseDate(text: String): Long {
        if (text.isBlank()) return 0L
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (f in formats) {
            try {
                return SimpleDateFormat(f, Locale.US).parse(text.trim())?.time ?: 0L
            } catch (_: Exception) {
            }
        }
        return 0L
    }

    private fun String.stripHtml(): String {
        // 反复移除 HTML 标签（含属性），再清掉未配对的尖括号
        var s = Regex("<[^>]*>").replace(this, " ")
        s = s.replace(Regex("[<>]"), " ")
        val escaped = s
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
        // 中文正文使用全角引号，残留的半角双引号多为标签属性碎片，予以清除
        return escaped
            .replace("\"", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 12) RSSReader/1.0"
    }
}
