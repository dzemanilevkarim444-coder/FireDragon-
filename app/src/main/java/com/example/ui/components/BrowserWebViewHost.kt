package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.browser.AdBlockEngine
import com.example.browser.UrlUtils
import com.example.data.ShieldConfig
import com.example.ui.BrowserTab
import com.example.ui.BrowserViewModel
import com.example.ui.WebViewAction

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

private class ThreadSafeShieldSnapshot(
    @Volatile var shieldConfig: ShieldConfig,
    @Volatile var whitelistedDomains: Set<String>,
    @Volatile var currentPageHost: String
)

@Composable
fun BrowserWebViewHost(
    activeTab: BrowserTab,
    shieldConfig: ShieldConfig,
    whitelistedDomains: Set<String>,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val webViewPool = remember { mutableMapOf<String, WebView>() }
    val shieldSnapshots = remember { mutableMapOf<String, ThreadSafeShieldSnapshot>() }

    val latestConfig by rememberUpdatedState(shieldConfig)
    val latestWhitelist by rememberUpdatedState(whitelistedDomains)

    // Keep thread-safe snapshot updated on main thread for WebViewClient background thread reads
    LaunchedEffect(activeTab.id, activeTab.url, shieldConfig, whitelistedDomains) {
        val snap = shieldSnapshots.getOrPut(activeTab.id) {
            ThreadSafeShieldSnapshot(shieldConfig, whitelistedDomains, UrlUtils.extractHost(activeTab.url))
        }
        snap.shieldConfig = shieldConfig
        snap.whitelistedDomains = whitelistedDomains
        if (activeTab.url.isNotBlank()) {
            snap.currentPageHost = UrlUtils.extractHost(activeTab.url)
        }
    }

    // Collect WebView commands from ViewModel
    LaunchedEffect(viewModel) {
        viewModel.webViewActions.collect { action ->
            when (action) {
                is WebViewAction.LoadUrl -> {
                    shieldSnapshots[action.tabId]?.currentPageHost = UrlUtils.extractHost(action.url)
                    webViewPool[action.tabId]?.let { wv ->
                        if (wv.url != action.url) {
                            wv.loadUrl(action.url)
                        }
                    }
                }
                is WebViewAction.GoBack -> {
                    webViewPool[action.tabId]?.takeIf { it.canGoBack() }?.goBack()
                }
                is WebViewAction.GoForward -> {
                    webViewPool[action.tabId]?.takeIf { it.canGoForward() }?.goForward()
                }
                is WebViewAction.Reload -> {
                    webViewPool[action.tabId]?.reload()
                }
                is WebViewAction.Stop -> {
                    webViewPool[action.tabId]?.stopLoading()
                }
                is WebViewAction.SetDesktopMode -> {
                    webViewPool[action.tabId]?.let { wv ->
                        applyDesktopMode(wv, action.enabled)
                        wv.reload()
                    }
                }
                is WebViewAction.FindInPage -> {
                    webViewPool[action.tabId]?.findAllAsync(action.query)
                }
                is WebViewAction.FindNext -> {
                    webViewPool[action.tabId]?.findNext(action.forward)
                }
                is WebViewAction.ClearFind -> {
                    webViewPool[action.tabId]?.clearMatches()
                }
                is WebViewAction.DestroyTab -> {
                    webViewPool.remove(action.tabId)?.let { wv ->
                        wv.stopLoading()
                        wv.destroy()
                    }
                    shieldSnapshots.remove(action.tabId)
                }
                WebViewAction.DestroyAll -> {
                    webViewPool.values.forEach { wv ->
                        wv.stopLoading()
                        wv.destroy()
                    }
                    webViewPool.clear()
                    shieldSnapshots.clear()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewPool.values.forEach { wv ->
                wv.stopLoading()
                wv.destroy()
            }
            webViewPool.clear()
            shieldSnapshots.clear()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("browser_webview"),
            factory = { ctx ->
                val container = android.widget.FrameLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                container
            },
            update = { container ->
                val tabId = activeTab.id
                val targetWebView = webViewPool.getOrPut(tabId) {
                    val snapshot = shieldSnapshots.getOrPut(tabId) {
                        ThreadSafeShieldSnapshot(
                            latestConfig,
                            latestWhitelist,
                            UrlUtils.extractHost(activeTab.url)
                        )
                    }
                    createConfiguredWebView(
                        context = container.context,
                        tabId = tabId,
                        isIncognito = activeTab.isIncognito,
                        isDesktop = activeTab.isDesktopMode,
                        snapshot = snapshot,
                        viewModel = viewModel
                    )
                }

                // Update popup guard dynamically
                targetWebView.settings.javaScriptCanOpenWindowsAutomatically = !latestConfig.popupGuardEnabled

                if (container.childCount != 1 || container.getChildAt(0) !== targetWebView) {
                    container.removeAllViews()
                    (targetWebView.parent as? ViewGroup)?.removeView(targetWebView)
                    container.addView(
                        targetWebView,
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                }

                if (activeTab.url.isNotBlank() && targetWebView.url == null) {
                    targetWebView.loadUrl(activeTab.url)
                }
            }
        )

        // Safe SSL Warning Overlay
        if (activeTab.sslErrorHost != null) {
            SslWarningOverlay(
                host = activeTab.sslErrorHost,
                onBackToSafety = {
                    if (activeTab.canGoBack) viewModel.goBackActiveTab()
                    else viewModel.goHomeNewTabPage()
                }
            )
        } else if (activeTab.errorMessage != null) {
            WebErrorOverlay(
                url = activeTab.url,
                errorMessage = activeTab.errorMessage,
                onRetry = { viewModel.reloadOrStopActiveTab() },
                onHome = { viewModel.goHomeNewTabPage() }
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createConfiguredWebView(
    context: android.content.Context,
    tabId: String,
    isIncognito: Boolean,
    isDesktop: Boolean,
    snapshot: ThreadSafeShieldSnapshot,
    viewModel: BrowserViewModel
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = !isIncognito
            databaseEnabled = !isIncognito
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = !snapshot.shieldConfig.popupGuardEnabled
            if (isIncognito) {
                cacheMode = WebSettings.LOAD_NO_CACHE
            }
        }

        CookieManager.getInstance().setAcceptCookie(!isIncognito)
        if (isDesktop) {
            applyDesktopMode(this, true)
        }

        setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
            viewModel.onFindResultsUpdated(activeMatchOrdinal, numberOfMatches)
        }

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                viewModel.onProgressChanged(tabId, newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) {
                    viewModel.onTitleReceived(tabId, title)
                }
            }
        }

        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val reqUrl = request?.url?.toString() ?: return null
                // Never inspect WebView.url here (background thread); read from thread-safe snapshot
                val decision = AdBlockEngine.evaluateRequest(
                    requestUrl = reqUrl,
                    pageHost = snapshot.currentPageHost,
                    shieldConfig = snapshot.shieldConfig,
                    whitelistedDomains = snapshot.whitelistedDomains
                )
                return if (decision.blocked) {
                    viewModel.onRequestBlocked(tabId, decision.category)
                    AdBlockEngine.createEmptyResponse()
                } else {
                    super.shouldInterceptRequest(view, request)
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                val scheme = uri.scheme?.lowercase() ?: return false

                if (scheme == "http" || scheme == "https") {
                    if (scheme == "http" && snapshot.shieldConfig.httpsUpgradeEnabled &&
                        uri.host?.lowercase() !in setOf("localhost", "127.0.0.1")
                    ) {
                        val upgraded = uri.buildUpon().scheme("https").build().toString()
                        snapshot.currentPageHost = UrlUtils.extractHost(upgraded)
                        view?.loadUrl(upgraded)
                        return true
                    }
                    return false
                }

                // Safely hand off external schemes (tel:, mailto:, intent:) without crashing
                return try {
                    val intent = if (scheme == "intent") {
                        Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                    } else {
                        Intent(Intent.ACTION_VIEW, uri)
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    true
                } catch (_: Exception) {
                    viewModel.onPopupBlocked()
                    true
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                val safeUrl = url ?: return
                snapshot.currentPageHost = UrlUtils.extractHost(safeUrl)
                viewModel.onPageStarted(
                    tabId = tabId,
                    url = safeUrl,
                    canGoBack = view?.canGoBack() ?: false,
                    canGoForward = view?.canGoForward() ?: false
                )
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                val host = url?.let { UrlUtils.extractHost(it) } ?: snapshot.currentPageHost
                val script = AdBlockEngine.buildCosmeticScript(
                    pageHost = host,
                    shieldConfig = snapshot.shieldConfig,
                    whitelistedDomains = snapshot.whitelistedDomains
                )
                if (script != null) {
                    view?.evaluateJavascript(script, null)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                val safeUrl = url ?: return
                snapshot.currentPageHost = UrlUtils.extractHost(safeUrl)
                val script = AdBlockEngine.buildCosmeticScript(
                    pageHost = snapshot.currentPageHost,
                    shieldConfig = snapshot.shieldConfig,
                    whitelistedDomains = snapshot.whitelistedDomains
                )
                if (script != null) {
                    view?.evaluateJavascript(script, null)
                }
                viewModel.onPageFinished(
                    tabId = tabId,
                    url = safeUrl,
                    title = view?.title,
                    canGoBack = view?.canGoBack() ?: false,
                    canGoForward = view?.canGoForward() ?: false
                )
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    val msg = error?.description?.toString()?.takeIf { it.isNotBlank() }
                        ?: "Unable to reach this page. Check your connection."
                    viewModel.onWebViewError(tabId, msg)
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                // Never auto-proceed on SSL errors
                handler?.cancel()
                val host = error?.url?.let { UrlUtils.extractHost(it) } ?: "this site"
                viewModel.onSslError(tabId, host)
            }
        }
    }
}

private fun applyDesktopMode(webView: WebView, enabled: Boolean) {
    val defaultUa = WebSettings.getDefaultUserAgent(webView.context)
    webView.settings.userAgentString = if (enabled) DESKTOP_USER_AGENT else defaultUa
    webView.settings.useWideViewPort = true
    webView.settings.loadWithOverviewMode = true
}

@Composable
private fun WebErrorOverlay(
    url: String,
    errorMessage: String,
    onRetry: () -> Unit,
    onHome: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = "Page load error",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "This site can't be reached",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${UrlUtils.extractHost(url).ifBlank { url }}\n$errorMessage",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onHome,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("New Tab")
                    }
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("webview_retry_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reload")
                    }
                }
            }
        }
    }
}

@Composable
private fun SslWarningOverlay(
    host: String,
    onBackToSafety: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Security warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Connection is not private",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "FireDragon blocked an unsafe SSL certificate on $host to protect your account and browsing data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onBackToSafety,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Back to Safety")
                }
            }
        }
    }
}
