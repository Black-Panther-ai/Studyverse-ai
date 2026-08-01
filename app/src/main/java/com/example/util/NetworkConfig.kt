package com.example.util

import com.example.BuildConfig

object NetworkConfig {
    val baseUrl: String
        get() = normalizeUrl(BuildConfig.BACKEND_BASE_URL)

    fun normalizeUrl(url: String): String {
        var trimmed = url.trim()
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length - 1)
        }
        return trimmed
    }
}
