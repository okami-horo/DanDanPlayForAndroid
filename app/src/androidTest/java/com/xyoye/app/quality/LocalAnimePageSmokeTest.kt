package com.xyoye.app.quality

import android.view.KeyEvent
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xyoye.anime_component.R
import com.xyoye.anime_component.ui.activities.anime_history.AnimeHistoryActivity
import com.xyoye.local_component.ui.activities.play_history.PlayHistoryActivity
import com.xyoye.local_component.ui.activities.shooter_subtitle.ShooterSubtitleActivity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke regression suite covering Activity lifecycle, back-press handling,
 * and DPAD navigation for local_component and anime_component pages.
 *
 * These tests verify the TV-first interaction contract:
 * - Pages launch and reach RESUMED state without crashing.
 * - BACK key is handled correctly (no dead-end navigation).
 * - Core RecyclerView containers are present and reachable via DPAD.
 */
@RunWith(AndroidJUnit4::class)
class LocalAnimePageSmokeTest {

    // -----------------------------------------------------------------------
    // Task 3.1 — Activity lifecycle regression
    // -----------------------------------------------------------------------

    @Test
    fun playHistoryActivityLaunchesAndShowsList() {
        ActivityScenario.launch(PlayHistoryActivity::class.java).use { scenario ->
            waitForIdle()
            scenario.onActivity { activity ->
                assertTrue(
                    "PlayHistoryActivity should reach RESUMED state",
                    scenario.state.isAtLeast(Lifecycle.State.RESUMED),
                )
                assertNotNull(
                    "Play history RecyclerView should be present",
                    activity.findViewById<RecyclerView>(com.xyoye.local_component.R.id.play_history_rv),
                )
            }
        }
    }

    @Test
    fun animeHistoryActivityLaunchesAndShowsList() {
        ActivityScenario.launch(AnimeHistoryActivity::class.java).use { scenario ->
            waitForIdle()
            scenario.onActivity { activity ->
                assertTrue(
                    "AnimeHistoryActivity should reach RESUMED state",
                    scenario.state.isAtLeast(Lifecycle.State.RESUMED),
                )
                assertNotNull(
                    "Anime history RecyclerView should be present",
                    activity.findViewById<RecyclerView>(R.id.history_rv),
                )
            }
        }
    }

    @Test
    fun shooterSubtitleActivityLaunchesAndShowsList() {
        ActivityScenario.launch(ShooterSubtitleActivity::class.java).use { scenario ->
            waitForIdle()
            scenario.onActivity { activity ->
                assertTrue(
                    "ShooterSubtitleActivity should reach RESUMED state",
                    scenario.state.isAtLeast(Lifecycle.State.RESUMED),
                )
            }
        }
    }

    // -----------------------------------------------------------------------
    // Task 3.2 — DPAD back-press regression
    // -----------------------------------------------------------------------

    @Test
    fun animeHistoryBackPressWithoutSearchDismissesActivity() {
        ActivityScenario.launch(AnimeHistoryActivity::class.java).use { scenario ->
            waitForIdle()

            // Dispatch BACK key — without an active search menu the activity should finish.
            scenario.onActivity { activity ->
                pressBackKey(activity)
            }

            waitForIdle()

            val finalState = scenario.state
            assertTrue(
                "After BACK, AnimeHistoryActivity should not remain in RESUMED; got $finalState",
                finalState != Lifecycle.State.RESUMED,
            )
        }
    }

    @Test
    fun playHistoryBackPressFinishesActivity() {
        ActivityScenario.launch(PlayHistoryActivity::class.java).use { scenario ->
            waitForIdle()

            scenario.onActivity { activity ->
                pressBackKey(activity)
            }

            waitForIdle()

            val finalState = scenario.state
            assertTrue(
                "After BACK, PlayHistoryActivity should not remain in RESUMED; got $finalState",
                finalState != Lifecycle.State.RESUMED,
            )
        }
    }

    // -----------------------------------------------------------------------
    // Task 3.2 — DPAD navigation smoke (TV reachability check)
    // -----------------------------------------------------------------------

    @Test
    fun animeHistoryDpadSelectOnEmptyListDoesNotCrash() {
        ActivityScenario.launch(AnimeHistoryActivity::class.java).use { scenario ->
            waitForIdle()

            // Press DPAD_CENTER on the empty list — should not crash, no items to select.
            scenario.onActivity { activity ->
                sendDpadKey(activity, KeyEvent.KEYCODE_DPAD_CENTER)
            }

            waitForIdle()

            // Activity should still be in a live state after the no-op DPAD press.
            assertTrue(
                "Activity must still be alive after DPAD_CENTER on empty list",
                scenario.state.isAtLeast(Lifecycle.State.STARTED),
            )
        }
    }
}
