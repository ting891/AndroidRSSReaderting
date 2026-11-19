package com.ting.rssreader.data.model

/** 解析后的订阅源及其文章 */
data class ParsedFeed(
    val title: String,
    val siteLink: String,
    val description: String,
    val articles: List<ParsedArticle>
)

data class ParsedArticle(
    val guid: String,
    val title: String,
    val link: String,
    val author: String,
    val pubDate: Long,
    val summary: String,
    val content: String
)
