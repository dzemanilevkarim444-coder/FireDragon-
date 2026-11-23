package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.browser.UrlUtils
import com.example.data.AccountEntity
import com.example.data.BookmarkEntity
import com.example.ui.BrowserTab
import com.example.ui.BrowserUiState
import com.example.ui.BrowserViewModel
import com.example.ui.components.BookmarksHistorySheet
import com.example.ui.components.BrowserWebViewHost
import com.example.ui.components.ShieldBottomSheet
import com.example.ui.components.TabSwitcherOverlay
import com.example.ui.theme.AmberGold
import com.example.ui.theme.IncognitoPurple
import com.example.ui.theme.ShieldGreen

@Composable
fun BrowserScreen(
    uiState: BrowserUiState,
    viewModel: BrowserViewModel,
    onRequestDefaultBrowser: () -> Unit,
    onOpenAccountSettings: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val activeTab = uiState.activeTab

    var isOmniboxFocused by remember { mutableStateOf(false) }
    var omniboxText by remember(activeTab.id, activeTab.url) { mutableStateOf(activeTab.url) }
    var overflowExpanded by remember { mutableStateOf(false) }
    var showTabSwitcher by remember { mutableStateOf(false) }
    var showShieldSheet by remember { mutableStateOf(false) }
    var libraryInitialTab by remember { mutableIntStateOf(-1) }
    var findQuery by remember { mutableStateOf("") }

    // Synchronize omnibox text when navigation updates activeTab.url while unfocused
    LaunchedEffect(activeTab.url, isOmniboxFocused) {
        if (!isOmniboxFocused) {
            omniboxText = activeTab.url
        }
    }

    // Back gesture handling: overlays -> find -> omnibox focus -> webview back -> NTP
    BackHandler(
        enabled = showTabSwitcher ||
            showShieldSheet ||
            libraryInitialTab >= 0 ||
            uiState.findInPageVisible ||
            isOmniboxFocused ||
            activeTab.canGoBack ||
            activeTab.url.isNotBlank()
    ) {
        when {
            showTabSwitcher -> showTabSwitcher = false
            showShieldSheet -> showShieldSheet = false
            libraryInitialTab >= 0 -> libraryInitialTab = -1
            uiState.findInPageVisible -> viewModel.closeFindInPage()
            isOmniboxFocused -> {
                isOmniboxFocused = false
                omniboxText = activeTab.url
                focusManager.clearFocus()
            }
            activeTab.canGoBack -> viewModel.goBackActiveTab()
            activeTab.url.isNotBlank() -> viewModel.goHomeNewTabPage()
        }
    }

    val isBookmarked = remember(uiState.bookmarks, activeTab.url) {
        activeTab.url.isNotBlank() && uiState.bookmarks.any { it.url == activeTab.url }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Chrome-style Top Omnibox Toolbar
            ChromeOmniboxToolbar(
                activeTab = activeTab,
                tabCount = uiState.tabs.size,
                omniboxText = omniboxText,
                isOmniboxFocused = isOmniboxFocused,
                isBookmarked = isBookmarked,
                shieldActive = uiState.shieldConfig.adBlockEnabled &&
                    !uiState.whitelistedDomains.contains(UrlUtils.extractHost(activeTab.url)),
                overflowExpanded = overflowExpanded,
                onOmniboxTextChange = { omniboxText = it },
                onOmniboxFocusChanged = { focused ->
                    isOmniboxFocused = focused
                    if (focused && omniboxText.isBlank()) {
                        omniboxText = activeTab.url
                    }
                },
                onSubmitOmnibox = {
                    val query = omniboxText.trim()
                    if (query.isNotBlank()) {
                        viewModel.navigateActiveTab(query)
                    }
                    isOmniboxFocused = false
                    focusManager.clearFocus()
                },
                onShieldClick = { showShieldSheet = true },
                onToggleBookmark = { viewModel.toggleBookmarkActivePage() },
                onReloadOrStop = { viewModel.reloadOrStopActiveTab() },
                onOpenTabSwitcher = { showTabSwitcher = true },
                onToggleOverflow = { overflowExpanded = it },
                onOverflowAction = { action ->
                    overflowExpanded = false
                    when (action) {
                        OverflowAction.NEW_TAB -> viewModel.createNewTab(incognito = false)
                        OverflowAction.NEW_INCOGNITO -> viewModel.createNewTab(incognito = true)
                        OverflowAction.BOOKMARKS -> libraryInitialTab = 0
                        OverflowAction.HISTORY -> libraryInitialTab = 1
                        OverflowAction.FIND_IN_PAGE -> {
                            findQuery = ""
                            viewModel.openFindInPage()
                        }
                        OverflowAction.DESKTOP_SITE -> viewModel.toggleDesktopMode()
                        OverflowAction.SHARE -> {
                            if (activeTab.url.isNotBlank()) {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, activeTab.title)
                                    putExtra(Intent.EXTRA_TEXT, activeTab.url)
                                }
                                runCatching {
                                    context.startActivity(Intent.createChooser(sendIntent, "Share link"))
                                }
                            }
                        }
                        OverflowAction.TUTORIAL -> viewModel.reopenTutorial()
                        OverflowAction.ACCOUNT_SETTINGS -> onOpenAccountSettings()
                    }
                }
            )

            // 2. Loading Progress Bar
            if (activeTab.isLoading && activeTab.url.isNotBlank()) {
                LinearProgressIndicator(
                    progress = { (activeTab.progress / 100f).coerceIn(0.05f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = if (activeTab.isIncognito) IncognitoPurple else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // 3. Find in Page Bar
            AnimatedVisibility(visible = uiState.findInPageVisible && activeTab.url.isNotBlank()) {
                FindInPageBar(
                    query = findQuery,
                    matchCount = uiState.findMatchCount,
                    activeIndex = uiState.findActiveIndex,
                    onQueryChange = { q ->
                        findQuery = q
                        if (q.isNotBlank()) viewModel.searchInPage(q)
                    },
                    onPrevious = { viewModel.findNextInPage(false) },
                    onNext = { viewModel.findNextInPage(true) },
                    onClose = { viewModel.closeFindInPage() }
                )
            }

            // 4. Main Viewport (New Tab Page vs Real WebView)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (activeTab.url.isBlank()) {
                    FireDragonNewTabPage(
                        account = uiState.currentAccount,
                        activeTab = activeTab,
                        uiState = uiState,
                        onNavigateUrl = { url -> viewModel.navigateActiveTab(url) },
                        onRequestDefaultBrowser = onRequestDefaultBrowser,
                        onDismissDefaultBanner = { viewModel.dismissDefaultBrowserBanner() },
                        onOpenShield = { showShieldSheet = true },
                        onOpenAccountSettings = onOpenAccountSettings,
                        onOpenTutorial = { viewModel.reopenTutorial() }
                    )
                } else {
                    BrowserWebViewHost(
                        activeTab = activeTab,
                        shieldConfig = uiState.shieldConfig,
                        whitelistedDomains = uiState.whitelistedDomains,
                        viewModel = viewModel
                    )
                }
            }

            // 5. Chrome-style Bottom Navigation Toolbar
            ChromeBottomNavBar(
                canGoBack = activeTab.canGoBack,
                canGoForward = activeTab.canGoForward,
                isOnNewTabPage = activeTab.url.isBlank(),
                isIncognito = activeTab.isIncognito,
                tabCount = uiState.tabs.size,
                onBack = { viewModel.goBackActiveTab() },
                onForward = { viewModel.goForwardActiveTab() },
                onHome = { viewModel.goHomeNewTabPage() },
                onBookmarks = { libraryInitialTab = 0 },
                onTabs = { showTabSwitcher = true }
            )
        }

        // Overlays & Sheets
        if (showTabSwitcher) {
            TabSwitcherOverlay(
                tabs = uiState.tabs,
                activeTabIndex = uiState.activeTabIndex,
                onSelectTab = { viewModel.selectTab(it) },
                onCloseTab = { viewModel.closeTab(it) },
                onNewTab = { incognito -> viewModel.createNewTab(incognito = incognito) },
                onCloseAllTabs = { viewModel.closeAllTabs() },
                onDismiss = { showTabSwitcher = false }
            )
        }

        if (showShieldSheet) {
            ShieldBottomSheet(
                currentUrl = activeTab.url,
                blockedAdsOnPage = activeTab.blockedAdsOnPage,
                blockedTrackersOnPage = activeTab.blockedTrackersOnPage,
                shieldConfig = uiState.shieldConfig,
                shieldStats = uiState.shieldStats,
                whitelistedDomains = uiState.whitelistedDomains,
                onToggleDomainWhitelist = { viewModel.toggleWhitelistForDomain(it) },
                onToggleAdBlock = { viewModel.toggleAdBlock(it) },
                onToggleYouTubeShield = { viewModel.toggleYouTubeShield(it) },
                onToggleCosmeticFilter = { viewModel.toggleCosmeticFilter(it) },
                onTogglePopupGuard = { viewModel.togglePopupGuard(it) },
                onToggleHttpsUpgrade = { viewModel.toggleHttpsUpgrade(it) },
                onDismiss = { showShieldSheet = false }
            )
        }

        if (libraryInitialTab >= 0) {
            BookmarksHistorySheet(
                initialTab = libraryInitialTab,
                bookmarks = uiState.bookmarks,
                history = uiState.history,
                onOpenUrl = { url -> viewModel.navigateActiveTab(url) },
                onAddBookmark = { title, url -> viewModel.addBookmark(title, url) },
                onDeleteBookmark = { viewModel.deleteBookmark(it) },
                onDeleteHistoryItem = { viewModel.deleteHistoryItem(it) },
                onClearHistory = { viewModel.clearHistory() },
                onDismiss = { libraryInitialTab = -1 }
            )
        }
    }
}

private enum class OverflowAction {
    NEW_TAB, NEW_INCOGNITO, BOOKMARKS, HISTORY, FIND_IN_PAGE, DESKTOP_SITE, SHARE, TUTORIAL, ACCOUNT_SETTINGS
}

@Composable
private fun ChromeOmniboxToolbar(
    activeTab: BrowserTab,
    tabCount: Int,
    omniboxText: String,
    isOmniboxFocused: Boolean,
    isBookmarked: Boolean,
    shieldActive: Boolean,
    overflowExpanded: Boolean,
    onOmniboxTextChange: (String) -> Unit,
    onOmniboxFocusChanged: (Boolean) -> Unit,
    onSubmitOmnibox: () -> Unit,
    onShieldClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    onReloadOrStop: () -> Unit,
    onOpenTabSwitcher: () -> Unit,
    onToggleOverflow: (Boolean) -> Unit,
    onOverflowAction: (OverflowAction) -> Unit
) {
    val pageBlocked = activeTab.blockedAdsOnPage + activeTab.blockedTrackersOnPage
    val barBg = if (activeTab.isIncognito) Color(0xFF1E1928) else MaterialTheme.colorScheme.surface

    Surface(
        color = barBg,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // FireDragon Shield Button
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .clickable(onClick = onShieldClick)
                    .testTag("omnibox_shield_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Open FireDragon Shield",
                    tint = if (shieldActive) ShieldGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                if (pageBlocked > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (pageBlocked > 99) "99+" else pageBlocked.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            // Chrome-style Omnibox Pill
            val pillBg = if (activeTab.isIncognito) Color(0xFF2D253B)
            else MaterialTheme.colorScheme.surfaceVariant

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(pillBg)
                    .border(
                        width = if (isOmniboxFocused) 1.5.dp else 0.5.dp,
                        color = if (isOmniboxFocused) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        activeTab.url.isBlank() -> Icons.Default.Search
                        UrlUtils.isSecureUrl(activeTab.url) -> Icons.Default.Lock
                        else -> Icons.Default.LockOpen
                    },
                    contentDescription = null,
                    tint = when {
                        activeTab.url.isBlank() -> MaterialTheme.colorScheme.onSurfaceVariant
                        UrlUtils.isSecureUrl(activeTab.url) -> ShieldGreen
                        else -> AmberGold
                    },
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    val displayValue = if (isOmniboxFocused || activeTab.url.isBlank()) {
                        omniboxText
                    } else {
                        UrlUtils.extractHost(activeTab.url).ifBlank { activeTab.url }
                    }

                    BasicTextField(
                        value = displayValue,
                        onValueChange = onOmniboxTextChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { onSubmitOmnibox() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { onOmniboxFocusChanged(it.isFocused) }
                            .testTag("omnibox_input")
                    )

                    if (displayValue.isEmpty()) {
                        Text(
                            text = if (activeTab.isIncognito) "Search incognito or type URL"
                            else "Search or type web address",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isOmniboxFocused && omniboxText.isNotEmpty()) {
                    IconButton(
                        onClick = { onOmniboxTextChange("") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear address",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else if (activeTab.url.isNotBlank()) {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.size(32.dp).testTag("omnibox_bookmark_button")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark
                            else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark page",
                            tint = if (isBookmarked) AmberGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (activeTab.url.isNotBlank()) {
                IconButton(
                    onClick = onReloadOrStop,
                    modifier = Modifier.size(44.dp).testTag("omnibox_reload_button")
                ) {
                    Icon(
                        imageVector = if (activeTab.isLoading) Icons.Default.Close else Icons.Default.Refresh,
                        contentDescription = if (activeTab.isLoading) "Stop loading" else "Reload page",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Chrome-style Tab Switcher Badge [N]
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onOpenTabSwitcher)
                    .testTag("omnibox_tab_switcher_button"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(
                            width = 1.8.dp,
                            color = if (activeTab.isIncognito) IncognitoPurple
                            else MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (tabCount > 99) ":D" else tabCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 3-dot Chrome Overflow Menu
            Box {
                IconButton(
                    onClick = { onToggleOverflow(true) },
                    modifier = Modifier.size(44.dp).testTag("omnibox_overflow_button")
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More browser options")
                }

                DropdownMenu(
                    expanded = overflowExpanded,
                    onDismissRequest = { onToggleOverflow(false) }
                ) {
                    DropdownMenuItem(
                        text = { Text("New tab") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        onClick = { onOverflowAction(OverflowAction.NEW_TAB) }
                    )
                    DropdownMenuItem(
                        text = { Text("New incognito tab") },
                        leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                        onClick = { onOverflowAction(OverflowAction.NEW_INCOGNITO) }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Bookmarks") },
                        leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                        onClick = { onOverflowAction(OverflowAction.BOOKMARKS) }
                    )
                    DropdownMenuItem(
                        text = { Text("History") },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                        onClick = { onOverflowAction(OverflowAction.HISTORY) }
                    )
                    if (activeTab.url.isNotBlank()) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Find in page") },
                            leadingIcon = { Icon(Icons.Default.FindInPage, contentDescription = null) },
                            onClick = { onOverflowAction(OverflowAction.FIND_IN_PAGE) }
                        )
                        DropdownMenuItem(
                            text = { Text("Desktop site") },
                            leadingIcon = { Icon(Icons.Default.DesktopWindows, contentDescription = null) },
                            trailingIcon = {
                                Checkbox(
                                    checked = activeTab.isDesktopMode,
                                    onCheckedChange = null
                                )
                            },
                            onClick = { onOverflowAction(OverflowAction.DESKTOP_SITE) }
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = { onOverflowAction(OverflowAction.SHARE) }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Tutorial & Setup") },
                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                        onClick = { onOverflowAction(OverflowAction.TUTORIAL) }
                    )
                    DropdownMenuItem(
                        text = { Text("Account & Settings") },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        onClick = { onOverflowAction(OverflowAction.ACCOUNT_SETTINGS) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FindInPageBar(
    query: String,
    matchCount: Int,
    activeIndex: Int,
    onQueryChange: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .testTag("find_in_page_input")
            ) { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        text = "Find in page...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                innerTextField()
            }

            Text(
                text = if (matchCount == 0) "0/0" else "${activeIndex + 1}/$matchCount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous match")
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next match")
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close find bar")
            }
        }
    }
}

@Composable
private fun FireDragonNewTabPage(
    account: AccountEntity?,
    activeTab: BrowserTab,
    uiState: BrowserUiState,
    onNavigateUrl: (String) -> Unit,
    onRequestDefaultBrowser: () -> Unit,
    onDismissDefaultBanner: () -> Unit,
    onOpenShield: () -> Unit,
    onOpenAccountSettings: () -> Unit,
    onOpenTutorial: () -> Unit
) {
    var ntpSearchText by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = if (maxWidth > 620.dp) 4 else 3

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter)
                .fillMaxSize()
        ) {
            // Account & Incognito Header Pill
            item(span = { GridItemSpan(columns) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onOpenAccountSettings)
                            .testTag("ntp_account_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(account?.avatarColorHex ?: 0xFFFF5722L)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = account?.displayName?.take(1)?.uppercase() ?: "F",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = account?.displayName ?: "FireDragon",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onOpenTutorial,
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(40.dp).testTag("ntp_tutorial_button")
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tutorial", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // Hero Artwork + Live Shield Stats Card
            item(span = { GridItemSpan(columns) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(164.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClick = onOpenShield)
                        .testTag("ntp_hero_shield_card")
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_firedragon_hero_1795447990933),
                        contentDescription = "FireDragon Shield Status",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.2f),
                                        Color.Black.copy(alpha = 0.88f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_dragon_emblem_1795447991126),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (activeTab.isIncognito) "FireDragon Incognito" else "FireDragon",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (uiState.shieldConfig.adBlockEnabled) ShieldGreen.copy(alpha = 0.24f)
                                else Color.White.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = if (uiState.shieldConfig.youtubeShieldEnabled) "YT Shield ON" else "Shield",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (uiState.shieldConfig.adBlockEnabled) ShieldGreen else Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            NtpStatColumn(
                                value = uiState.shieldStats.adsBlockedTotal.toString(),
                                label = "Ads Blocked"
                            )
                            NtpStatColumn(
                                value = uiState.shieldStats.trackersBlockedTotal.toString(),
                                label = "Trackers Stopped"
                            )
                            NtpStatColumn(
                                value = uiState.shieldStats.youtubeAdsBlockedTotal.toString(),
                                label = "YouTube Cleaned"
                            )
                        }
                    }
                }
            }

            // Default Browser Callout Card
            if (!uiState.isDefaultBrowser && !uiState.defaultBannerDismissed) {
                item(span = { GridItemSpan(columns) }) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Set FireDragon as Default Browser",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Open all links with built-in AdBlock and YouTube Clean Shield.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = onDismissDefaultBanner) {
                                    Text("Later")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = onRequestDefaultBrowser,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("ntp_set_default_button")
                                ) {
                                    Text("Set Default")
                                }
                            }
                        }
                    }
                }
            }

            // Chrome-Style Central Search Pill
            item(span = { GridItemSpan(columns) }) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            RoundedCornerShape(28.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            BasicTextField(
                                value = ntpSearchText,
                                onValueChange = { ntpSearchText = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        if (ntpSearchText.isNotBlank()) {
                                            onNavigateUrl(ntpSearchText)
                                        }
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocusRequester)
                                    .testTag("ntp_search_input")
                            )
                            if (ntpSearchText.isEmpty()) {
                                Text(
                                    text = "Search with ${uiState.shieldConfig.searchEngine.label} or enter URL",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (ntpSearchText.isNotBlank()) {
                            IconButton(onClick = { onNavigateUrl(ntpSearchText) }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Go"
                                )
                            }
                        }
                    }
                }
            }

            // Quick YouTube Clean Launch Strip
            item(span = { GridItemSpan(columns) }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateUrl("https://m.youtube.com") }
                        .testTag("ntp_launch_youtube_clean")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Open YouTube with Clean Shield",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Blocks ad banners, promoted cards, and popup prompts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Speed Dial Section Header
            item(span = { GridItemSpan(columns) }) {
                Text(
                    text = "Speed Dial & Bookmarks",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Speed Dial Tiles
            items(uiState.bookmarks, key = { it.id }) { bookmark ->
                SpeedDialCard(
                    bookmark = bookmark,
                    onClick = { onNavigateUrl(bookmark.url) }
                )
            }
        }
    }
}

@Composable
private fun NtpStatColumn(value: String, label: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.78f)
        )
    }
}

@Composable
private fun SpeedDialCard(
    bookmark: BookmarkEntity,
    onClick: () -> Unit
) {
    val initial = bookmark.title.trim().take(1).uppercase().ifBlank { "W" }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("speed_dial_${bookmark.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = bookmark.title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ChromeBottomNavBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    isOnNewTabPage: Boolean,
    isIncognito: Boolean,
    tabCount: Int,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onBookmarks: () -> Unit,
    onTabs: () -> Unit
) {
    Surface(
        color = if (isIncognito) Color(0xFF1E1928) else MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                enabled = canGoBack,
                modifier = Modifier.testTag("bottom_nav_back")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            IconButton(
                onClick = onForward,
                enabled = canGoForward,
                modifier = Modifier.testTag("bottom_nav_forward")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Forward"
                )
            }

            IconButton(
                onClick = onHome,
                enabled = !isOnNewTabPage,
                modifier = Modifier.testTag("bottom_nav_home")
            ) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "New Tab Home"
                )
            }

            IconButton(
                onClick = onBookmarks,
                modifier = Modifier.testTag("bottom_nav_bookmarks")
            ) {
                Icon(
                    Icons.Default.Bookmark,
                    contentDescription = "Bookmarks"
                )
            }

            IconButton(
                onClick = onTabs,
                modifier = Modifier.testTag("bottom_nav_tabs")
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .border(
                            1.8.dp,
                            if (isIncognito) IncognitoPurple else MaterialTheme.colorScheme.onSurface,
                            RoundedCornerShape(5.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (tabCount > 99) ":D" else tabCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
