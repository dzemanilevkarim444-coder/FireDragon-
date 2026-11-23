package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.browser.AdBlockEngine
import com.example.browser.BlockCategory
import com.example.browser.UrlUtils
import com.example.data.SearchEngine
import com.example.data.ShieldConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches FireDragon`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FireDragon", appName)
    }

    @Test
    fun `omnibox resolves domains and search queries accurately`() {
        assertEquals(
            "https://m.youtube.com",
            UrlUtils.resolveOmniboxInput("m.youtube.com", SearchEngine.GOOGLE, httpsUpgrade = true)
        )
        assertEquals(
            "https://example.org/news",
            UrlUtils.resolveOmniboxInput("http://example.org/news", SearchEngine.GOOGLE, httpsUpgrade = true)
        )
        assertEquals(
            "https://duckduckgo.com/?q=firedragon+browser",
            UrlUtils.resolveOmniboxInput("firedragon browser", SearchEngine.DUCKDUCKGO, httpsUpgrade = true)
        )
    }

    @Test
    fun `adblock engine blocks ads trackers and youtube ad paths`() {
        val config = ShieldConfig(adBlockEnabled = true, youtubeShieldEnabled = true)

        val doubleClick = AdBlockEngine.evaluateRequest(
            requestUrl = "https://pubads.g.doubleclick.net/gampad/ads",
            pageHost = "news.example.com",
            shieldConfig = config,
            whitelistedDomains = emptySet()
        )
        assertTrue(doubleClick.blocked)
        assertEquals(BlockCategory.AD, doubleClick.category)

        val ytAd = AdBlockEngine.evaluateRequest(
            requestUrl = "https://m.youtube.com/api/stats/ads?ad_break=1",
            pageHost = "m.youtube.com",
            shieldConfig = config,
            whitelistedDomains = emptySet()
        )
        assertTrue(ytAd.blocked)
        assertEquals(BlockCategory.YOUTUBE_AD, ytAd.category)

        val normalContent = AdBlockEngine.evaluateRequest(
            requestUrl = "https://m.youtube.com/watch?v=dQw4w9WgXcQ",
            pageHost = "m.youtube.com",
            shieldConfig = config,
            whitelistedDomains = emptySet()
        )
        assertFalse(normalContent.blocked)
    }

    @Test
    fun `whitelisted site bypasses blocking and cosmetic scripts`() {
        val config = ShieldConfig(adBlockEnabled = true, youtubeShieldEnabled = true)
        val whitelisted = setOf("youtube.com")

        val decision = AdBlockEngine.evaluateRequest(
            requestUrl = "https://ads.youtube.com/banner",
            pageHost = "m.youtube.com",
            shieldConfig = config,
            whitelistedDomains = emptySet()
        )
        assertTrue(decision.blocked)

        val bypassed = AdBlockEngine.evaluateRequest(
            requestUrl = "https://pubads.g.doubleclick.net/gampad/ads",
            pageHost = "www.youtube.com",
            shieldConfig = config,
            whitelistedDomains = whitelisted
        )
        assertFalse(bypassed.blocked)

        assertNull(AdBlockEngine.buildCosmeticScript("www.youtube.com", config, whitelisted))
        assertNotNull(AdBlockEngine.buildCosmeticScript("m.youtube.com", config, emptySet()))
    }
}
