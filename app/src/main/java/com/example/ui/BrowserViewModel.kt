package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.browser.BlockCategory
import com.example.browser.UrlUtils
import com.example.data.AccountEntity
import com.example.data.BookmarkEntity
import com.example.data.FireDragonRepository
import com.example.data.HistoryEntity
import com.example.data.SearchEngine
import com.example.data.ShieldConfig
import com.example.data.ShieldStatsEntity
import com.example.data.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "",
    val title: String = "New Tab",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isDesktopMode: Boolean = false,
    val isIncognito: Boolean = false,
    val blockedAdsOnPage: Int = 0,
    val blockedTrackersOnPage: Int = 0,
    val errorMessage: String? = null,
    val sslErrorHost: String? = null
)

sealed interface WebViewAction {
    data class LoadUrl(val tabId: String, val url: String) : WebViewAction
    data class GoBack(val tabId: String) : WebViewAction
    data class GoForward(val tabId: String) : WebViewAction
    data class Reload(val tabId: String) : WebViewAction
    data class Stop(val tabId: String) : WebViewAction
    data class SetDesktopMode(val tabId: String, val enabled: Boolean) : WebViewAction
    data class FindInPage(val tabId: String, val query: String) : WebViewAction
    data class FindNext(val tabId: String, val forward: Boolean) : WebViewAction
    data class ClearFind(val tabId: String) : WebViewAction
    data class DestroyTab(val tabId: String) : WebViewAction
    data object DestroyAll : WebViewAction
}

data class AuthFormState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class BrowserUiState(
    val isSessionLoaded: Boolean = false,
    val currentAccount: AccountEntity? = null,
    val allAccounts: List<AccountEntity> = emptyList(),
    val hasCompletedTutorial: Boolean = false,
    val forceTutorialVisible: Boolean = false,
    val defaultBannerDismissed: Boolean = false,
    val isDefaultBrowser: Boolean = false,
    val shieldConfig: ShieldConfig = ShieldConfig(),
    val shieldStats: ShieldStatsEntity = ShieldStatsEntity(0L),
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val history: List<HistoryEntity> = emptyList(),
    val whitelistedDomains: Set<String> = emptySet(),
    val tabs: List<BrowserTab> = listOf(BrowserTab()),
    val activeTabIndex: Int = 0,
    val authFormState: AuthFormState = AuthFormState(),
    val findInPageVisible: Boolean = false,
    val findMatchCount: Int = 0,
    val findActiveIndex: Int = 0
) {
    val activeTab: BrowserTab
        get() = tabs.getOrElse(activeTabIndex) { tabs.first() }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BrowserViewModel(
    private val repository: FireDragonRepository
) : ViewModel() {

    private val _tabsState = MutableStateFlow(listOf(BrowserTab()))
    private val _activeTabIndex = MutableStateFlow(0)
    private val _authFormState = MutableStateFlow(AuthFormState())
    private val _isDefaultBrowser = MutableStateFlow(false)
    private val _forceTutorialVisible = MutableStateFlow(false)
    private val _findInPageState = MutableStateFlow(Triple(false, 0, 0))
    private val _pendingExternalUrl = MutableStateFlow<String?>(null)

    private val _webViewActions = MutableSharedFlow<WebViewAction>(extraBufferCapacity = 32)
    val webViewActions = _webViewActions.asSharedFlow()

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val toastEvents = _toastEvents.asSharedFlow()

    private val activeAccountFlow = repository.sessionPrefs.flatMapLatest { prefs ->
        if (prefs.activeAccountId == null) flowOf(null)
        else repository.observeAccount(prefs.activeAccountId)
    }

    private val accountDataFlow = activeAccountFlow.flatMapLatest { account ->
        if (account == null) {
            flowOf(AccountScopedData())
        } else {
            combine(
                repository.observeBookmarks(account.id),
                repository.observeHistory(account.id),
                repository.observeWhitelistedDomains(account.id),
                repository.observeShieldStats(account.id)
            ) { bookmarks, history, whitelist, stats ->
                AccountScopedData(
                    bookmarks = bookmarks,
                    history = history,
                    whitelistedDomains = whitelist,
                    shieldStats = stats
                )
            }
        }
    }

    private data class AccountScopedData(
        val bookmarks: List<BookmarkEntity> = emptyList(),
        val history: List<HistoryEntity> = emptyList(),
        val whitelistedDomains: Set<String> = emptySet(),
        val shieldStats: ShieldStatsEntity = ShieldStatsEntity(0L)
    )

    private val coreSessionFlow = combine(
        repository.sessionPrefs,
        activeAccountFlow,
        repository.allAccounts,
        repository.shieldConfig,
        accountDataFlow
    ) { sessionPrefs, account, allAccounts, shieldConfig, scoped ->
        val completed = account != null &&
            sessionPrefs.completedTutorialAccountIds.contains(account.id.toString())
        CoreSessionSnapshot(
            isSessionLoaded = true,
            currentAccount = account,
            allAccounts = allAccounts,
            hasCompletedTutorial = completed,
            defaultBannerDismissed = sessionPrefs.defaultBannerDismissed,
            shieldConfig = shieldConfig,
            scopedData = scoped
        )
    }

    private data class CoreSessionSnapshot(
        val isSessionLoaded: Boolean,
        val currentAccount: AccountEntity?,
        val allAccounts: List<AccountEntity>,
        val hasCompletedTutorial: Boolean,
        val defaultBannerDismissed: Boolean,
        val shieldConfig: ShieldConfig,
        val scopedData: AccountScopedData
    )

    private val localUiFlow = combine(
        _tabsState,
        _activeTabIndex,
        _authFormState,
        _isDefaultBrowser,
        _forceTutorialVisible,
        _findInPageState
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val tabs = arr[0] as List<BrowserTab>
        val activeIdx = arr[1] as Int
        val auth = arr[2] as AuthFormState
        val isDefault = arr[3] as Boolean
        val forceTut = arr[4] as Boolean
        @Suppress("UNCHECKED_CAST")
        val findTuple = arr[5] as Triple<Boolean, Int, Int>
        LocalUiSnapshot(tabs, activeIdx, auth, isDefault, forceTut, findTuple)
    }

    private data class LocalUiSnapshot(
        val tabs: List<BrowserTab>,
        val activeTabIndex: Int,
        val authFormState: AuthFormState,
        val isDefaultBrowser: Boolean,
        val forceTutorialVisible: Boolean,
        val findTuple: Triple<Boolean, Int, Int>
    )

    val uiState: StateFlow<BrowserUiState> = combine(
        coreSessionFlow,
        localUiFlow
    ) { core, local ->
        val safeIdx = local.activeTabIndex.coerceIn(0, (local.tabs.size - 1).coerceAtLeast(0))
        BrowserUiState(
            isSessionLoaded = core.isSessionLoaded,
            currentAccount = core.currentAccount,
            allAccounts = core.allAccounts,
            hasCompletedTutorial = core.hasCompletedTutorial,
            forceTutorialVisible = local.forceTutorialVisible,
            defaultBannerDismissed = core.defaultBannerDismissed,
            isDefaultBrowser = local.isDefaultBrowser,
            shieldConfig = core.shieldConfig,
            shieldStats = core.scopedData.shieldStats,
            bookmarks = core.scopedData.bookmarks,
            history = core.scopedData.history,
            whitelistedDomains = core.scopedData.whitelistedDomains,
            tabs = local.tabs,
            activeTabIndex = safeIdx,
            authFormState = local.authFormState,
            findInPageVisible = local.findTuple.first,
            findMatchCount = local.findTuple.second,
            findActiveIndex = local.findTuple.third
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = BrowserUiState()
    )

    // --- Authentication Actions ---

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authFormState.value = AuthFormState(isLoading = true, errorMessage = null)
            val result = repository.signIn(email, password)
            result.onSuccess {
                _authFormState.value = AuthFormState()
                consumePendingExternalUrlIfAny()
            }.onFailure { err ->
                _authFormState.value = AuthFormState(
                    isLoading = false,
                    errorMessage = err.message ?: "Unable to sign in."
                )
            }
        }
    }

    fun registerAccount(displayName: String, email: String, password: String) {
        viewModelScope.launch {
            _authFormState.value = AuthFormState(isLoading = true, errorMessage = null)
            val result = repository.registerAccount(displayName, email, password)
            result.onSuccess {
                _authFormState.value = AuthFormState()
                consumePendingExternalUrlIfAny()
            }.onFailure { err ->
                _authFormState.value = AuthFormState(
                    isLoading = false,
                    errorMessage = err.message ?: "Unable to create account."
                )
            }
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            _authFormState.value = AuthFormState(isLoading = true, errorMessage = null)
            repository.continueAsGuest()
            _authFormState.value = AuthFormState()
            consumePendingExternalUrlIfAny()
        }
    }

    fun switchAccount(accountId: Long) {
        viewModelScope.launch {
            _webViewActions.tryEmit(WebViewAction.DestroyAll)
            _tabsState.value = listOf(BrowserTab())
            _activeTabIndex.value = 0
            repository.switchAccount(accountId)
            _toastEvents.tryEmit("Switched FireDragon profile")
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _webViewActions.tryEmit(WebViewAction.DestroyAll)
            _tabsState.value = listOf(BrowserTab())
            _activeTabIndex.value = 0
            _forceTutorialVisible.value = false
            repository.signOut()
        }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            _webViewActions.tryEmit(WebViewAction.DestroyAll)
            _tabsState.value = listOf(BrowserTab())
            _activeTabIndex.value = 0
            _forceTutorialVisible.value = false
            repository.deleteAccount(accountId)
            _toastEvents.tryEmit("Account removed")
        }
    }

    fun updateDisplayName(newName: String) {
        val accountId = uiState.value.currentAccount?.id ?: return
        viewModelScope.launch {
            repository.updateDisplayName(accountId, newName)
            _toastEvents.tryEmit("Profile updated")
        }
    }

    fun clearAuthError() {
        _authFormState.update { it.copy(errorMessage = null) }
    }

    // --- Tutorial & Default Browser ---

    fun completeTutorial() {
        val accountId = uiState.value.currentAccount?.id ?: return
        viewModelScope.launch {
            repository.markTutorialCompleted(accountId)
            _forceTutorialVisible.value = false
            consumePendingExternalUrlIfAny()
        }
    }

    fun reopenTutorial() {
        _forceTutorialVisible.value = true
    }

    fun dismissDefaultBrowserBanner() {
        viewModelScope.launch {
            repository.setDefaultBannerDismissed(true)
        }
    }

    fun updateIsDefaultBrowser(isDefault: Boolean) {
        _isDefaultBrowser.value = isDefault
    }

    // --- Shield & Browser Settings ---

    fun toggleAdBlock(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateShieldConfig { it.copy(adBlockEnabled = enabled) }
        }
    }

    fun toggleYouTubeShield(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateShieldConfig { it.copy(youtubeShieldEnabled = enabled) }
        }
    }

    fun toggleCosmeticFilter(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateShieldConfig { it.copy(cosmeticFilterEnabled = enabled) }
        }
    }

    fun togglePopupGuard(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateShieldConfig { it.copy(popupGuardEnabled = enabled) }
        }
    }

    fun toggleHttpsUpgrade(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateShieldConfig { it.copy(httpsUpgradeEnabled = enabled) }
        }
    }

    fun setSearchEngine(engine: SearchEngine) {
        viewModelScope.launch {
            repository.updateShieldConfig { it.copy(searchEngine = engine) }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.updateShieldConfig { it.copy(themeMode = mode) }
        }
    }

    fun toggleWhitelistForDomain(domain: String) {
        val accountId = uiState.value.currentAccount?.id ?: return
        val cleanDomain = domain.lowercase().removePrefix("www.")
        if (cleanDomain.isBlank()) return
        val currentlyWhitelisted = uiState.value.whitelistedDomains.contains(cleanDomain)
        viewModelScope.launch {
            repository.toggleDomainWhitelist(accountId, cleanDomain, currentlyWhitelisted)
            val activeTab = uiState.value.activeTab
            if (activeTab.url.isNotBlank()) {
                _webViewActions.tryEmit(WebViewAction.Reload(activeTab.id))
            }
            _toastEvents.tryEmit(
                if (currentlyWhitelisted) "Shields enabled for $cleanDomain"
                else "Shields paused for $cleanDomain"
            )
        }
    }

    // --- Tab & Navigation Actions ---

    fun openExternalUrl(rawUrl: String) {
        val clean = rawUrl.trim()
        if (clean.isBlank()) return
        if (uiState.value.currentAccount == null) {
            _pendingExternalUrl.value = clean
            return
        }
        val active = uiState.value.activeTab
        if (active.url.isBlank()) {
            navigateActiveTab(clean)
        } else {
            createNewTab(initialUrl = clean, incognito = false)
        }
    }

    private fun consumePendingExternalUrlIfAny() {
        val pending = _pendingExternalUrl.value ?: return
        _pendingExternalUrl.value = null
        openExternalUrl(pending)
    }

    fun navigateActiveTab(rawInput: String) {
        val config = uiState.value.shieldConfig
        val resolved = UrlUtils.resolveOmniboxInput(
            rawInput = rawInput,
            searchEngine = config.searchEngine,
            httpsUpgrade = config.httpsUpgradeEnabled
        )
        if (resolved.isBlank()) return
        val tab = uiState.value.activeTab
        _tabsState.update { tabs ->
            tabs.map {
                if (it.id == tab.id) {
                    it.copy(
                        url = resolved,
                        title = UrlUtils.extractHost(resolved).ifBlank { resolved },
                        isLoading = true,
                        progress = 10,
                        blockedAdsOnPage = 0,
                        blockedTrackersOnPage = 0,
                        errorMessage = null,
                        sslErrorHost = null
                    )
                } else it
            }
        }
        _webViewActions.tryEmit(WebViewAction.LoadUrl(tab.id, resolved))
    }

    fun goHomeNewTabPage() {
        val tab = uiState.value.activeTab
        _webViewActions.tryEmit(WebViewAction.Stop(tab.id))
        _webViewActions.tryEmit(WebViewAction.DestroyTab(tab.id))
        _tabsState.update { tabs ->
            tabs.map {
                if (it.id == tab.id) {
                    it.copy(
                        url = "",
                        title = if (it.isIncognito) "Incognito Tab" else "New Tab",
                        isLoading = false,
                        progress = 0,
                        canGoBack = false,
                        canGoForward = false,
                        blockedAdsOnPage = 0,
                        blockedTrackersOnPage = 0,
                        errorMessage = null,
                        sslErrorHost = null
                    )
                } else it
            }
        }
    }

    fun createNewTab(initialUrl: String = "", incognito: Boolean = false) {
        val config = uiState.value.shieldConfig
        val resolved = if (initialUrl.isBlank()) "" else UrlUtils.resolveOmniboxInput(
            rawInput = initialUrl,
            searchEngine = config.searchEngine,
            httpsUpgrade = config.httpsUpgradeEnabled
        )
        val newTab = BrowserTab(
            url = resolved,
            title = when {
                resolved.isNotBlank() -> UrlUtils.extractHost(resolved).ifBlank { resolved }
                incognito -> "Incognito Tab"
                else -> "New Tab"
            },
            isLoading = resolved.isNotBlank(),
            progress = if (resolved.isNotBlank()) 10 else 0,
            isIncognito = incognito
        )
        _tabsState.update { it + newTab }
        _activeTabIndex.value = _tabsState.value.lastIndex
        closeFindInPage()
    }

    fun selectTab(index: Int) {
        if (index in _tabsState.value.indices) {
            _activeTabIndex.value = index
            closeFindInPage()
        }
    }

    fun closeTab(tabId: String) {
        _webViewActions.tryEmit(WebViewAction.DestroyTab(tabId))
        val currentTabs = _tabsState.value
        val closingIdx = currentTabs.indexOfFirst { it.id == tabId }
        if (closingIdx == -1) return

        val remaining = currentTabs.filterNot { it.id == tabId }
        if (remaining.isEmpty()) {
            _tabsState.value = listOf(BrowserTab())
            _activeTabIndex.value = 0
        } else {
            val currentIdx = _activeTabIndex.value
            val nextIdx = when {
                closingIdx < currentIdx -> currentIdx - 1
                currentIdx >= remaining.size -> remaining.lastIndex
                else -> currentIdx
            }.coerceIn(0, remaining.lastIndex)
            _tabsState.value = remaining
            _activeTabIndex.value = nextIdx
        }
        closeFindInPage()
    }

    fun closeAllTabs() {
        _webViewActions.tryEmit(WebViewAction.DestroyAll)
        _tabsState.value = listOf(BrowserTab())
        _activeTabIndex.value = 0
        closeFindInPage()
    }

    fun goBackActiveTab() {
        val tab = uiState.value.activeTab
        if (tab.canGoBack) {
            _webViewActions.tryEmit(WebViewAction.GoBack(tab.id))
        }
    }

    fun goForwardActiveTab() {
        val tab = uiState.value.activeTab
        if (tab.canGoForward) {
            _webViewActions.tryEmit(WebViewAction.GoForward(tab.id))
        }
    }

    fun reloadOrStopActiveTab() {
        val tab = uiState.value.activeTab
        if (tab.url.isBlank()) return
        if (tab.isLoading) {
            _webViewActions.tryEmit(WebViewAction.Stop(tab.id))
        } else {
            _tabsState.update { tabs ->
                tabs.map { if (it.id == tab.id) it.copy(errorMessage = null, sslErrorHost = null) else it }
            }
            _webViewActions.tryEmit(WebViewAction.Reload(tab.id))
        }
    }

    fun toggleDesktopMode() {
        val tab = uiState.value.activeTab
        val next = !tab.isDesktopMode
        _tabsState.update { tabs ->
            tabs.map { if (it.id == tab.id) it.copy(isDesktopMode = next) else it }
        }
        _webViewActions.tryEmit(WebViewAction.SetDesktopMode(tab.id, next))
        _toastEvents.tryEmit(if (next) "Desktop site enabled" else "Mobile site restored")
    }

    // --- Find in Page ---

    fun openFindInPage() {
        _findInPageState.value = Triple(true, 0, 0)
    }

    fun closeFindInPage() {
        val activeId = uiState.value.activeTab.id
        _webViewActions.tryEmit(WebViewAction.ClearFind(activeId))
        _findInPageState.value = Triple(false, 0, 0)
    }

    fun searchInPage(query: String) {
        val activeId = uiState.value.activeTab.id
        _webViewActions.tryEmit(WebViewAction.FindInPage(activeId, query))
    }

    fun findNextInPage(forward: Boolean) {
        val activeId = uiState.value.activeTab.id
        _webViewActions.tryEmit(WebViewAction.FindNext(activeId, forward))
    }

    fun onFindResultsUpdated(activeMatchOrdinal: Int, numberOfMatches: Int) {
        _findInPageState.update { Triple(it.first, numberOfMatches, activeMatchOrdinal) }
    }

    // --- WebView Callbacks ---

    fun onPageStarted(tabId: String, url: String, canGoBack: Boolean, canGoForward: Boolean) {
        _tabsState.update { tabs ->
            tabs.map {
                if (it.id == tabId) {
                    it.copy(
                        url = url,
                        isLoading = true,
                        progress = 12,
                        canGoBack = canGoBack,
                        canGoForward = canGoForward,
                        blockedAdsOnPage = 0,
                        blockedTrackersOnPage = 0,
                        errorMessage = null,
                        sslErrorHost = null
                    )
                } else it
            }
        }
    }

    fun onProgressChanged(tabId: String, progress: Int) {
        _tabsState.update { tabs ->
            tabs.map {
                if (it.id == tabId) {
                    it.copy(progress = progress, isLoading = progress < 100)
                } else it
            }
        }
    }

    fun onTitleReceived(tabId: String, title: String) {
        if (title.isBlank()) return
        _tabsState.update { tabs ->
            tabs.map { if (it.id == tabId) it.copy(title = title) else it }
        }
    }

    fun onPageFinished(
        tabId: String,
        url: String,
        title: String?,
        canGoBack: Boolean,
        canGoForward: Boolean
    ) {
        var isIncognito = false
        _tabsState.update { tabs ->
            tabs.map {
                if (it.id == tabId) {
                    isIncognito = it.isIncognito
                    it.copy(
                        url = url,
                        title = title?.takeIf { t -> t.isNotBlank() } ?: it.title,
                        isLoading = false,
                        progress = 100,
                        canGoBack = canGoBack,
                        canGoForward = canGoForward
                    )
                } else it
            }
        }
        val accountId = uiState.value.currentAccount?.id
        if (accountId != null && !isIncognito && url.isNotBlank()) {
            viewModelScope.launch {
                repository.recordHistoryVisit(
                    accountId = accountId,
                    title = title?.takeIf { it.isNotBlank() } ?: UrlUtils.extractHost(url),
                    url = url
                )
            }
        }
    }

    fun onWebViewError(tabId: String, description: String) {
        _tabsState.update { tabs ->
            tabs.map {
                if (it.id == tabId) {
                    it.copy(isLoading = false, progress = 100, errorMessage = description)
                } else it
            }
        }
    }

    fun onSslError(tabId: String, host: String) {
        _tabsState.update { tabs ->
            tabs.map {
                if (it.id == tabId) {
                    it.copy(isLoading = false, progress = 100, sslErrorHost = host)
                } else it
            }
        }
    }

    fun onRequestBlocked(tabId: String, category: BlockCategory) {
        if (category == BlockCategory.NONE) return
        _tabsState.update { tabs ->
            tabs.map {
                if (it.id == tabId) {
                    when (category) {
                        BlockCategory.AD, BlockCategory.YOUTUBE_AD ->
                            it.copy(blockedAdsOnPage = it.blockedAdsOnPage + 1)
                        BlockCategory.TRACKER ->
                            it.copy(blockedTrackersOnPage = it.blockedTrackersOnPage + 1)
                        BlockCategory.NONE -> it
                    }
                } else it
            }
        }
        val accountId = uiState.value.currentAccount?.id ?: return
        viewModelScope.launch {
            when (category) {
                BlockCategory.AD -> repository.recordBlockedEvents(accountId, ads = 1L)
                BlockCategory.TRACKER -> repository.recordBlockedEvents(accountId, trackers = 1L)
                BlockCategory.YOUTUBE_AD -> repository.recordBlockedEvents(accountId, ads = 1L, youtubeAds = 1L)
                BlockCategory.NONE -> Unit
            }
        }
    }

    fun onPopupBlocked() {
        val accountId = uiState.value.currentAccount?.id ?: return
        viewModelScope.launch {
            repository.recordBlockedEvents(accountId, popups = 1L)
        }
    }

    // --- Bookmarks & History ---

    fun toggleBookmarkActivePage() {
        val accountId = uiState.value.currentAccount?.id ?: return
        val tab = uiState.value.activeTab
        if (tab.url.isBlank()) return
        viewModelScope.launch {
            val added = repository.toggleBookmark(accountId, tab.title, tab.url)
            _toastEvents.tryEmit(if (added) "Bookmark saved" else "Bookmark removed")
        }
    }

    fun addBookmark(title: String, url: String) {
        val accountId = uiState.value.currentAccount?.id ?: return
        val config = uiState.value.shieldConfig
        val resolved = UrlUtils.resolveOmniboxInput(url, config.searchEngine, config.httpsUpgradeEnabled)
        if (resolved.isBlank()) return
        viewModelScope.launch {
            repository.addBookmark(accountId, title, resolved)
            _toastEvents.tryEmit("Bookmark added")
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            repository.deleteBookmark(id)
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
        }
    }

    fun clearHistory() {
        val accountId = uiState.value.currentAccount?.id ?: return
        viewModelScope.launch {
            repository.clearHistory(accountId)
            _toastEvents.tryEmit("Browsing history cleared")
        }
    }

    class Factory(private val repository: FireDragonRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BrowserViewModel(repository) as T
        }
    }
}
