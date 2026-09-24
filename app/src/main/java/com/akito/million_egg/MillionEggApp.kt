package com.akito.million_egg

import android.app.Application
import android.content.Context
import androidx.datastore.dataStore

import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val Context.playerStateStore by dataStore(
    fileName = "player_state.json",
    serializer = PlayerStateSerializer
)

val Context.gameProgressStore by dataStore(
    fileName = "game_progress.json",
    serializer = GameProgressSerializer
)

class MillionEggApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Mobile Ads SDK の初期化
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@MillionEggApp) {}
        }
    }
}
