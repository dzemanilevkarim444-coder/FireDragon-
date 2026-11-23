package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "firedragon_prefs")

enum class SearchEngine(val label: String, val searchUrlTemplate: String, val homeUrl: String) {
    GOOGLE("Google", "https://www.google.com/search?q=%s", "https://www.google.com"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=%s", "https://duckduckgo.com"),
    BRAVE("Brave Search", "https://search.brave.com/search?q=%s", "https://search.brave.com"),
    BING("Bing", "https://www.bing.com/search?q=%s", "https://www.bing.com");

    companion object {
        fun fromName(raw: String?): SearchEngine {
            return entries.firstOrNull { it.name == raw } ?: GOOGLE
        }
    }
}

enum class ThemeMode(val label: String) {
    DARK("Dark Obsidian"),
    LIGHT("Light Paper"),
    SYSTEM("Follow System");

    companion object {
        fun fromName(raw: String?): ThemeMode {
            return entries.firstOrNull { it.name == raw } ?: DARK
        }
    }
}

data class ShieldConfig(
    val adBlockEnabled: Boolean = true,
    val youtubeShieldEnabled: Boolean = true,
    val cosmeticFilterEnabled: Boolean = true,
    val popupGuardEnabled: Boolean = true,
    val httpsUpgradeEnabled: Boolean = true,
    val searchEngine: SearchEngine = SearchEngine.GOOGLE,
    val themeMode: ThemeMode = ThemeMode.DARK
)

data class SessionPrefs(
    val activeAccountId: Long? = null,
    val completedTutorialAccountIds: Set<String> = emptySet(),
    val defaultBannerDismissed: Boolean = false
)

class PreferencesManager(context: Context) {
    private val appContext = context.applicationContext

    private object Keys {
        val ACTIVE_ACCOUNT_ID = longPreferencesKey("active_account_id")
        val TUTORIAL_COMPLETED_IDS = stringSetPreferencesKey("tutorial_completed_ids")
        val DEFAULT_BANNER_DISMISSED = booleanPreferencesKey("default_banner_dismissed")
        val AD_BLOCK_ENABLED = booleanPreferencesKey("ad_block_enabled")
        val YOUTUBE_SHIELD_ENABLED = booleanPreferencesKey("youtube_shield_enabled")
        val COSMETIC_FILTER_ENABLED = booleanPreferencesKey("cosmetic_filter_enabled")
        val POPUP_GUARD_ENABLED = booleanPreferencesKey("popup_guard_enabled")
        val HTTPS_UPGRADE_ENABLED = booleanPreferencesKey("https_upgrade_enabled")
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val sessionPrefs: Flow<SessionPrefs> = appContext.dataStore.data.map { prefs ->
        val rawId = prefs[Keys.ACTIVE_ACCOUNT_ID]
        SessionPrefs(
            activeAccountId = if (rawId != null && rawId > 0L) rawId else null,
            completedTutorialAccountIds = prefs[Keys.TUTORIAL_COMPLETED_IDS] ?: emptySet(),
            defaultBannerDismissed = prefs[Keys.DEFAULT_BANNER_DISMISSED] ?: false
        )
    }

    val shieldConfig: Flow<ShieldConfig> = appContext.dataStore.data.map { prefs ->
        ShieldConfig(
            adBlockEnabled = prefs[Keys.AD_BLOCK_ENABLED] ?: true,
            youtubeShieldEnabled = prefs[Keys.YOUTUBE_SHIELD_ENABLED] ?: true,
            cosmeticFilterEnabled = prefs[Keys.COSMETIC_FILTER_ENABLED] ?: true,
            popupGuardEnabled = prefs[Keys.POPUP_GUARD_ENABLED] ?: true,
            httpsUpgradeEnabled = prefs[Keys.HTTPS_UPGRADE_ENABLED] ?: true,
            searchEngine = SearchEngine.fromName(prefs[Keys.SEARCH_ENGINE]),
            themeMode = ThemeMode.fromName(prefs[Keys.THEME_MODE])
        )
    }

    suspend fun setActiveAccountId(accountId: Long?) {
        appContext.dataStore.edit { prefs ->
            if (accountId == null || accountId <= 0L) {
                prefs.remove(Keys.ACTIVE_ACCOUNT_ID)
            } else {
                prefs[Keys.ACTIVE_ACCOUNT_ID] = accountId
            }
        }
    }

    suspend fun markTutorialCompleted(accountId: Long) {
        appContext.dataStore.edit { prefs ->
            val current = prefs[Keys.TUTORIAL_COMPLETED_IDS] ?: emptySet()
            prefs[Keys.TUTORIAL_COMPLETED_IDS] = current + accountId.toString()
        }
    }

    suspend fun setDefaultBannerDismissed(dismissed: Boolean) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_BANNER_DISMISSED] = dismissed
        }
    }

    suspend fun updateShieldConfig(transform: (ShieldConfig) -> ShieldConfig) {
        appContext.dataStore.edit { prefs ->
            val current = ShieldConfig(
                adBlockEnabled = prefs[Keys.AD_BLOCK_ENABLED] ?: true,
                youtubeShieldEnabled = prefs[Keys.YOUTUBE_SHIELD_ENABLED] ?: true,
                cosmeticFilterEnabled = prefs[Keys.COSMETIC_FILTER_ENABLED] ?: true,
                popupGuardEnabled = prefs[Keys.POPUP_GUARD_ENABLED] ?: true,
                httpsUpgradeEnabled = prefs[Keys.HTTPS_UPGRADE_ENABLED] ?: true,
                searchEngine = SearchEngine.fromName(prefs[Keys.SEARCH_ENGINE]),
                themeMode = ThemeMode.fromName(prefs[Keys.THEME_MODE])
            )
            val updated = transform(current)
            prefs[Keys.AD_BLOCK_ENABLED] = updated.adBlockEnabled
            prefs[Keys.YOUTUBE_SHIELD_ENABLED] = updated.youtubeShieldEnabled
            prefs[Keys.COSMETIC_FILTER_ENABLED] = updated.cosmeticFilterEnabled
            prefs[Keys.POPUP_GUARD_ENABLED] = updated.popupGuardEnabled
            prefs[Keys.HTTPS_UPGRADE_ENABLED] = updated.httpsUpgradeEnabled
            prefs[Keys.SEARCH_ENGINE] = updated.searchEngine.name
            prefs[Keys.THEME_MODE] = updated.themeMode.name
        }
    }
}
