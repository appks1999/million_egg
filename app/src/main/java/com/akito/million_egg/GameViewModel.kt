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
import kotlinx.coroutines.flow.first
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
        const val BASE_UPGRADE_COST = 100L
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

    private val _criticalHitEvent = MutableSharedFlow<CriticalHitEvent>()
    val criticalHitEvent: SharedFlow<CriticalHitEvent> = _criticalHitEvent.asSharedFlow()

    private val _goldEarnedEvent = MutableSharedFlow<GoldEarnedEvent>()
    val goldEarnedEvent: SharedFlow<GoldEarnedEvent> = _goldEarnedEvent.asSharedFlow()

    private var isPlayerStateInitialized = false
    private var isProgressInitialized = false

    init {
        viewModelScope.launch {
            playerStateStore.data.collect { savedState ->
                if (!isPlayerStateInitialized) {
                    _playerState.value = savedState
                    isPlayerStateInitialized = true
                }
            }
        }
        viewModelScope.launch {
            progressStore.data.collect { savedProgress ->
                if (!isProgressInitialized) {
                    _progress.value = savedProgress
                    isProgressInitialized = true
                }
            }
        }
        
        startAutoTapLoop()
    }

    private fun calculateTitleBonuses(): Pair<Int, Double> {
        val currentTitle = _playerState.value.currentTitle
        val powerBonus = currentTitle?.powerBonus ?: 0
        val critBonus = currentTitle?.criticalBonus ?: 0.0
        return Pair(powerBonus, critBonus)
    }

    fun onTap(offset: Offset = Offset.Zero) {
        val currentState = _playerState.value
        val (powerBonus, critBonus) = calculateTitleBonuses()
        
        var damage = (currentState.tapPower + powerBonus).toLong()

        val totalCritRate = (currentState.criticalRate + critBonus).coerceAtMost(1.0)
        val isCritical = Random.nextDouble() < totalCritRate
        
        var earnedGold = 0L
        val isFever = _feverCountdown.value > 0

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
            if (Random.nextDouble() < 0.01) {
                earnedGold = Random.nextLong(1, 11)
            }
        } else {
            audioManager.playSound("tap")
            if (Random.nextDouble() < 0.02) {
                earnedGold = Random.nextLong(1, 21)
                audioManager.playSound("coin")
            }
            if (Random.nextDouble() < 0.00025) {
                startFever()
            }
        }

        if (earnedGold > 0) {
            val newState = _playerState.value.copy(gold = _playerState.value.gold + earnedGold)
            _playerState.value = newState
            viewModelScope.launch {
                playerStateStore.updateData { newState }
                if (!isCritical) {
                    _goldEarnedEvent.emit(GoldEarnedEvent(earnedGold, offset))
                }
            }
        }

        updateProgress(damage, isManualTap = true)
    }

    private fun updateProgress(damage: Long, isManualTap: Boolean) {
        val current = _progress.value
        if (current.remainingTaps <= 0L) return

        isProgressInitialized = true
        val isFeverActive = _feverCountdown.value > 0
        val minRemaining = if (isManualTap) 0L else 1L
        
        val newRemaining = if (isFeverActive) current.remainingTaps else (current.remainingTaps - damage).coerceAtLeast(minRemaining)
        val newTotal = current.totalDamage + damage
        
        val newProgress = current.copy(
            remainingTaps = newRemaining,
            totalDamage = newTotal,
            tapCount = if (isManualTap) current.tapCount + 1 else current.tapCount,
            isCleared = newRemaining <= 0L
        )
        _progress.value = newProgress
        checkAchievements(newTotal)

        viewModelScope.launch {
            progressStore.updateData { newProgress }
        }
    }

    private var feverJob: kotlinx.coroutines.Job? = null

    private fun startFever() {
        feverJob?.cancel()
        feverJob = viewModelScope.launch {
            _feverCountdown.value = 10
            while (_feverCountdown.value > 0 && isActive) {
                delay(1000)
                _feverCountdown.value -= 1
            }
        }
    }

    private fun checkAchievements(totalDamage: Long = _progress.value.totalDamage) {
        val unlockedTitle = Titles.filter { it.requirement <= totalDamage }.maxByOrNull { it.requirement }
        
        if (unlockedTitle != _playerState.value.currentTitle) {
            val newState = _playerState.value.copy(currentTitle = unlockedTitle)
            _playerState.value = newState
            viewModelScope.launch {
                playerStateStore.updateData { newState }
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
        isPlayerStateInitialized = true
        isProgressInitialized = true
        val pState = PlayerState()
        val prog = GameProgress()
        _playerState.value = pState
        _progress.value = prog
        _feverCountdown.value = 0
        _autoTapCountdown.value = 0
        viewModelScope.launch {
            playerStateStore.updateData { pState }
            progressStore.updateData { prog }
        }
    }

    fun markEndingAsSeen() {
        val newProg = _progress.value.copy(hasSeenEnding = true)
        _progress.value = newProg
        viewModelScope.launch {
            progressStore.updateData { newProg }
        }
    }

    fun startNewGamePlus() {
        val newProg = _progress.value.copy(
            remainingTaps = 1_000_000L,
            isCleared = false,
            startDateMillis = System.currentTimeMillis(),
            hasSeenEnding = false
        )
        _progress.value = newProg
        viewModelScope.launch {
            progressStore.updateData { newProg }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }

    fun clearForTest() {
        viewModelScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        onCleared()
    }

    fun debugAddGold(amount: Long) {
        val newState = _playerState.value.copy(gold = _playerState.value.gold + amount)
        _playerState.value = newState
        viewModelScope.launch {
            playerStateStore.updateData { newState }
        }
    }

    fun debugAddDamage(amount: Long) {
        updateProgress(amount, isManualTap = false)
    }

    fun debugTriggerFever() {
        startFever()
    }

    fun debugTriggerCritical() {
        viewModelScope.launch {
            val dummyOffset = Offset(540f, 1000f)
            _criticalHitEvent.emit(CriticalHitEvent(dummyOffset))
            val currentState = _playerState.value
            val (powerBonus, _) = calculateTitleBonuses()
            val baseDamage = (currentState.tapPower + powerBonus).toDouble()
            val multiplier = Random.nextDouble(1.5, 2.0)
            val critDamage = kotlin.math.round(baseDamage * multiplier).toLong().coerceAtLeast(1L)
            Log.d("GameViewModel", "[Debug] Critical Hit! Multiplier: %.2fx (Base: %.0f -> Final: %d)".format(multiplier, baseDamage, critDamage))
            updateProgress(critDamage, isManualTap = true)
        }
    }

    fun getUpgradeCost(level: Int): Long {
        if (level == 0) return BASE_UPGRADE_COST
        return (BASE_UPGRADE_COST * 1.15.pow(level.toDouble())).toLong()
    }

    fun upgradePower() {
        val currentState = _playerState.value
        val cost = if (currentState.hasFreeUpgrade) 0L else getUpgradeCost(currentState.powerLevel)
        if ((currentState.gold >= cost || currentState.hasFreeUpgrade) && currentState.powerLevel < MAX_POWER_LEVEL) {
            val newState = currentState.copy(
                gold = if (currentState.hasFreeUpgrade) currentState.gold else currentState.gold - cost,
                powerLevel = currentState.powerLevel + 1,
                hasFreeUpgrade = false
            )
            _playerState.value = newState
            viewModelScope.launch {
                playerStateStore.updateData { newState }
                audioManager.playSound("status_up")
            }
        }
    }

    fun upgradeCritical() {
        val currentState = _playerState.value
        val (_, titleCritBonus) = calculateTitleBonuses()
        val currentTotalCrit = currentState.criticalRate + titleCritBonus
        
        val cost = if (currentState.hasFreeUpgrade) 0L else getUpgradeCost(currentState.criticalLevel)
        if ((currentState.gold >= cost || currentState.hasFreeUpgrade) && currentTotalCrit < 1.0) {
            val newState = currentState.copy(
                gold = if (currentState.hasFreeUpgrade) currentState.gold else currentState.gold - cost,
                criticalLevel = currentState.criticalLevel + 1,
                hasFreeUpgrade = false
            )
            _playerState.value = newState
            viewModelScope.launch {
                playerStateStore.updateData { newState }
                audioManager.playSound("status_up")
            }
        }
    }

    fun upgradeAutoTap() {
        val currentState = _playerState.value
        val cost = if (currentState.hasFreeUpgrade) 0L else getUpgradeCost(currentState.autoTapLevel)
        if ((currentState.gold >= cost || currentState.hasFreeUpgrade) && currentState.autoTapLevel < MAX_AUTO_TAP_LEVEL) {
            val newState = currentState.copy(
                gold = if (currentState.hasFreeUpgrade) currentState.gold else currentState.gold - cost,
                autoTapLevel = currentState.autoTapLevel + 1,
                hasFreeUpgrade = false
            )
            _playerState.value = newState
            viewModelScope.launch {
                playerStateStore.updateData { newState }
                audioManager.playSound("status_up")
            }
        }
    }

    fun grantFreeUpgrade() {
        val newState = _playerState.value.copy(hasFreeUpgrade = true)
        _playerState.value = newState
        viewModelScope.launch {
            playerStateStore.updateData { newState }
        }
    }

    private fun startAutoTapLoop() {
        viewModelScope.launch {
            while (isActive) {
                val interval = _playerState.value.autoTapInterval
                if (interval > 0) {
                    _autoTapCountdown.value = interval
                    while (_autoTapCountdown.value > 0 && isActive) {
                        delay(1000)
                        val currentInterval = _playerState.value.autoTapInterval
                        if (currentInterval < interval && currentInterval > 0) break 
                        _autoTapCountdown.value -= 1
                    }
                    
                    if (_autoTapCountdown.value <= 0 && _playerState.value.autoTapInterval > 0) {
                        val currentState = _playerState.value
                        val (powerBonus, _) = calculateTitleBonuses()
                        val damage = (currentState.tapPower + powerBonus).toLong()
                        updateProgress(damage, isManualTap = false)
                    }
                } else {
                    _playerState.first { it.autoTapInterval > 0 }
                }
            }
        }
    }
}
