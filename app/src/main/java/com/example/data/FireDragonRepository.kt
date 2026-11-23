package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FireDragonRepository(
    private val db: FireDragonDatabase,
    private val prefs: PreferencesManager
) {
    private val accountDao = db.accountDao()
    private val bookmarkDao = db.bookmarkDao()
    private val historyDao = db.historyDao()
    private val whitelistDao = db.whitelistDao()
    private val shieldStatsDao = db.shieldStatsDao()

    val sessionPrefs: Flow<SessionPrefs> = prefs.sessionPrefs
    val shieldConfig: Flow<ShieldConfig> = prefs.shieldConfig
    val allAccounts: Flow<List<AccountEntity>> = accountDao.observeAllAccounts()

    fun observeAccount(accountId: Long): Flow<AccountEntity?> = accountDao.observeAccountById(accountId)

    fun observeBookmarks(accountId: Long): Flow<List<BookmarkEntity>> =
        bookmarkDao.observeBookmarks(accountId)

    fun observeHistory(accountId: Long): Flow<List<HistoryEntity>> =
        historyDao.observeHistory(accountId)

    fun observeWhitelistedDomains(accountId: Long): Flow<Set<String>> =
        whitelistDao.observeWhitelistedSites(accountId)
            .map { sites -> sites.map { it.domain.lowercase() }.toSet() }

    fun observeShieldStats(accountId: Long): Flow<ShieldStatsEntity> =
        shieldStatsDao.observeStats(accountId).map { it ?: ShieldStatsEntity(accountId = accountId) }

    suspend fun registerAccount(
        displayName: String,
        email: String,
        password: String
    ): Result<AccountEntity> {
        val cleanName = displayName.trim()
        val cleanEmail = email.trim().lowercase()
        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("Display name must be at least 2 characters."))
        }
        if (!cleanEmail.contains("@") || cleanEmail.length < 5) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }
        val existing = accountDao.getAccountByEmail(cleanEmail)
        if (existing != null) {
            return Result.failure(IllegalStateException("An account with this email already exists."))
        }
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword(password, salt)
        val palette = listOf(0xFFFF5722L, 0xFFFFB300L, 0xFFEF4444L, 0xFF3B82F6L, 0xFF10B981L)
        val avatarColor = palette[(cleanEmail.hashCode().let { if (it < 0) -it else it }) % palette.size]

        val account = AccountEntity(
            displayName = cleanName,
            email = cleanEmail,
            passwordHash = hash,
            salt = salt,
            avatarColorHex = avatarColor,
            isGuest = false
        )
        val id = accountDao.insertAccount(account)
        val saved = account.copy(id = id)
        seedAccountDefaults(id)
        prefs.setActiveAccountId(id)
        return Result.success(saved)
    }

    suspend fun signIn(email: String, password: String): Result<AccountEntity> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter both email and password."))
        }
        val account = accountDao.getAccountByEmail(cleanEmail)
            ?: return Result.failure(IllegalArgumentException("No FireDragon account found for $cleanEmail."))
        if (account.isGuest) {
            prefs.setActiveAccountId(account.id)
            return Result.success(account)
        }
        val verified = PasswordHasher.verifyPassword(password, account.salt, account.passwordHash)
        if (!verified) {
            return Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
        }
        shieldStatsDao.ensureRow(ShieldStatsEntity(accountId = account.id))
        prefs.setActiveAccountId(account.id)
        return Result.success(account)
    }

    suspend fun continueAsGuest(): AccountEntity {
        val guestEmail = "guest@firedragon.local"
        val existing = accountDao.getAccountByEmail(guestEmail)
        if (existing != null) {
            shieldStatsDao.ensureRow(ShieldStatsEntity(accountId = existing.id))
            prefs.setActiveAccountId(existing.id)
            return existing
        }
        val salt = PasswordHasher.generateSalt()
        val guest = AccountEntity(
            displayName = "Dragon Guest",
            email = guestEmail,
            passwordHash = "",
            salt = salt,
            avatarColorHex = 0xFFFF5722L,
            isGuest = true
        )
        val id = accountDao.insertAccount(guest)
        val saved = guest.copy(id = id)
        seedAccountDefaults(id)
        prefs.setActiveAccountId(id)
        return saved
    }

    suspend fun switchAccount(accountId: Long) {
        val acc = accountDao.getAccountById(accountId)
        if (acc != null) {
            shieldStatsDao.ensureRow(ShieldStatsEntity(accountId = acc.id))
            prefs.setActiveAccountId(acc.id)
        }
    }

    suspend fun signOut() {
        prefs.setActiveAccountId(null)
    }

    suspend fun deleteAccount(accountId: Long) {
        bookmarkDao.deleteAllForAccount(accountId)
        historyDao.clearHistory(accountId)
        whitelistDao.clearWhitelistForAccount(accountId)
        shieldStatsDao.deleteStatsForAccount(accountId)
        accountDao.deleteAccountById(accountId)
        prefs.setActiveAccountId(null)
    }

    suspend fun updateDisplayName(accountId: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.length >= 2) {
            accountDao.updateDisplayName(accountId, trimmed)
        }
    }

    suspend fun markTutorialCompleted(accountId: Long) {
        prefs.markTutorialCompleted(accountId)
    }

    suspend fun setDefaultBannerDismissed(dismissed: Boolean) {
        prefs.setDefaultBannerDismissed(dismissed)
    }

    suspend fun updateShieldConfig(transform: (ShieldConfig) -> ShieldConfig) {
        prefs.updateShieldConfig(transform)
    }

    suspend fun toggleBookmark(accountId: Long, title: String, url: String): Boolean {
        val normalized = url.trim()
        if (normalized.isBlank()) return false
        val exists = bookmarkDao.countByUrl(accountId, normalized) > 0
        return if (exists) {
            bookmarkDao.deleteBookmarkByUrl(accountId, normalized)
            false
        } else {
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    accountId = accountId,
                    title = title.ifBlank { normalized },
                    url = normalized,
                    folder = "Bookmarks"
                )
            )
            true
        }
    }

    suspend fun addBookmark(accountId: Long, title: String, url: String) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return
        val existingCount = bookmarkDao.countByUrl(accountId, cleanUrl)
        if (existingCount == 0) {
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    accountId = accountId,
                    title = title.ifBlank { cleanUrl },
                    url = cleanUrl,
                    folder = "Bookmarks"
                )
            )
        }
    }

    suspend fun deleteBookmark(id: Long) {
        bookmarkDao.deleteBookmarkById(id)
    }

    suspend fun recordHistoryVisit(accountId: Long, title: String, url: String) {
        if (url.isBlank() || url.startsWith("about:")) return
        historyDao.insertHistory(
            HistoryEntity(
                accountId = accountId,
                title = title.ifBlank { url },
                url = url
            )
        )
    }

    suspend fun deleteHistoryItem(id: Long) {
        historyDao.deleteHistoryItem(id)
    }

    suspend fun clearHistory(accountId: Long) {
        historyDao.clearHistory(accountId)
    }

    suspend fun toggleDomainWhitelist(accountId: Long, domain: String, currentlyWhitelisted: Boolean) {
        val clean = domain.trim().lowercase()
        if (clean.isBlank()) return
        if (currentlyWhitelisted) {
            whitelistDao.removeWhitelistDomain(accountId, clean)
        } else {
            whitelistDao.insertWhitelistSite(
                WhitelistSiteEntity(
                    accountId = accountId,
                    domain = clean
                )
            )
        }
    }

    suspend fun recordBlockedEvents(
        accountId: Long,
        ads: Long = 0L,
        trackers: Long = 0L,
        youtubeAds: Long = 0L,
        popups: Long = 0L
    ) {
        if (ads == 0L && trackers == 0L && youtubeAds == 0L && popups == 0L) return
        shieldStatsDao.ensureRow(ShieldStatsEntity(accountId = accountId))
        shieldStatsDao.incrementStats(accountId, ads, trackers, youtubeAds, popups)
    }

    private suspend fun seedAccountDefaults(accountId: Long) {
        shieldStatsDao.ensureRow(ShieldStatsEntity(accountId = accountId))
        val starters = listOf(
            BookmarkEntity(accountId = accountId, title = "YouTube", url = "https://m.youtube.com", folder = "Speed Dial"),
            BookmarkEntity(accountId = accountId, title = "Google", url = "https://www.google.com", folder = "Speed Dial"),
            BookmarkEntity(accountId = accountId, title = "Wikipedia", url = "https://en.m.wikipedia.org", folder = "Speed Dial"),
            BookmarkEntity(accountId = accountId, title = "DuckDuckGo", url = "https://duckduckgo.com", folder = "Speed Dial"),
            BookmarkEntity(accountId = accountId, title = "GitHub", url = "https://github.com", folder = "Speed Dial"),
            BookmarkEntity(accountId = accountId, title = "Reddit", url = "https://www.reddit.com", folder = "Speed Dial")
        )
        starters.forEach { bookmarkDao.insertBookmark(it) }
    }
}
