# AndroidRSSReaderting

Android 课程设计：**B3 RSS 阅读器（主 App）** + **P6 备份与恢复（伴生模块）**。

## 模块结构

| 模块 | 角色 | 说明 |
| --- | --- | --- |
| `:app` | 主 App（2 学分） | 订阅源管理、RSS/Atom 解析、离线缓存、已读未读、后台刷新、全文检索 |
| `:backup` | 伴生模块（1 学分） | 备份快照、JSON 导入/导出、FileProvider 分享、定时快照，与主 App 通过约定 JSON 交换数据 |

## 主 App 功能

- **订阅源管理**：添加 / 删除订阅源，单源手动刷新。
- **XML 解析**：OkHttp 拉取，`XmlPullParser` 兼容 RSS 2.0 与 Atom 1.0，解析标题、链接、作者、日期、摘要、正文。
- **离线缓存**：Room 数据库持久化订阅源与文章（含正文/摘要），无网络时仍可浏览已缓存内容。
- **已读 / 未读**：进入详情自动标记已读；列表未读红点，支持收藏。
- **后台刷新**：WorkManager 每 6 小时周期刷新全部订阅源。
- **全文检索**：按标题、摘要、正文关键字搜索。
- **界面**：列表 → 详情 → 订阅源管理，Navigation Compose 导航；含加载态 / 空态 / 错误态。

## 伴生模块（:backup）功能

- 生成带时间戳的历史备份快照。
- 通过系统文件选择器（SAF）导入 JSON 备份并校验。
- FileProvider 分享当前备份文件。
- WorkManager 每 12 小时定时生成快照。
- 与主 App 约定同一 JSON 结构（`BackupDto`）：主 App 可导出 `backup.json`，伴生模块导入后查看/再备份；恢复数据由主 App 的导入接口完成。

## 技术栈

- Kotlin + Jetpack Compose + Material3
- MVVM：ViewModel + StateFlow + 协程
- Room（KSP 注解处理）
- WorkManager
- OkHttp + XmlPullParser
- Gson（备份序列化）
- Navigation Compose

## 构建环境

- AGP 9.3.0 / Gradle 9.5.0 / Kotlin 2.2.10
- compileSdk 37，minSdk 24，JDK 11 字节码（运行于 JBR 25）

## 运行

```bash
# 命令行构建 Debug APK
./gradlew :app:assembleDebug
./gradlew :backup:assembleDebug
```

产物：
- `app/build/outputs/apk/debug/app-debug.apk`
- `backup/build/outputs/apk/debug/backup-debug.apk`

> `local.properties` 为本地 SDK 路径配置，不纳入版本控制。
