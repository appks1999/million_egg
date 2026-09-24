package com.akito.million_egg

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdConstants {
    const val BANNER_ID = "ca-app-pub-8950375321788767/4040335204"
    const val INTERSTITIAL_ID = "ca-app-pub-8950375321788767/1887805420"
    const val REWARDED_ID = "ca-app-pub-8950375321788767/4356774833"
}

/**
 * バナー広告を表示するコンポーザブル
 */
@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdConstants.BANNER_ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

/**
 * インタースティシャル広告の管理
 */
class InterstitialAdHelper(private val context: Context) {
    private var interstitialAd: InterstitialAd? = null
    private var isAdLoading = false

    fun loadAd() {
        if (interstitialAd != null || isAdLoading) return

        isAdLoading = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            AdConstants.INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                    isAdLoading = false
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isAdLoading = false
                }
            }
        )
    }

    fun showAd(activity: Activity, onAdClosed: () -> Unit) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadAd() // 次のためにリロード
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    interstitialAd = null
                    onAdClosed()
                }
            }
            interstitialAd?.show(activity)
        } else {
            onAdClosed()
        }
    }
}

/**
 * リワード広告の管理
 */
class RewardedAdHelper(private val context: Context) {
    private var rewardedAd: com.google.android.gms.ads.rewarded.RewardedAd? = null
    private var isAdLoading = false

    fun loadAd() {
        if (rewardedAd != null || isAdLoading) return

        isAdLoading = true
        val adRequest = AdRequest.Builder().build()
        com.google.android.gms.ads.rewarded.RewardedAd.load(
            context,
            AdConstants.REWARDED_ID,
            adRequest,
            object : com.google.android.gms.ads.rewarded.RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                    isAdLoading = false
                }

                override fun onAdLoaded(ad: com.google.android.gms.ads.rewarded.RewardedAd) {
                    rewardedAd = ad
                    isAdLoading = false
                }
            }
        )
    }

    fun showAd(activity: Activity, onUserEarnedReward: () -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadAd() // 次のためにリロード
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    rewardedAd = null
                }
            }
            rewardedAd?.show(activity) {
                onUserEarnedReward()
            }
        }
    }
}
