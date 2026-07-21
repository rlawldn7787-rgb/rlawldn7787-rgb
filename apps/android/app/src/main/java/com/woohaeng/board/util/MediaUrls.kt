package com.woohaeng.board.util

import android.net.Uri
import com.woohaeng.board.BuildConfig

/** API가 localhost URL을 줄 때 실기기/에뮬에서 열리도록 변환합니다. */
fun resolveMediaUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val base = BuildConfig.API_BASE_URL.trimEnd('/')
    val trimmed = url.trim()
    if (trimmed.startsWith("/")) return base + trimmed

    return runCatching {
        val uri = Uri.parse(trimmed)
        val host = uri.host?.lowercase()
        if (host == "localhost" || host == "127.0.0.1" || host == "10.0.2.2") {
            val path = uri.encodedPath.orEmpty()
            val query = uri.encodedQuery?.let { "?$it" }.orEmpty()
            "$base$path$query"
        } else {
            trimmed
        }
    }.getOrDefault(trimmed)
}
