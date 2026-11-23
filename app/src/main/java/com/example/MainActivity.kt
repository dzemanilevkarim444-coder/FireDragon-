package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.browser.DefaultBrowserHelper
import com.example.data.FireDragonDatabase
import com.example.data.FireDragonRepository
import com.example.data.PreferencesManager
import com.example.data.ThemeMode
import com.example.ui.BrowserViewModel
import com.example.ui.screens.AccountAndSettingsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.TutorialSetupScreen
import com.example.ui.theme.FireDragonTheme

const val ROUTE_BROWSER = "browser"
const val ROUTE_SETTINGS = "settings"

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: BrowserViewModel

    private val defaultBrowserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.updateIsDefaultBrowser(DefaultBrowserHelper.isDefaultBrowser(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = FireDragonDatabase.getInstance(applicationContext)
        val prefs = PreferencesManager(applicationContext)
        val repository = FireDragonRepository(db, prefs)
        viewModel = ViewModelProvider(
            this,
            BrowserViewModel.Factory(repository)
        )[BrowserViewModel::class.java]

        viewModel.updateIsDefaultBrowser(DefaultBrowserHelper.isDefaultBrowser(this))
        handleIncomingIntent(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(viewModel) {
                viewModel.toastEvents.collect { message ->
                    snackbarHostState.showSnackbar(message)
                }
            }

            val darkTheme = when (uiState.shieldConfig.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            FireDragonTheme(darkTheme = darkTheme) {
                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    containerColor = MaterialTheme.colorScheme.background
                ) { _ ->
                    when {
                        !uiState.isSessionLoaded -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        uiState.currentAccount == null -> {
                            AuthScreen(
                                authFormState = uiState.authFormState,
                                savedAccounts = uiState.allAccounts,
                                onSignIn = { email, pass -> viewModel.signIn(email, pass) },
                                onCreateAccount = { name, email, pass ->
                                    viewModel.registerAccount(name, email, pass)
                                },
                                onQuickSwitchAccount = { viewModel.switchAccount(it) },
                                onContinueAsGuest = { viewModel.continueAsGuest() },
                                onClearError = { viewModel.clearAuthError() }
                            )
                        }

                        !uiState.hasCompletedTutorial || uiState.forceTutorialVisible -> {
                            TutorialSetupScreen(
                                displayName = uiState.currentAccount?.displayName ?: "Explorer",
                                shieldConfig = uiState.shieldConfig,
                                isDefaultBrowser = uiState.isDefaultBrowser,
                                onToggleAdBlock = { viewModel.toggleAdBlock(it) },
                                onToggleYouTubeShield = { viewModel.toggleYouTubeShield(it) },
                                onTogglePopupGuard = { viewModel.togglePopupGuard(it) },
                                onToggleHttpsUpgrade = { viewModel.toggleHttpsUpgrade(it) },
                                onRequestDefaultBrowser = { triggerDefaultBrowserPrompt() },
                                onOpenDefaultAppsSettings = {
                                    DefaultBrowserHelper.openDefaultAppsSettings(this@MainActivity)
                                },
                                onFinishTutorial = { viewModel.completeTutorial() }
                            )
                        }

                        else -> {
                            val navController = rememberNavController()
                            NavHost(
                                navController = navController,
                                startDestination = ROUTE_BROWSER,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                composable(ROUTE_BROWSER) {
                                    BrowserScreen(
                                        uiState = uiState,
                                        viewModel = viewModel,
                                        onRequestDefaultBrowser = { triggerDefaultBrowserPrompt() },
                                        onOpenAccountSettings = {
                                            navController.navigate(ROUTE_SETTINGS) {
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                                composable(ROUTE_SETTINGS) {
                                    AccountAndSettingsScreen(
                                        uiState = uiState,
                                        onBack = { navController.popBackStack() },
                                        onUpdateDisplayName = { viewModel.updateDisplayName(it) },
                                        onSwitchAccount = { viewModel.switchAccount(it) },
                                        onSignOut = { viewModel.signOut() },
                                        onDeleteAccount = { viewModel.deleteAccount(it) },
                                        onRequestDefaultBrowser = { triggerDefaultBrowserPrompt() },
                                        onOpenDefaultAppsSettings = {
                                            DefaultBrowserHelper.openDefaultAppsSettings(this@MainActivity)
                                        },
                                        onSelectSearchEngine = { viewModel.setSearchEngine(it) },
                                        onSelectThemeMode = { viewModel.setThemeMode(it) },
                                        onToggleAdBlock = { viewModel.toggleAdBlock(it) },
                                        onToggleYouTubeShield = { viewModel.toggleYouTubeShield(it) },
                                        onToggleHttpsUpgrade = { viewModel.toggleHttpsUpgrade(it) },
                                        onReopenTutorial = { viewModel.reopenTutorial() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.updateIsDefaultBrowser(DefaultBrowserHelper.isDefaultBrowser(this))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val url = intent.dataString
            if (!url.isNullOrBlank()) {
                viewModel.openExternalUrl(url)
            }
        }
    }

    private fun triggerDefaultBrowserPrompt() {
        val roleIntent = DefaultBrowserHelper.createDefaultBrowserRoleIntent(this)
        if (roleIntent != null) {
            defaultBrowserLauncher.launch(roleIntent)
        } else {
            DefaultBrowserHelper.openDefaultAppsSettings(this)
        }
    }
}
