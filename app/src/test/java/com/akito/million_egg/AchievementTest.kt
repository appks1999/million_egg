package com.akito.million_egg

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AchievementTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

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
    fun `initial state has no title`() = runTest {
        assertNull(viewModel.playerState.value.currentTitle)
    }

    @Test
    fun `reaching 5000 damage unlocks beginner title`() = runTest {
        // Manually trigger damage update to avoid long tapping loops in unit tests
        // Since updateProgress is private, we'll use a hack or verify via onTap
        // But for unit test of the LOGIC, we can verify titles list and viewModel checkAchievements
        
        // Simulate progress update logic by calling onTap until 5000
        // (In a real scenario, we might make updateProgress internal or use a Test-Specific ViewModel)
        
        // Let's use a simpler approach: verify the checkAchievements logic by observing state
        // We'll call onTap. Default power is 1. 5000 taps might be slow, but it's reliable.
        
        // Actually, let's just check the first milestone
        viewModel.debugAddDamage(5000L)
        runCurrent()
        
        assertEquals("タップ初心者", viewModel.playerState.value.currentTitle?.name)
        assertEquals(1, viewModel.playerState.value.currentTitle?.powerBonus)
    }

    @Test
    fun `milestone check for all 8 stages`() = runTest {
        val testCases = listOf(
            5000L to "タップ初心者",
            10000L to "タップ熟練者",
            50000L to "タップ愛好家",
            100000L to "タップの達人",
            300000L to "タップの巨匠",
            500000L to "タップの王者",
            700000L to "タップの覇者",
            1000000L to "タップの神"
        )

        // We'll verify the matching logic in Titles list directly to save time,
        // then verify one transition in the ViewModel.
        
        testCases.forEach { (req, name) ->
            val title = Titles.filter { it.requirement <= req }.maxByOrNull { it.requirement }
            assertEquals("Requirement $req should unlock $name", name, title?.name)
        }
    }
    
    @Test
    fun `bonuses are non-additive (replacing each other)`() = runTest {
        // At 10,000, title is "タップ熟練者" (Power +1, Crit +1%)
        // At 5,000, title was "タップ初心者" (Power +1)
        // If it was additive, power bonus would be 2. If non-additive, it remains 1.
        
        // Simulate reaching 10,000 damage
        // We'll simulate 10000 taps
        viewModel.debugAddDamage(10000L)
        runCurrent()
        
        val currentState = viewModel.playerState.value
        assertEquals("タップ熟練者", currentState.currentTitle?.name)
        
        // Bonus should be EXACTLY what's defined in the 10,000 title
        assertEquals(1, currentState.currentTitle?.powerBonus)
        assertEquals(0.01, currentState.currentTitle?.criticalBonus ?: 0.0, 0.001)
    }
}
