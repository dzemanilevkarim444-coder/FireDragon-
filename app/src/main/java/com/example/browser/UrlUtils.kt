package com.example.browser

import android.net.Uri
import com.example.data.SearchEngine
import java.net.URLEncoder

object UrlUtils {
    private val domainRegex = Regex("^(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(?::\\d+)?(?:/.*)?$")
    private val ipOrLocalRegex = Regex("^(?:localhost|\\d{1,3}(?:\\.\\d{1,3}){3})(?::\\d+)?(?:/.*)?$")

    fun resolveOmniboxInput(
        rawInput: String,
        searchEngine: SearchEngine,
        httpsUpgrade: Boolean
    ): String {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return ""

        if (trimmed.startsWith("http://", ignoreCase = true)) {
            return if (httpsUpgrade && !trimmed.contains("localhost") && !trimmed.contains("127.0.0.1")) {
                "https://" + trimmed.substring(7)
            } else {
                trimmed
            }
        }
        if (trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("about:", ignoreCase = true)
        ) {
            return trimmed
        }

        val hasWhitespace = trimmed.any { it.isWhitespace() }
        if (!hasWhitespace && (domainRegex.matches(trimmed) || ipOrLocalRegex.matches(trimmed))) {
            val scheme = if (httpsUpgrade && !ipOrLocalRegex.matches(trimmed)) "https://" else "http://"
            return "$scheme$trimmed"
        }

        val encoded = URLEncoder.encode(trimmed, "UTF-8")
        return searchEngine.searchUrlTemplate.replace("%s", encoded)
    }

    fun extractHost(url: String): String {
        if (url.isBlank()) return ""
        return try {
            Uri.parse(url).host?.lowercase()?.removePrefix("www.") ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    fun extractRawHost(url: String): String {
        if (url.isBlank()) return ""
        return try {
            Uri.parse(url).host?.lowercase() ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    fun isSecureUrl(url: String): Boolean = url.startsWith("https://", ignoreCase = true)

    fun isYouTubeUrl(url: String): Boolean {
        val host = extractHost(url)
        return host == "youtube.com" || host.endsWith(".youtube.com") || host == "youtu.be"
    }
}
