package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ShieldConfig
import com.example.ui.theme.ShieldGreen
import kotlinx.coroutines.launch

@Composable
fun TutorialSetupScreen(
    displayName: String,
    shieldConfig: ShieldConfig,
    isDefaultBrowser: Boolean,
    onToggleAdBlock: (Boolean) -> Unit,
    onToggleYouTubeShield: (Boolean) -> Unit,
    onTogglePopupGuard: (Boolean) -> Unit,
    onToggleHttpsUpgrade: (Boolean) -> Unit,
    onRequestDefaultBrowser: () -> Unit,
    onOpenDefaultAppsSettings: () -> Unit,
    onFinishTutorial: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val scope = rememberCoroutineScope()

    BackHandler {
        if (pagerState.currentPage > 0) {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
        } else {
            onFinishTutorial()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar with step badge and Skip
            Row(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Setup ${pagerState.currentPage + 1} of 5",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                TextButton(
                    onClick = onFinishTutorial,
                    modifier = Modifier.testTag("tutorial_skip_button")
                ) {
                    Text("Skip Setup")
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> WelcomeStep(displayName = displayName)
                    1 -> ChromeTourStep()
                    2 -> ShieldSetupStep(
                        shieldConfig = shieldConfig,
                        onToggleAdBlock = onToggleAdBlock,
                        onToggleYouTubeShield = onToggleYouTubeShield,
                        onTogglePopupGuard = onTogglePopupGuard,
                        onToggleHttpsUpgrade = onToggleHttpsUpgrade
                    )
                    3 -> DefaultBrowserStep(
                        isDefaultBrowser = isDefaultBrowser,
                        onRequestDefaultBrowser = onRequestDefaultBrowser,
                        onOpenDefaultAppsSettings = onOpenDefaultAppsSettings
                    )
                    4 -> ReadyStep(isDefaultBrowser = isDefaultBrowser, shieldConfig = shieldConfig)
                }
            }

            // Bottom indicator + Next / Finish controls
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(5) { idx ->
                        val active = idx == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (active) 28.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (pagerState.currentPage > 0) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("tutorial_back_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Back")
                        }
                    }

                    val isLast = pagerState.currentPage == 4
                    Button(
                        onClick = {
                            if (isLast) {
                                onFinishTutorial()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(if (pagerState.currentPage > 0) 1.5f else 1f)
                            .height(52.dp)
                            .testTag(if (isLast) "tutorial_finish_button" else "tutorial_next_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(if (isLast) "Start Browsing" else "Continue")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isLast) Icons.Default.CheckCircle
                            else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(displayName: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_firedragon_hero_1795447990933),
                contentDescription = "FireDragon welcome banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp)
            ) {
                Text(
                    text = "Welcome, $displayName",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Let's configure FireDragon Browser in under 30 seconds.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        TutorialFeatureCard(
            icon = Icons.Default.Search,
            title = "Familiar Chrome-Style Interface",
            subtitle = "Unified Omnibox search & URL bar, Tab Grid Switcher, Incognito tabs, and Speed Dial."
        )
        TutorialFeatureCard(
            icon = Icons.Default.Shield,
            title = "Built-In AdBlock & YouTube Shield",
            subtitle = "Blocks intrusive banners, trackers, pop-ups, and cleans YouTube ad overlays automatically."
        )
        TutorialFeatureCard(
            icon = Icons.Default.Language,
            title = "System Default Browser Ready",
            subtitle = "Set FireDragon as your Android default browser so every external link opens protected."
        )
    }
}

@Composable
private fun ChromeTourStep() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Browser Controls Tour",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Everything is positioned right where Chrome users expect it, enhanced with FireDragon Shield.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Interactive Mock Omnibox Preview
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "TOP BAR PREVIEW",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = ShieldGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "m.youtube.com",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(
                                1.5.dp,
                                MaterialTheme.colorScheme.onSurface,
                                RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("3", style = MaterialTheme.typography.labelSmall)
                    }
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        TutorialFeatureCard(
            icon = Icons.Default.Shield,
            title = "1. FireDragon Shield Icon",
            subtitle = "Tap the shield on the left of the address bar to view live ads blocked or pause protection on a site."
        )
        TutorialFeatureCard(
            icon = Icons.Default.Tab,
            title = "2. Tab Counter Badge [N]",
            subtitle = "Tap the square tab number to open the Chrome-style Tab Grid, create Incognito tabs, or switch tabs."
        )
        TutorialFeatureCard(
            icon = Icons.Default.MoreVert,
            title = "3. Overflow Menu",
            subtitle = "Access Bookmarks, History, Find in Page, Desktop Site mode, and Account Settings."
        )
    }
}

@Composable
private fun ShieldSetupStep(
    shieldConfig: ShieldConfig,
    onToggleAdBlock: (Boolean) -> Unit,
    onToggleYouTubeShield: (Boolean) -> Unit,
    onTogglePopupGuard: (Boolean) -> Unit,
    onToggleHttpsUpgrade: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "AdBlock & YouTube Shield",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Image(
            painter = painterResource(id = R.drawable.img_shield_tutorial_1795447991767),
            contentDescription = "AdBlock and YouTube Shield illustration",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(155.dp)
                .clip(RoundedCornerShape(20.dp))
        )

        ShieldToggleRow(
            title = "Ad & Tracker Blocker",
            subtitle = "Blocks third-party ad networks, banners, and analytics trackers.",
            checked = shieldConfig.adBlockEnabled,
            onCheckedChange = onToggleAdBlock,
            tag = "tutorial_toggle_adblock"
        )
        ShieldToggleRow(
            title = "YouTube Clean Shield",
            subtitle = "Removes YouTube ad overlays, promo cards, and auto-skips ad notices.",
            checked = shieldConfig.youtubeShieldEnabled,
            onCheckedChange = onToggleYouTubeShield,
            tag = "tutorial_toggle_youtube"
        )
        ShieldToggleRow(
            title = "Popup & Redirect Guard",
            subtitle = "Stops aggressive pop-under windows and unwanted ad redirects.",
            checked = shieldConfig.popupGuardEnabled,
            onCheckedChange = onTogglePopupGuard,
            tag = "tutorial_toggle_popup"
        )
        ShieldToggleRow(
            title = "Automatic HTTPS Upgrade",
            subtitle = "Upgrades typed website addresses to encrypted HTTPS automatically.",
            checked = shieldConfig.httpsUpgradeEnabled,
            onCheckedChange = onToggleHttpsUpgrade,
            tag = "tutorial_toggle_https"
        )
    }
}

@Composable
private fun DefaultBrowserStep(
    isDefaultBrowser: Boolean,
    onRequestDefaultBrowser: () -> Unit,
    onOpenDefaultAppsSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Set Default Browser",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Make FireDragon your system default browser so links from messages, YouTube shares, and apps open with AdBlock already active.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDefaultBrowser) ShieldGreen.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (isDefaultBrowser) ShieldGreen else MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(22.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isDefaultBrowser) Icons.Default.CheckCircle else Icons.Default.Language,
                        contentDescription = null,
                        tint = if (isDefaultBrowser) ShieldGreen else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isDefaultBrowser) "FireDragon is Default Browser"
                            else "Default Browser Not Set Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isDefaultBrowser) "All web links open directly in FireDragon."
                            else "Tap below and choose FireDragon in the Android prompt.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!isDefaultBrowser) {
                    Button(
                        onClick = onRequestDefaultBrowser,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("tutorial_set_default_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Set FireDragon as Default")
                    }

                    OutlinedButton(
                        onClick = onOpenDefaultAppsSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("tutorial_open_default_settings"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Open Android Default Apps Settings")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadyStep(
    isDefaultBrowser: Boolean,
    shieldConfig: ShieldConfig
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "FireDragon is Ready!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Your personalized Chrome-style browser and shields are configured. Tap Start Browsing below to open your New Tab Page.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        TutorialFeatureCard(
            icon = Icons.Default.Shield,
            title = "AdBlock Engine",
            subtitle = if (shieldConfig.adBlockEnabled) "Enabled — Ads and trackers will be blocked"
            else "Disabled — You can enable it anytime from the Shield menu"
        )
        TutorialFeatureCard(
            icon = Icons.Default.PlayCircle,
            title = "YouTube Clean Shield",
            subtitle = if (shieldConfig.youtubeShieldEnabled) "Active — Ad overlays and promos removed on YouTube"
            else "Off — Toggle inside Shield settings"
        )
        TutorialFeatureCard(
            icon = Icons.Default.Security,
            title = "Default Browser Status",
            subtitle = if (isDefaultBrowser) "Active — FireDragon handles your web links"
            else "Optional — You can set FireDragon as default anytime in Settings"
        )
    }
}

@Composable
private fun TutorialFeatureCard(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ShieldToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(tag)
            )
        }
    }
}
