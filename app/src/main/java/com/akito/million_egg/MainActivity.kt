package com.akito.million_egg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akito.million_egg.ui.theme.Million_eggTheme

enum class Screen {
    Game,
    Titles,
    Clear
}

// スクリーンショット撮影等でバナー広告を非表示にする場合は false にする
private const val SHOW_BANNER_AD = false

class MainActivity : ComponentActivity() {
    private var interstitialCounter = 0
    private lateinit var interstitialAdHelper: InterstitialAdHelper
    private lateinit var rewardedAdHelper: RewardedAdHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        interstitialAdHelper = InterstitialAdHelper(this)
        interstitialAdHelper.loadAd()

        rewardedAdHelper = RewardedAdHelper(this)
        rewardedAdHelper.loadAd()

        setContent {
            Million_eggTheme {
                val viewModel: GameViewModel = viewModel()
                
                // リワード広告を表示する関数を定義
                val showRewardedAd = { 
                    rewardedAdHelper.showAd(this@MainActivity) {
                        viewModel.grantFreeUpgrade()
                    }
                }
                
                // 画面遷移の簡易管理
                var screenState by remember { mutableStateOf(Screen.Game) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (SHOW_BANNER_AD) {
                            BannerAdView()
                        }
                    }
                ) { innerPadding ->
                    when (screenState) {
                        Screen.Game -> {
                            GameScreen(
                                modifier = Modifier.padding(innerPadding),
                                viewModel = viewModel,
                                onNavigateToTitles = { screenState = Screen.Titles },
                                onShowRewardedAd = showRewardedAd
                            )
                        }
                        Screen.Titles -> {
                            BackHandler { 
                                interstitialCounter++
                                if (interstitialCounter % 2 == 0) {
                                    interstitialAdHelper.showAd(this@MainActivity) {
                                        screenState = Screen.Game
                                    }
                                } else {
                                    screenState = Screen.Game
                                }
                            }
                            TitlesScreen(
                                viewModel = viewModel,
                                onBack = { 
                                    interstitialCounter++
                                    if (interstitialCounter % 2 == 0) {
                                        interstitialAdHelper.showAd(this@MainActivity) {
                                            screenState = Screen.Game
                                        }
                                    } else {
                                        screenState = Screen.Game
                                    }
                                }
                            )
                        }
                        Screen.Clear -> {
                            // 背景としてGameを表示したままにするため、ここでもGameを描画
                            GameScreen(
                                modifier = Modifier.padding(innerPadding),
                                viewModel = viewModel,
                                onNavigateToTitles = { screenState = Screen.Titles },
                                onShowRewardedAd = showRewardedAd
                            )
                        }
                    }

                    // クリア画面を最前面にオーバーレイとして表示
                    if (screenState == Screen.Clear) {
                        ClearScreen(
                            viewModel = viewModel,
                            onRestart = { screenState = Screen.Game }
                        )
                    }
                }

                // クリア状態を監視して自動遷移
                val progress by viewModel.progress.collectAsState()
                LaunchedEffect(progress.isCleared) {
                    if (progress.isCleared && screenState != Screen.Clear) {
                        screenState = Screen.Clear
                    }
                }
            }
        }
    }
}
