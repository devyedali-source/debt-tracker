package com.elyedali.debttracker.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import com.elyedali.debttracker.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AdMob ads for the free plan only. PRO users never see ads.
 * Ad unit IDs come from app/build.gradle.kts (test IDs until the real ones are set).
 */
object AdsManager {

    private val started = AtomicBoolean(false)
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    private var interstitial: InterstitialAd? = null
    private var loadingInterstitial = false
    private var savesSinceAd = 0
    private var lastShownAt = 0L

    private const val SAVES_BETWEEN_ADS = 4
    private const val MIN_GAP_MS = 2 * 60 * 1000L

    /** Asks for consent where the law requires it (Europe, UK...), then starts ads. */
    fun gatherConsentAndStart(activity: Activity) {
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()
        consent.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    if (consent.canRequestAds()) start(activity)
                }
            },
            { if (consent.canRequestAds()) start(activity) },
        )
        if (consent.canRequestAds()) start(activity)
    }

    fun privacyOptionsRequired(context: Context): Boolean =
        UserMessagingPlatform.getConsentInformation(context).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { }
    }

    private fun start(context: Context) {
        if (!started.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(appContext) {
                _ready.value = true
                CoroutineScope(Dispatchers.Main).launch { loadInterstitial(appContext) }
            }
        }
    }

    private fun loadInterstitial(context: Context) {
        if (interstitial != null || loadingInterstitial || !_ready.value) return
        loadingInterstitial = true
        InterstitialAd.load(
            context,
            BuildConfig.AD_UNIT_INTERSTITIAL,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loadingInterstitial = false
                    interstitial = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingInterstitial = false
                    interstitial = null
                }
            },
        )
    }

    /** Called after the user saves an entry. Shows a full-screen ad now and then, never too often. */
    fun onEntrySaved(activity: Activity, premium: Boolean) {
        if (premium || !_ready.value) return
        savesSinceAd++
        val now = SystemClock.elapsedRealtime()
        val ad = interstitial
        if (ad == null) {
            loadInterstitial(activity)
            return
        }
        if (savesSinceAd >= SAVES_BETWEEN_ADS && (lastShownAt == 0L || now - lastShownAt > MIN_GAP_MS)) {
            interstitial = null
            savesSinceAd = 0
            lastShownAt = now
            ad.show(activity)
            loadInterstitial(activity)
        }
    }
}

/** Banner at the bottom of a screen, only for the free plan. */
@Composable
fun AdBanner(premium: Boolean, modifier: Modifier = Modifier) {
    val ready by AdsManager.ready.collectAsState()
    if (premium || !ready) return
    val widthDp = LocalConfiguration.current.screenWidthDp
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                adUnitId = BuildConfig.AD_UNIT_BANNER
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, widthDp))
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { it.destroy() },
    )
}
