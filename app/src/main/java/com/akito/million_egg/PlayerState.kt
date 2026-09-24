package com.akito.million_egg

import kotlinx.serialization.Serializable

/**
 * プレイヤーのRPG的な強さ（ステータス）を保持するクラス
 */
@Serializable
data class PlayerState(
    val gold: Long = 0,
    val powerLevel: Int = 1,
    val criticalLevel: Int = 0,
    val autoTapLevel: Int = 0,
    val currentTitle: Title? = null,
    val hasFreeUpgrade: Boolean = false // 無料アップグレード権
) {
    // 実際のステータス値を計算するプロパティ
    val tapPower: Int get() = powerLevel
    val criticalRate: Double get() = 0.01 + (criticalLevel * 0.01) // 初期 1%
    val autoTapInterval: Int get() = if (autoTapLevel == 0) 0 else (61 - autoTapLevel).coerceAtLeast(1)
}

/**
 * 称号の定義
 */
@Serializable
data class Title(
    val name: String,
    val requirement: Long,
    val powerBonus: Int = 0,
    val criticalBonus: Double = 0.0
)

val Titles = listOf(
    Title("タップ初心者", 5000L, powerBonus = 1),
    Title("タップ熟練者", 10000L, powerBonus = 1, criticalBonus = 0.01),
    Title("タップ愛好家", 50000L, powerBonus = 2, criticalBonus = 0.01),
    Title("タップの達人", 100000L, powerBonus = 2, criticalBonus = 0.02),
    Title("タップの巨匠", 300000L, powerBonus = 4, criticalBonus = 0.02),
    Title("タップの王者", 500000L, powerBonus = 4, criticalBonus = 0.04),
    Title("タップの覇者", 700000L, powerBonus = 8, criticalBonus = 0.04),
    Title("タップの神", 1000000L, powerBonus = 8, criticalBonus = 0.08)
)
