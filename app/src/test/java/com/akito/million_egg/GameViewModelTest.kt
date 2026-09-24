package com.akito.million_egg

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GameViewModelTest {
    private lateinit var viewModel: GameViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Application>()
        viewModel = GameViewModel(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial remaining taps should be 1 million`() = runTest {
        assertEquals(1_000_000L, viewModel.progress.value.remainingTaps)
    }

    @Test
    fun `tapping reduces remaining taps and increases tapCount`() = runTest {
        val initialTaps = viewModel.progress.value.remainingTaps
        val initialCount = viewModel.progress.value.tapCount
        
        viewModel.onTap()
        runCurrent()
        
        assertTrue(viewModel.progress.value.remainingTaps < initialTaps)
        assertEquals(initialCount + 1, viewModel.progress.value.tapCount)
    }

    @Test
    fun `upgrading power increases damage`() = runTest {
        viewModel.upgradePower() 
        runCurrent()
        
        viewModel.onTap()
        runCurrent()
        
        val damage = 1_000_000L - viewModel.progress.value.remainingTaps
        // powerLevel starts at 1, upgradePower makes it 2
        assertTrue(damage >= 2)
    }

    @Test
    fun `auto tap reduces remaining taps but does not increase tapCount`() = runTest {
        viewModel.upgradeAutoTap() // Lv 1 (30s interval)
        runCurrent()
        
        // Wait for 31 seconds to trigger auto tap
        advanceTimeBy(31000)
        
        val damage = 1_000_000L - viewModel.progress.value.remainingTaps
        assertTrue(damage >= 1)
        assertEquals(0, viewModel.progress.value.tapCount)
    }

    @Test
    fun `achieving total damage unlocks title`() = runTest {
        // We'll simulate 5000 damage
        // To speed up, we could just check Title list, but let's do a few taps
        // Since we want to test "Achievement transitions", 5000 might take too long in real taps
        // But in runTest with StandardTestDispatcher, it's just loop iterations.
        
        viewModel.debugAddDamage(5000L)
        runCurrent()
        
        assertEquals("タップ初心者", viewModel.playerState.value.currentTitle?.name)
    }

    @Test
    fun `reaching 0 remaining taps triggers clear state and reset works`() = runTest {
        viewModel.debugAddDamage(1_000_000L)
        runCurrent()
        
        assertTrue(viewModel.progress.value.isCleared)
        assertEquals(0L, viewModel.progress.value.remainingTaps)

        // Test playGameClearSound
        viewModel.playGameClearSound()

        // Test reset
        viewModel.resetGame()
        runCurrent()

        assertEquals(1_000_000L, viewModel.progress.value.remainingTaps)
        assertEquals(false, viewModel.progress.value.isCleared)
    }
}
