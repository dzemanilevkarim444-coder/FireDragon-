package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.browser.UrlUtils
import com.example.data.BookmarkEntity
import com.example.data.HistoryEntity
import com.example.data.ShieldConfig
import com.example.data.ShieldStatsEntity
import com.example.ui.BrowserTab
import com.example.ui.theme.IncognitoPurple
import com.example.ui.theme.ShieldGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TabSwitcherOverlay(
    tabs: List<BrowserTab>,
    activeTabIndex: Int,
    onSelectTab: (Int) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: (Boolean) -> Unit,
    onCloseAllTabs: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Chrome-style Tab Grid Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onNewTab(false)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(48.dp).testTag("tab_switcher_new_tab")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Tab")
                    }

                    OutlinedButton(
                        onClick = {
                            onNewTab(true)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(48.dp).testTag("tab_switcher_incognito")
                    ) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Incognito")
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("tab_switcher_close_overlay")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close tab switcher")
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 156.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(tabs, key = { _, item -> item.id }) { index, tab ->
                    val isSelected = index == activeTabIndex
                    val borderColor = when {
                        isSelected && tab.isIncognito -> IncognitoPurple
                        isSelected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    }
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (tab.isIncognito) MaterialTheme.colorScheme.surfaceContainerHigh
                            else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clip(RoundedCornerShape(18.dp))
                            .testTag("tab_card_$index")
                            .clickable {
                                onSelectTab(index)
                                onDismiss()
                            }
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (tab.isIncognito) IncognitoPurple.copy(alpha = 0.22f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (tab.isIncognito) Icons.Default.VisibilityOff
                                    else Icons.Default.Public,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (tab.isIncognito) IncognitoPurple
                                    else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.title.ifBlank { "New Tab" },
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onCloseTab(tab.id) },
                                    modifier = Modifier.size(36.dp).testTag("close_tab_$index")
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close tab",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Tab preview body
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (tab.url.isBlank()) {
                                        if (tab.isIncognito) "Incognito New Tab\nHistory & cookies paused"
                                        else "FireDragon New Tab\nSpeed Dial & Shield ready"
                                    } else {
                                        tab.url
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (tab.blockedAdsOnPage + tab.blockedTrackersOnPage > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = ShieldGreen.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = "${tab.blockedAdsOnPage + tab.blockedTrackersOnPage} blocked",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ShieldGreen,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (tabs.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            onCloseAllTabs()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("close_all_tabs_button")
                    ) {
                        Text("Close all ${tabs.size} tabs")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShieldBottomSheet(
    currentUrl: String,
    blockedAdsOnPage: Int,
    blockedTrackersOnPage: Int,
    shieldConfig: ShieldConfig,
    shieldStats: ShieldStatsEntity,
    whitelistedDomains: Set<String>,
    onToggleDomainWhitelist: (String) -> Unit,
    onToggleAdBlock: (Boolean) -> Unit,
    onToggleYouTubeShield: (Boolean) -> Unit,
    onToggleCosmeticFilter: (Boolean) -> Unit,
    onTogglePopupGuard: (Boolean) -> Unit,
    onToggleHttpsUpgrade: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val host = remember(currentUrl) { UrlUtils.extractHost(currentUrl) }
    val isSitePaused = host.isNotBlank() && whitelistedDomains.contains(host)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (shieldConfig.adBlockEnabled && !isSitePaused) ShieldGreen.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (shieldConfig.adBlockEnabled && !isSitePaused) ShieldGreen
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FireDragon Shield",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (host.isNotBlank()) host else "Global Protection",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Live Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ShieldMetricTile(
                    label = "Page Blocked",
                    value = (blockedAdsOnPage + blockedTrackersOnPage).toString(),
                    modifier = Modifier.weight(1f)
                )
                ShieldMetricTile(
                    label = "YouTube Ads",
                    value = shieldStats.youtubeAdsBlockedTotal.toString(),
                    modifier = Modifier.weight(1f)
                )
                ShieldMetricTile(
                    label = "Total Saved",
                    value = (shieldStats.adsBlockedTotal + shieldStats.trackersBlockedTotal).toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            if (host.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Shields for $host",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isSitePaused) "Paused on this site" else "Active on this site",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = !isSitePaused,
                            onCheckedChange = { onToggleDomainWhitelist(host) },
                            modifier = Modifier.testTag("shield_site_switch")
                        )
                    }
                }
            }

            HorizontalDivider()

            ShieldSheetToggle(
                icon = Icons.Default.Shield,
                title = "Ad & Tracker Blocker",
                subtitle = "Blocks third-party ad networks and tracking scripts",
                checked = shieldConfig.adBlockEnabled,
                onCheckedChange = onToggleAdBlock,
                tag = "sheet_toggle_adblock"
            )
            ShieldSheetToggle(
                icon = Icons.Default.PlayCircle,
                title = "YouTube Clean Shield",
                subtitle = "Removes YouTube ad banners, overlays, and promo dialogs",
                checked = shieldConfig.youtubeShieldEnabled,
                onCheckedChange = onToggleYouTubeShield,
                tag = "sheet_toggle_youtube"
            )
            ShieldSheetToggle(
                icon = Icons.Default.Language,
                title = "Cosmetic Element Hiding",
                subtitle = "Collapses empty ad frames and sponsored widgets",
                checked = shieldConfig.cosmeticFilterEnabled,
                onCheckedChange = onToggleCosmeticFilter,
                tag = "sheet_toggle_cosmetic"
            )
            ShieldSheetToggle(
                icon = Icons.Default.VisibilityOff,
                title = "Popup & Redirect Guard",
                subtitle = "Blocks unsolicited popup windows and ad redirects",
                checked = shieldConfig.popupGuardEnabled,
                onCheckedChange = onTogglePopupGuard,
                tag = "sheet_toggle_popup"
            )
            ShieldSheetToggle(
                icon = Icons.Default.Shield,
                title = "HTTPS Upgrade",
                subtitle = "Automatically upgrades HTTP navigation to HTTPS",
                checked = shieldConfig.httpsUpgradeEnabled,
                onCheckedChange = onToggleHttpsUpgrade,
                tag = "sheet_toggle_https"
            )
        }
    }
}

@Composable
private fun ShieldMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ShieldSheetToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksHistorySheet(
    initialTab: Int,
    bookmarks: List<BookmarkEntity>,
    history: List<HistoryEntity>,
    onOpenUrl: (String) -> Unit,
    onAddBookmark: (title: String, url: String) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onDeleteHistoryItem: (Long) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
                .padding(horizontal = 16.dp)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Bookmarks (${bookmarks.size})") },
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("History (${history.size})") },
                    icon = { Icon(Icons.Default.History, contentDescription = null) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (selectedTab == 0) "Filter bookmarks..." else "Filter history...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )

                if (selectedTab == 0) {
                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(52.dp).testTag("add_bookmark_dialog_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add bookmark")
                    }
                } else if (history.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearHistory,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(52.dp).testTag("clear_history_button")
                    ) {
                        Text("Clear")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedTab == 0) {
                val filtered = remember(bookmarks, searchQuery) {
                    if (searchQuery.isBlank()) bookmarks
                    else bookmarks.filter {
                        it.title.contains(searchQuery, true) || it.url.contains(searchQuery, true)
                    }
                }
                if (filtered.isEmpty()) {
                    EmptyLibraryState(
                        title = "No bookmarks found",
                        subtitle = "Tap the star icon in the address bar or + above to save your favorite sites."
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filtered, key = { it.id }) { item ->
                            LibraryItemRow(
                                title = item.title,
                                subtitle = item.url,
                                onClick = {
                                    onOpenUrl(item.url)
                                    onDismiss()
                                },
                                onDelete = { onDeleteBookmark(item.id) }
                            )
                        }
                    }
                }
            } else {
                val filtered = remember(history, searchQuery) {
                    if (searchQuery.isBlank()) history
                    else history.filter {
                        it.title.contains(searchQuery, true) || it.url.contains(searchQuery, true)
                    }
                }
                val formatter = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
                if (filtered.isEmpty()) {
                    EmptyLibraryState(
                        title = "No browsing history yet",
                        subtitle = "Pages you visit outside Incognito tabs appear here."
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filtered, key = { it.id }) { item ->
                            LibraryItemRow(
                                title = item.title,
                                subtitle = "${formatter.format(Date(item.visitedAt))} · ${item.url}",
                                onClick = {
                                    onOpenUrl(item.url)
                                    onDismiss()
                                },
                                onDelete = { onDeleteHistoryItem(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var bTitle by remember { mutableStateOf("") }
        var bUrl by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Bookmark") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = bTitle,
                        onValueChange = { bTitle = it },
                        label = { Text("Title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = bUrl,
                        onValueChange = { bUrl = it },
                        label = { Text("URL (e.g. youtube.com)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bUrl.isNotBlank()) {
                            onAddBookmark(bTitle.ifBlank { bUrl }, bUrl)
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LibraryItemRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Remove item",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Bookmark,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        )
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
