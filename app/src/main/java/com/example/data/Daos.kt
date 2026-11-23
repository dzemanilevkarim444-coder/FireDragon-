package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY createdAt DESC")
    fun observeAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    fun observeAccountById(id: Long): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getAccountByEmail(email: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccount(account: AccountEntity): Long

    @Query("UPDATE accounts SET displayName = :displayName WHERE id = :id")
    suspend fun updateDisplayName(id: Long, displayName: String)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Long)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE accountId = :accountId ORDER BY createdAt DESC")
    fun observeBookmarks(accountId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT COUNT(*) FROM bookmarks WHERE accountId = :accountId AND url = :url")
    suspend fun countByUrl(accountId: Long, url: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM bookmarks WHERE accountId = :accountId AND url = :url")
    suspend fun deleteBookmarkByUrl(accountId: Long, url: String)

    @Query("DELETE FROM bookmarks WHERE accountId = :accountId")
    suspend fun deleteAllForAccount(accountId: Long)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history WHERE accountId = :accountId ORDER BY visitedAt DESC LIMIT 200")
    fun observeHistory(accountId: Long): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: HistoryEntity): Long

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryItem(id: Long)

    @Query("DELETE FROM history WHERE accountId = :accountId")
    suspend fun clearHistory(accountId: Long)
}

@Dao
interface WhitelistDao {
    @Query("SELECT * FROM whitelist_sites WHERE accountId = :accountId ORDER BY addedAt DESC")
    fun observeWhitelistedSites(accountId: Long): Flow<List<WhitelistSiteEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWhitelistSite(site: WhitelistSiteEntity)

    @Query("DELETE FROM whitelist_sites WHERE accountId = :accountId AND LOWER(domain) = LOWER(:domain)")
    suspend fun removeWhitelistDomain(accountId: Long, domain: String)

    @Query("DELETE FROM whitelist_sites WHERE accountId = :accountId")
    suspend fun clearWhitelistForAccount(accountId: Long)
}

@Dao
interface ShieldStatsDao {
    @Query("SELECT * FROM shield_stats WHERE accountId = :accountId LIMIT 1")
    fun observeStats(accountId: Long): Flow<ShieldStatsEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ensureRow(stats: ShieldStatsEntity)

    @Query(
        """
        UPDATE shield_stats 
        SET adsBlockedTotal = adsBlockedTotal + :ads,
            trackersBlockedTotal = trackersBlockedTotal + :trackers,
            youtubeAdsBlockedTotal = youtubeAdsBlockedTotal + :ytAds,
            popupsBlockedTotal = popupsBlockedTotal + :popups
        WHERE accountId = :accountId
        """
    )
    suspend fun incrementStats(
        accountId: Long,
        ads: Long,
        trackers: Long,
        ytAds: Long,
        popups: Long
    )

    @Query("DELETE FROM shield_stats WHERE accountId = :accountId")
    suspend fun deleteStatsForAccount(accountId: Long)
}
