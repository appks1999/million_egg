package com.akito.million_egg

import android.app.Application
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlin.math.pow
import kotlin.random.Random

data class GoldEarnedEvent(val amount: Long, val offset: Offset)
data class CriticalHitEvent(val offset: Offset)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        const val MAX_POWER_LEVEL = 100
        const val MAX_AUTO_TAP_LEVEL = 60
        const val BASE_UPGRADE_COST = 100L // 10Lから100Lへ10倍に変更
    }

    private val progressStore = application.gameProgressStore
    private val playerStateStore = application.playerStateStore
    private val audioManager = AudioManager(application)

    private val _progress = MutableStateFlow(GameProgress())
    val progress: StateFlow<GameProgress> = _progress.asStateFlow()

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _autoTapCountdown = MutableStateFlow(0)
    val autoTapCountdown: StateFlow<Int> = _autoTapCountdown.asStateFlow()

    private val _feverCountdown = MutableStateFlow(0)
    val feverCountdown: StateFlow<Int> = _feverCountdown.asStateFlow()

    // クリティカルヒットイベント (座標付き)
    private val _criticalHitEvent = MutableSharedFlow<CriticalHitEvent>()
    val criticalHitEvent: SharedFlow<CriticalHitEvent> = _criticalHitEvent.asSharedFlow()

    // ゴールド獲得イベント (座標付き)
    private val _goldEarnedEvent = MutableSharedFlow<GoldEarnedEvent>()
    val goldEarnedEvent: SharedFlow<GoldEarnedEvent> = _goldEarnedEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            playerStateStore.data.collect { _playerState.value = it }
        }
        viewModelScope.launch {
            progressStore.data.collect { _progress.value = it }
        }
        
        startAutoTapLoop()
    }

    /**
     * 現在達成している最上位の称号によるボーナスを計算する（非加算方式）
     */
    private fun calculateTitleBonuses(): Pair<Int, Double> {
        val totalDamage = _progress.value.totalDamage
        val highestTitle = Titles.filter { it.requirement <= totalDamage }.maxByOrNull { it.requirement }
        
        val powerBonus = highestTitle?.powerBonus ?: 0
        val critBonus = highestTitle?.criticalBonus ?: 0.0
        
        return Pair(powerBonus, critBonus)
    }

    fun onTap(offset: Offset = Offset.Zero) {
        val currentState = _playerState.value
        val (powerBonus, critBonus) = calculateTitleBonuses()
        
        var damage = (currentState.tapPower + powerBonus).toLong()

        // 称号ボーナス込みのクリティカル率（最大 1.0）
        val totalCritRate = (currentState.criticalRate + critBonus).coerceAtMost(1.0)
        val isCritical = Random.nextDouble() < totalCritRate
        
        var earnedGold = 0L
        val isFever = _feverCountdown.value > 0

        // 音声再生とゴールド計算
        if (isFever) {
            audioManager.playSound("fever")
            earnedGold = Random.nextLong(10, 31)
        } else if (isCritical) {
            audioManager.playSound("critical")
            val multiplier = Random.nextDouble(1.5, 2.0)
            val baseDamage = damage
            damage = kotlin.math.round(damage * multiplier).toLong().coerceAtLeast(damage + 1)
            Log.d("GameViewModel", "Critical Hit! Multiplier: %.2fx (Base: %d -> Final: %d)".format(multiplier, baseDamage, damage))
            viewModelScope.launch {
                _criticalHitEvent.emit(CriticalHitEvent(offset))
            }
            // クリティカル時はゴールド獲得判定があっても演出をOFF
            if (Random.nextDouble() < 0.01) { // テスト用100%
                earnedGold = Random.nextLong(1, 11)
            }
        } else {
            audioManager.playSound("tap")
            // 金の卵確率
            if (Random.nextDouble() < 0.02) { // テスト用100%
                earnedGold = Random.nextLong(1, 21)
                audioManager.playSound("coin")
            }
            // フィーバー突入判定
            if (Random.nextDouble() < 0.00025) {
                startFever()
            }
        }

        // ゴールド加算とUI通知
        if (earnedGold > 0) {
            viewModelScope.launch {
                playerStateStore.updateData { it.copy(gold = it.gold + earnedGold) }
                // クリティカルでない場合のみ、ゴールドのポップアップを表示
                if (!isCritical) {
                    _goldEarnedEvent.emit(GoldEarnedEvent(earnedGold, offset))
                }
            }
        }

        updateProgress(damage, isManualTap = true)
    }

    private fun updateProgress(damage: Long, isManualTap: Boolean) {
        viewModelScope.launch {
            progressStore.updateData { 
                // すでに0以下の場合は何もしない
                if (it.remainingTaps <= 0L) return@updateData it

                val isFeverActive = _feverCountdown.value > 0
                val minRemaining = if (isManualTap) 0L else 1L
                
                val newRemaining = if (isFeverActive) it.remainingTaps else (it.remainingTaps - damage).coerceAtLeast(minRemaining)
                
                it.copy(
                    remainingTaps = newRemaining,
                    totalDamage = it.totalDamage + damage,
                    tapCount = if (isManualTap) it.tapCount + 1 else it.tapCount,
                    isCleared = newRemaining <= 0L
                )
            }
            checkAchievements()
        }
    }

    private fun startFever() {
        viewModelScope.launch {
            _feverCountdown.value = 10
            while (_feverCountdown.value > 0) {
                delay(1000)
                _feverCountdown.value -= 1
            }
        }
    }

    private fun checkAchievements() {
        val total = _progress.value.totalDamage
        val unlockedTitle = Titles.filter { it.requirement <= total }.maxByOrNull { it.requirement }
        
        if (unlockedTitle != _playerState.value.currentTitle) {
            viewModelScope.launch {
                playerStateStore.updateData { it.copy(currentTitle = unlockedTitle) }
            }
        }
    }

    fun playSound(name: String) {
        audioManager.playSound(name)
    }

    fun playGameClearSound() {
        audioManager.playSound("game_clear")
    }

    fun resetGame() {
        viewModelScope.launch {
            playerStateStore.updateData { PlayerState() }
            progressStore.updateData { GameProgress() }
        }
    }

    fun markEndingAsSeen() {
        viewModelScope.launch {
            progressStore.updateData { it.copy(hasSeenEnding = true) }
        }
    }

    /**
     * 強くてニューゲーム：ステータスや累計ダメージを引き継ぎ、残りタップ数を100万回にリセットし、開始日時を更新する
     */
    fun startNewGamePlus() {
        viewModelScope.launch {
            progressStore.updateData { 
                it.copy(
                    remainingTaps = 1_000_000L,
                    isCleared = false,
                    startDateMillis = System.currentTimeMillis(),
                    hasSeenEnding = false
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }

    /**
     * デバッグ用：ゴールドを加算する
     */
    fun debugAddGold(amount: Long) {
        viewModelScope.launch {
            playerStateStore.updateData { it.copy(gold = it.gold + amount) }
        }
    }

    /**
     * デバッグ用：ダメージを加算する（残りタップ数を減らす）
     */
    fun debugAddDamage(amount: Long) {
        updateProgress(amount, isManualTap = false)
    }

    /**
     * デバッグ用：強制的にフィーバーを開始する
     */
    fun debugTriggerFever() {
        startFever()
    }

    /**
     * デバッグ用：強制的にクリティカルヒットを発生させる
     */
    fun debugTriggerCritical() {
        viewModelScope.launch {
            // デバッグ時は中央付近をダミー座標として渡す
            val dummyOffset = Offset(540f, 1000f)
            _criticalHitEvent.emit(CriticalHitEvent(dummyOffset))
            // ダメージも与える (1.5倍〜2.0倍のランダム倍率)
            val currentState = _playerState.value
            val (powerBonus, _) = calculateTitleBonuses()
            val baseDamage = (currentState.tapPower + powerBonus).toDouble()
            val multiplier = Random.nextDouble(1.5, 2.0)
            val critDamage = kotlin.math.round(baseDamage * multiplier).toLong().coerceAtLeast(1L)
            Log.d("GameViewModel", "[Debug] Critical Hit! Multiplier: %.2fx (Base: %.0f -> Final: %d)".format(multiplier, baseDamage, critDamage))
            updateProgress(critDamage, isManualTap = true)
        }
    }

    // --- アップグレード処理 (ゴールド消費) ---

    fun getUpgradeCost(level: Int): Long {
        if (level == 0) return BASE_UPGRADE_COST
        return (BASE_UPGRADE_COST * 1.15.pow(level.toDouble())).toLong()
    }

    fun upgradePower() {
        val currentState = _playerState.value
        val cost = if (currentState.hasFreeUpgrade) 0L else getUpgradeCost(currentState.powerLevel)
        if ((currentState.gold >= cost || currentState.hasFreeUpgrade) && currentState.powerLevel < MAX_POWER_LEVEL) {
            viewModelScope.launch {
                playerStateStore.updateData { 
                    it.copy(
                        gold = if (it.hasFreeUpgrade) it.gold else it.gold - cost,
                        powerLevel = it.powerLevel + 1,
                        hasFreeUpgrade = false // 使用したらフラグを戻す
                    )
                }
                audioManager.playSound("status_up")
            }
        }
    }

    fun upgradeCritical() {
        val currentState = _playerState.value
        val ( _, titleCritBonus) = calculateTitleBonuses()
        // 称号ボーナス込みで 1.0 (100%) になったらMAX
        val currentTotalCrit = currentState.criticalRate + titleCritBonus
        
        val cost = if (currentState.hasFreeUpgrade) 0L else getUpgradeCost(currentState.criticalLevel)
        if ((currentState.gold >= cost || currentState.hasFreeUpgrade) && currentTotalCrit < 1.0) {
            viewModelScope.launch {
                playerStateStore.updateData { 
                    it.copy(
                        gold = if (it.hasFreeUpgrade) it.gold else it.gold - cost,
                        criticalLevel = it.criticalLevel + 1,
                        hasFreeUpgrade = false // 使用したらフラグを戻す
                    )
                }
                audioManager.playSound("status_up")
            }
        }
    }

    fun upgradeAutoTap() {
        val currentState = _playerState.value
        val cost = if (currentState.hasFreeUpgrade) 0L else getUpgradeCost(currentState.autoTapLevel)
        if ((currentState.gold >= cost || currentState.hasFreeUpgrade) && currentState.autoTapLevel < MAX_AUTO_TAP_LEVEL) {
            viewModelScope.launch {
                playerStateStore.updateData { 
                    it.copy(
                        gold = if (it.hasFreeUpgrade) it.gold else it.gold - cost,
                        autoTapLevel = it.autoTapLevel + 1,
                        hasFreeUpgrade = false // 使用したらフラグを戻す
                    )
                }
                audioManager.playSound("status_up")
            }
        }
    }

    /**
     * リワード報酬：無料アップグレード権を付与
     */
    fun grantFreeUpgrade() {
        viewModelScope.launch {
            playerStateStore.updateData { it.copy(hasFreeUpgrade = true) }
        }
    }

    private fun startAutoTapLoop() {
        viewModelScope.launch {
            while (isActive) {
                val interval = _playerState.value.autoTapInterval
                if (interval > 0) {
                    _autoTapCountdown.value = interval
                    while (_autoTapCountdown.value > 0) {
                        delay(1000)
                        val currentInterval = _playerState.value.autoTapInterval
                        if (currentInterval < interval && currentInterval > 0) break 
                        _autoTapCountdown.value -= 1
                    }
                    
                    if (_autoTapCountdown.value <= 0) {
                        val currentState = _playerState.value
                        val (powerBonus, _) = calculateTitleBonuses()
                        val damage = (currentState.tapPower + powerBonus).toLong()
                        updateProgress(damage, isManualTap = false)
                    }
                } else {
                    delay(1000)
                }
            }
        }
    }
}
