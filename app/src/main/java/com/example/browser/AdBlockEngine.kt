package com.example.browser

import android.net.Uri
import android.webkit.WebResourceResponse
import com.example.data.ShieldConfig
import java.io.ByteArrayInputStream

enum class BlockCategory {
    NONE,
    AD,
    TRACKER,
    YOUTUBE_AD
}

data class BlockDecision(
    val blocked: Boolean,
    val category: BlockCategory = BlockCategory.NONE
)

object AdBlockEngine {

    private val adDomains = hashSetOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "tpc.googlesyndication.com",
        "ads.youtube.com",
        "taboola.com",
        "outbrain.com",
        "criteo.com",
        "criteo.net",
        "pubmatic.com",
        "rubiconproject.com",
        "openx.net",
        "adnxs.com",
        "appnexus.com",
        "moatads.com",
        "adsafeprotected.com",
        "amazon-adsystem.com",
        "media.net",
        "revcontent.com",
        "mgid.com",
        "propellerads.com",
        "popads.net",
        "popcash.net",
        "adsterra.com",
        "bidswitch.net",
        "casalemedia.com",
        "smartadserver.com",
        "yieldmo.com",
        "33across.com",
        "teads.tv",
        "zemanta.com",
        "sharethrough.com",
        "adform.net",
        "adsrvr.org",
        "advertising.com",
        "spotxchange.com",
        "liadm.com"
    )

    private val trackerDomains = hashSetOf(
        "scorecardresearch.com",
        "quantserve.com",
        "hotjar.com",
        "mixpanel.com",
        "amplitude.com",
        "segment.io",
        "segment.com",
        "mc.yandex.ru",
        "pixel.facebook.com",
        "an.facebook.com",
        "bat.bing.com",
        "ct.pinterest.com",
        "analytics.twitter.com",
        "ads-twitter.com",
        "snap.licdn.com",
        "px.ads.linkedin.com",
        "chartbeat.com",
        "omtrdc.net",
        "demdex.net",
        "krxd.net",
        "bluekai.com",
        "exelator.com",
        "branch.io",
        "appsflyer.com",
        "adjust.com",
        "clarity.ms",
        "fullstory.com",
        "newrelic.com",
        "nr-data.net",
        "sentry.io",
        "bugsnag.com"
    )

    private val youtubeAdPathHints = listOf(
        "/pagead/",
        "/api/stats/ads",
        "/api/stats/atr",
        "/ptracking",
        "/get_midroll_info",
        "/youtubei/v1/log_event",
        "ad_break",
        "adformat=",
        "ad_type=",
        "oad="
    )

    private val generalAdPathHints = listOf(
        "/pagead/",
        "/adserver",
        "/adservice",
        "/banner_ad",
        "/popunder",
        "fbevents.js",
        "gpt.js",
        "pubads_impl",
        "prebid.js",
        "show_ads.js"
    )

    fun evaluateRequest(
        requestUrl: String,
        pageHost: String,
        shieldConfig: ShieldConfig,
        whitelistedDomains: Set<String>
    ): BlockDecision {
        val cleanPageHost = pageHost.lowercase().removePrefix("www.")
        if (cleanPageHost.isNotBlank() && whitelistedDomains.contains(cleanPageHost)) {
            return BlockDecision(blocked = false)
        }

        val uri = try {
            Uri.parse(requestUrl)
        } catch (_: Exception) {
            return BlockDecision(blocked = false)
        }

        val reqHost = uri.host?.lowercase() ?: return BlockDecision(blocked = false)
        val lowerUrl = requestUrl.lowercase()

        // YouTube ad network & telemetry interception
        if (shieldConfig.youtubeShieldEnabled) {
            if (reqHost == "ads.youtube.com" || reqHost.endsWith(".ads.youtube.com")) {
                return BlockDecision(blocked = true, category = BlockCategory.YOUTUBE_AD)
            }
            if ((cleanPageHost.contains("youtube.com") || reqHost.contains("youtube.com") || reqHost.contains("googlevideo.com")) &&
                youtubeAdPathHints.any { lowerUrl.contains(it) }
            ) {
                return BlockDecision(blocked = true, category = BlockCategory.YOUTUBE_AD)
            }
            // Google videoplayback with explicit ad parameter (&ctier=L or &oad=)
            if (reqHost.endsWith("googlevideo.com") &&
                (lowerUrl.contains("&oad=") || lowerUrl.contains("?oad="))
            ) {
                return BlockDecision(blocked = true, category = BlockCategory.YOUTUBE_AD)
            }
        }

        if (!shieldConfig.adBlockEnabled) {
            return BlockDecision(blocked = false)
        }

        if (matchesDomainSet(reqHost, adDomains)) {
            return BlockDecision(blocked = true, category = BlockCategory.AD)
        }

        if (matchesDomainSet(reqHost, trackerDomains)) {
            return BlockDecision(blocked = true, category = BlockCategory.TRACKER)
        }

        // Block third-party known ad scripts/paths
        if (reqHost != cleanPageHost && !reqHost.endsWith(".$cleanPageHost")) {
            if (generalAdPathHints.any { lowerUrl.contains(it) }) {
                return BlockDecision(blocked = true, category = BlockCategory.AD)
            }
        }

        return BlockDecision(blocked = false)
    }

    private fun matchesDomainSet(host: String, domainSet: Set<String>): Boolean {
        if (domainSet.contains(host)) return true
        var idx = host.indexOf('.')
        while (idx != -1 && idx + 1 < host.length) {
            val parent = host.substring(idx + 1)
            if (domainSet.contains(parent)) return true
            idx = host.indexOf('.', idx + 1)
        }
        return false
    }

    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "utf-8",
            200,
            "OK",
            mapOf("Access-Control-Allow-Origin" to "*"),
            ByteArrayInputStream(ByteArray(0))
        )
    }

    fun buildCosmeticScript(
        pageHost: String,
        shieldConfig: ShieldConfig,
        whitelistedDomains: Set<String>
    ): String? {
        val cleanHost = pageHost.lowercase().removePrefix("www.")
        if (cleanHost.isNotBlank() && whitelistedDomains.contains(cleanHost)) return null
        if (!shieldConfig.adBlockEnabled && !shieldConfig.youtubeShieldEnabled) return null

        val isYouTube = cleanHost == "youtube.com" || cleanHost.endsWith(".youtube.com")
        if (isYouTube && shieldConfig.youtubeShieldEnabled) {
            return """
                (function() {
                    if (window.__fireDragonYtShield) return;
                    window.__fireDragonYtShield = true;
                    const styleId = 'firedragon-yt-shield-style';
                    if (!document.getElementById(styleId)) {
                        const style = document.createElement('style');
                        style.id = styleId;
                        style.textContent = `
                            ytd-ad-slot-renderer,
                            ytd-banner-promo-renderer,
                            ytd-statement-banner-renderer,
                            ytd-in-feed-ad-layout-renderer,
                            ytd-display-ad-renderer,
                            ytm-promoted-sparkles-web-renderer,
                            ytm-promoted-video-renderer,
                            ytm-companion-slot,
                            ytd-companion-slot-renderer,
                            ytd-enforcement-message-view-model,
                            tp-yt-paper-dialog:has(ytd-enforcement-message-view-model),
                            .ytp-ad-module,
                            .ytp-ad-overlay-container,
                            .ytp-ad-image-overlay,
                            .ytp-ad-text-overlay,
                            .ad-showing .video-ads,
                            #masthead-ad,
                            #player-ads,
                            #mealbar-promo-renderer,
                            ytm-mealbar-promo-renderer {
                                display: none !important;
                                visibility: hidden !important;
                                height: 0 !important;
                                opacity: 0 !important;
                                pointer-events: none !important;
                            }
                        `;
                        (document.head || document.documentElement).appendChild(style);
                    }
                    const cleanYouTube = function() {
                        const skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button');
                        if (skipBtn) { skipBtn.click(); }
                        const video = document.querySelector('video');
                        const adShowing = document.querySelector('.ad-showing');
                        if (video && adShowing && isFinite(video.duration) && video.duration > 0) {
                            video.muted = true;
                            video.currentTime = video.duration;
                        }
                        const dismissBtns = document.querySelectorAll('ytd-enforcement-message-view-model #dismiss-button, ytm-mealbar-promo-renderer button[aria-label*="No thanks"], ytm-mealbar-promo-renderer button[aria-label*="Dismiss"]');
                        dismissBtns.forEach(b => b.click());
                    };
                    cleanYouTube();
                    const observer = new MutationObserver(cleanYouTube);
                    if (document.body) {
                        observer.observe(document.body, { childList: true, subtree: true });
                    }
                })();
            """.trimIndent()
        }

        if (shieldConfig.cosmeticFilterEnabled && shieldConfig.adBlockEnabled) {
            return """
                (function() {
                    if (window.__fireDragonCosmetic) return;
                    window.__fireDragonCosmetic = true;
                    const styleId = 'firedragon-cosmetic-style';
                    if (!document.getElementById(styleId)) {
                        const style = document.createElement('style');
                        style.id = styleId;
                        style.textContent = `
                            ins.adsbygoogle,
                            [id^="google_ads_"],
                            [id^="div-gpt-ad"],
                            [data-ad-slot],
                            .taboola-widget,
                            #taboola-below-article-thumbnails,
                            .OUTBRAIN,
                            .ad-banner,
                            .ad-container,
                            .sponsored-ad {
                                display: none !important;
                                height: 0 !important;
                            }
                        `;
                        (document.head || document.documentElement).appendChild(style);
                    }
                })();
            """.trimIndent()
        }
        return null
    }
}
