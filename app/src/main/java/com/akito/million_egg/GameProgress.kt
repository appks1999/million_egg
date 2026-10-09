package com.akito.million_egg

import kotlinx.serialization.Serializable

/**
 * ゲームの進行状況（タップ数やクリア状況）を保持するクラス
 */
@Serializable
data class GameProgress(
    val remainingTaps: Long = 1_000_000L,
    val totalDamage: Long = 0L,
    val tapCount: Long = 0L,
    val isCleared: Boolean = false,
    val startDateMillis: Long = System.currentTimeMillis(),
    val hasSeenEnding: Boolean = false
)
