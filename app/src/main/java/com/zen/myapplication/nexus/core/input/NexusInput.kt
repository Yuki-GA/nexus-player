package com.zen.myapplication.nexus.core.input

import android.view.KeyEvent
import android.util.Log

/**
 * NexusInput: The authoritative owner of the input pipeline.
 * Emulates a professional PC Keyboard environment for RPG Maker.
 */
class NexusInput(private val onInject: (Int, Boolean) -> Unit) {

    private val activeKeys = mutableSetOf<Int>()

    companion object {
        const val KEY_UP = 38
        const val KEY_DOWN = 40
        const val KEY_LEFT = 37
        const val KEY_RIGHT = 39
        const val KEY_Z = 90
        const val KEY_X = 88
        const val KEY_SHIFT = 16
        const val KEY_ENTER = 13
        const val KEY_ESCAPE = 27
        const val KEY_Q = 81
        const val KEY_W = 87
    }

    fun reset() {
        Log.e("NEXUS_INPUT", "Resetting input state")
        activeKeys.clear()
    }

    fun dispatchKey(keyCode: Int, isPressed: Boolean) {
        val keyName = getKeyName(keyCode)
        val action = if (isPressed) "DOWN" else "UP"
        Log.e("NEXUS_INPUT", "$action: $keyName")
        Log.e("NEXUS_INPUT", "dispatch -> $keyCode ($keyName)")

        if (isPressed) {
            if (activeKeys.add(keyCode)) {
                onInject(keyCode, true)
            }
        } else {
            if (activeKeys.remove(keyCode)) {
                onInject(keyCode, false)
            }
        }
    }

    private fun getKeyName(keyCode: Int): String = when (keyCode) {
        KEY_UP -> "UP"
        KEY_DOWN -> "DOWN"
        KEY_LEFT -> "LEFT"
        KEY_RIGHT -> "RIGHT"
        KEY_Z -> "Z"
        KEY_X -> "X"
        KEY_SHIFT -> "SHIFT"
        KEY_ENTER -> "ENTER"
        KEY_ESCAPE -> "ESCAPE"
        else -> "UNKNOWN ($keyCode)"
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        val pcKey = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> KEY_UP
            KeyEvent.KEYCODE_DPAD_DOWN -> KEY_DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> KEY_LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> KEY_RIGHT
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> KEY_ENTER
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_BUTTON_B -> KEY_ESCAPE
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_BUTTON_X -> KEY_SHIFT
            KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BUTTON_START -> KEY_ESCAPE
            else -> return false
        }
        
        dispatchKey(pcKey, event.action == KeyEvent.ACTION_DOWN)
        return true
    }

    /**
     * Hardened Deep Bridge Injection:
     * Calls the centralized __nexus.input handler which is synchronized with the engine loop.
     */
    fun getWebInjectionScript(keyCode: Int, isPressed: Boolean): String {
        return "if(window.__nexus && __nexus.input) __nexus.input.dispatch($keyCode, $isPressed);"
    }
}
