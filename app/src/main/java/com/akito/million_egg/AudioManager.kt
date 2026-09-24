package com.akito.million_egg

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class AudioManager(context: Context) {
    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<String, Int>()

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        // 音声ファイルのロード
        soundMap["tap"] = soundPool.load(context, R.raw.tap, 1)
        soundMap["critical"] = soundPool.load(context, R.raw.critical, 1)
        soundMap["fever"] = soundPool.load(context, R.raw.fever, 1)
        soundMap["coin"] = soundPool.load(context, R.raw.coin, 1)
        soundMap["status_up"] = soundPool.load(context, R.raw.status_up, 1)
        soundMap["game_clear"] = soundPool.load(context, R.raw.game_clear, 1)
    }

    fun playSound(name: String) {
        val soundId = soundMap[name] ?: return
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}
