package com.xyoye.app.quality

import android.app.Activity
import android.view.KeyEvent
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Shared helpers for instrumentation smoke tests.
 *
 * Centralising these one-liners removes duplicated boilerplate from every
 * smoke-test class and provides a stable fixture for future tests.
 *
 * Usage (from any class in this package or via import):
 *   waitForIdle()
 *   sendDpadKey(activity, KeyEvent.KEYCODE_DPAD_DOWN)
 *   pressBackKey(activity)
 */

/** Wait until the main looper is idle and all pending UI work has completed. */
fun waitForIdle() {
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
}

/**
 * Dispatch a single DPAD key press (DOWN + UP) to [activity].
 *
 * @param activity  The activity that should receive the key events.
 * @param keyCode   One of [KeyEvent.KEYCODE_DPAD_UP], [KEYCODE_DPAD_DOWN],
 *                  [KEYCODE_DPAD_LEFT], [KEYCODE_DPAD_RIGHT], [KEYCODE_DPAD_CENTER], etc.
 */
fun sendDpadKey(activity: Activity, keyCode: Int) {
    activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
    activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
}

/**
 * Dispatch a BACK key press (DOWN + UP) to [activity].
 *
 * Equivalent to pressing the physical back button on a TV remote.
 */
fun pressBackKey(activity: Activity) = sendDpadKey(activity, KeyEvent.KEYCODE_BACK)
