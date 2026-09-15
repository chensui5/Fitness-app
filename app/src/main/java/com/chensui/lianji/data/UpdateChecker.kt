package com.chensui.lianji.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * 版本更新检查。
 *
 * 更新源就是本项目在 GitHub 上的 Releases —— 不需要自建服务器，也不依赖任何
 * 第三方推送 SDK。只在用户主动点击「检查更新」时才发起一次请求，平时完全不联网。
 *
 * 之所以用 GitHub 而不是「服务端推送」：后者需要长期维护后端与推送通道，
 * 对一个自用 App 来说成本远超收益。
 */
object UpdateChecker {

    /** 最新发布版的接口，返回 tag_name / body / html_url */
    private const val API_LATEST =
        "https://api.github.com/repos/chensui5/Fitness-app/releases/latest"

    /** 固定下载入口，总是指向最新版 */
    const val DOWNLOAD_PAGE =
        "https://github.com/chensui5/Fitness-app/releases/latest"

    private const val TIMEOUT_MS = 10_000

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Serializable
    private data class Release(
        @SerialName("tag_name") val tagName: String = "",
        val name: String = "",
        val body: String = "",
        @SerialName("html_url") val htmlUrl: String = ""
    )

    sealed interface UpdateResult {
        /** 远端有更新版本 */
        data class Newer(val version: String, val notes: String, val url: String) : UpdateResult

        /** 当前已经是最新 */
        data class Latest(val version: String) : UpdateResult

        /** 检查失败：网络不通、超时、被限流、解析失败等 */
        data class Failed(val reason: String) : UpdateResult
    }

    /**
     * 设备当前是否有可用网络。
     *
     * 没网时直接跳过检查 —— 既不弹提示，也不白等到超时。
     * 注意这只代表「有网络」，不代表一定能访问 GitHub，
     * 所以最终判定仍然以 [check] 的返回结果为准。
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * 查询最新版本并与 [currentVersion] 比较。
     *
     * 网络请求在 IO 线程执行，调用方可直接在协程里 await。
     */
    suspend fun check(currentVersion: String): UpdateResult = withContext(Dispatchers.IO) {
        val viaApi = checkViaApi(currentVersion)
        if (viaApi is UpdateResult.Failed) {
            // 降级路径：API 的匿名配额只有 60 次/小时，而且是**按出口 IP 算**的。
            // 走代理、公司或校园网共享出口时，配额极易被别人耗光（实测就撞上过 403）。
            // releases/latest 的 302 跳转不受这个限流约束，代价是拿不到更新说明。
            checkViaRedirect(currentVersion) ?: viaApi
        } else {
            viaApi
        }
    }

    private fun checkViaApi(currentVersion: String): UpdateResult =
        runCatching {
            val conn = (URL(API_LATEST).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                // GitHub API 强制要求 User-Agent，缺失会直接返回 403。
                // 浏览器里手敲链接是好的，代码里漏了这个头就会挂 —— 这个坑很隐蔽。
                setRequestProperty("User-Agent", "LianJi-Android")
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            try {
                val code = conn.responseCode
                if (code != 200) {
                    return@runCatching UpdateResult.Failed(
                        when (code) {
                            404 -> "没有找到发布记录"
                            403 -> "访问被拒绝，可能是请求过于频繁"
                            else -> "服务返回 $code"
                        }
                    )
                }
                val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val rel = json.decodeFromString<Release>(text)
                val remote = rel.tagName.ifBlank { rel.name }.trim()
                if (remote.isBlank()) return@runCatching UpdateResult.Failed("返回内容无法识别")

                if (compareVersions(remote, currentVersion) > 0) {
                    UpdateResult.Newer(
                        version = stripV(remote),
                        notes = cleanNotes(rel.body),
                        url = rel.htmlUrl.ifBlank { DOWNLOAD_PAGE }
                    )
                } else {
                    UpdateResult.Latest(stripV(currentVersion))
                }
            } finally {
                conn.disconnect()
            }
        }.getOrElse { e ->
            UpdateResult.Failed(e.message?.take(60) ?: "网络不可用")
        }

    /**
     * 备用路径：读 `releases/latest` 的 302 跳转地址。
     *
     * `Location: .../releases/tag/v1.4.4` 里就带着最新版本号。这个入口走的是
     * github.com 而不是 api.github.com，没有 60 次/小时的匿名限流，
     * 但只有版本号可用，拿不到更新说明。
     *
     * 全部失败时返回 null，由调用方沿用 API 那条路径给出的失败原因。
     */
    private fun checkViaRedirect(currentVersion: String): UpdateResult? = runCatching {
        val conn = (URL(DOWNLOAD_PAGE).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            // 必须关掉自动跟随，否则会跳到标签页的 HTML，读不到版本号
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", "LianJi-Android")
        }
        try {
            conn.responseCode
            val loc = conn.getHeaderField("Location")
            val tag = loc?.trimEnd('/')?.substringAfterLast('/')?.trim().orEmpty()
            if (tag.isBlank()) return@runCatching null
            if (compareVersions(tag, currentVersion) > 0) {
                UpdateResult.Newer(stripV(tag), "", DOWNLOAD_PAGE)
            } else {
                UpdateResult.Latest(stripV(currentVersion))
            }
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    /**
     * 版本号比较：返回 >0 表示 [a] 比 [b] 新，0 表示相同，<0 表示更旧。
     *
     * **必须按整数逐段比较，绝不能直接用字符串比较。**
     * 字符串比较下 "1.4.10" < "1.4.9"（逐字符比到第三段时 '1' < '9'），
     * 结果是 1.4.10 永远提示不了更新，而且不报任何错 —— 很难查。
     */
    fun compareVersions(a: String, b: String): Int {
        val pa = segments(a)
        val pb = segments(b)
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return x - y
        }
        return 0
    }

    private fun segments(v: String): List<Int> =
        stripV(v)
            .substringBefore('-')
            .substringBefore('+')
            .split('.')
            .map { part -> part.filter { it.isDigit() }.ifBlank { "0" }.toIntOrNull() ?: 0 }

    private fun stripV(v: String): String =
        v.trim().removePrefix("v").removePrefix("V").trim()

    /**
     * Release 说明是 markdown，去掉标记符号后直接展示，免得满屏 # 和 *。
     * 我在写 Release 说明时也会尽量少用花哨语法，两边配合着来。
     */
    private fun cleanNotes(raw: String): String {
        val cleaned = raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { line ->
                val noHead = line.removePrefix("###").removePrefix("##").removePrefix("#").trim()
                noHead
                    .replace(Regex("^[-*+]\\s+"), "· ")
                    .replace("**", "")
                    .replace("`", "")
            }
            .joinToString("\n")
            .trim()
        return if (cleaned.length > 600) cleaned.take(600) + "…" else cleaned
    }
}
