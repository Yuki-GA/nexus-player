package com.zen.myapplication.nexus.core.input

import android.view.KeyEvent
import android.webkit.WebView

/**
 * Abstract input model for Nexus Player.
 * Decouples physical keycodes and touch intents from the JS runtime.
 */
enum class NexusInputIntent {
    DPAD_UP, DPAD_DOWN, DPAD_LEFT, DPAD_RIGHT,
    ACTION_SOUTH, // Confirm / Z
    ACTION_EAST,  // Cancel / X
    ACTION_WEST,  // Shift / Run
    ACTION_NORTH, // Menu / Esc
    START,
    SELECT,
    L1, R1
}

/**
 * NexusInput: The authoritative owner of the input pipeline.
 * Translates abstract intents into target-specific events.
 */
class NexusInput(private val onIntent: (NexusInputIntent, Boolean) -> Unit) {

    // State tracking to prevent keydown spam and ensure clean keyup cycles
    private val activeIntents = mutableSetOf<NexusInputIntent>()

    /**
     * Dispatches an abstract input intent.
     * Handles keydown/keyup lifecycle automatically.
     */
    fun dispatchIntent(intent: NexusInputIntent, isPressed: Boolean) {
        if (isPressed) {
            if (activeIntents.add(intent)) {
                onIntent(intent, true)
            }
        } else {
            if (activeIntents.remove(intent)) {
                onIntent(intent, false)
            }
        }
    }

    /**
     * Directly maps an Android physical KeyEvent.
     */
    fun handleKeyEvent(event: KeyEvent): Boolean {
        val intent = mapAndroidKeyCode(event.keyCode) ?: return false
        val isPressed = event.action == KeyEvent.ACTION_DOWN
        dispatchIntent(intent, isPressed)
        return true
    }

    private fun mapAndroidKeyCode(keyCode: Int): NexusInputIntent? = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> NexusInputIntent.DPAD_UP
        KeyEvent.KEYCODE_DPAD_DOWN -> NexusInputIntent.DPAD_DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> NexusInputIntent.DPAD_LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> NexusInputIntent.DPAD_RIGHT
        KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_ENTER -> NexusInputIntent.ACTION_SOUTH
        KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BACK -> NexusInputIntent.ACTION_EAST
        KeyEvent.KEYCODE_BUTTON_X -> NexusInputIntent.ACTION_WEST
        KeyEvent.KEYCODE_BUTTON_Y -> NexusInputIntent.ACTION_NORTH
        KeyEvent.KEYCODE_BUTTON_START -> NexusInputIntent.START
        KeyEvent.KEYCODE_BUTTON_SELECT -> NexusInputIntent.SELECT
        KeyEvent.KEYCODE_BUTTON_L1 -> NexusInputIntent.L1
        KeyEvent.KEYCODE_BUTTON_R1 -> NexusInputIntent.R1
        else -> null
    }

    /**
     * Utility to generate the JavaScript for injecting a KeyEvent into a WebView.
     */
    fun getWebInjectionScript(intent: NexusInputIntent, isPressed: Boolean): String {
        val type = if (isPressed) "keydown" else "keyup"
        val mapping = getWebMapping(intent)
        return """
            (function() {
                const event = new KeyboardEvent('$type', {
                    key: '${mapping.key}',
                    code: '${mapping.code}',
                    keyCode: ${mapping.keyCode},
                    which: ${mapping.keyCode},
                    bubbles: true,
                    cancelable: true
                });
                window.dispatchEvent(event);
                
                // Compatibility for older RM plugins that use window.Input
                if (window.Input && window.Input._onKeyDown && '$type' === 'keydown') {
                    window.Input._onKeyDown(event);
                }
                if (window.Input && window.Input._onKeyUp && '$type' === 'keyup') {
                    window.Input._onKeyUp(event);
                }
            })();
        """.trimIndent()
    }

    private data class WebKey(val key: String, val code: String, val keyCode: Int)

    private fun getWebMapping(intent: NexusInputIntent): WebKey = when (intent) {
        NexusInputIntent.DPAD_UP -> WebKey("ArrowUp", "ArrowUp", 38)
        NexusInputIntent.DPAD_DOWN -> WebKey("ArrowDown", "ArrowDown", 40)
        NexusInputIntent.DPAD_LEFT -> WebKey("ArrowLeft", "ArrowLeft", 37)
        NexusInputIntent.DPAD_RIGHT -> WebKey("ArrowRight", "ArrowRight", 39)
        NexusInputIntent.ACTION_SOUTH -> WebKey("z", "KeyZ", 90) // Confirm
        NexusInputIntent.ACTION_EAST -> WebKey("x", "KeyX", 88)  // Cancel
        NexusInputIntent.ACTION_WEST -> WebKey("Shift", "ShiftLeft", 16) // Run
        NexusInputIntent.ACTION_NORTH -> WebKey("Escape", "Escape", 27) // Menu
        NexusInputIntent.START -> WebKey("Enter", "Enter", 13)
        NexusInputIntent.SELECT -> WebKey("Tab", "Tab", 9)
        NexusInputIntent.L1 -> WebKey("q", "KeyQ", 81)
        NexusInputIntent.R1 -> WebKey("w", "KeyW", 87)
    }
}

