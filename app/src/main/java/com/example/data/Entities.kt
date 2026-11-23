package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["email"], unique = true)]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val displayName: String,
    val email: String,
    val passwordHash: String,
    val salt: String,
    val avatarColorHex: Long,
    val isGuest: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["accountId"])]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val accountId: Long,
    val title: String,
    val url: String,
    val folder: String = "Speed Dial",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "history",
    indices = [Index(value = ["accountId", "visitedAt"])]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val accountId: Long,
    val title: String,
    val url: String,
    val visitedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "whitelist_sites",
    indices = [Index(value = ["accountId", "domain"], unique = true)]
)
data class WhitelistSiteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val accountId: Long,
    val domain: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "shield_stats")
data class ShieldStatsEntity(
    @PrimaryKey val accountId: Long,
    val adsBlockedTotal: Long = 0L,
    val trackersBlockedTotal: Long = 0L,
    val youtubeAdsBlockedTotal: Long = 0L,
    val popupsBlockedTotal: Long = 0L
)
