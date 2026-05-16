package com.zen.myapplication.nexus.core.input

/**
 * Centralized mapping of Nexus abstract inputs to physical keycodes.
 * This ensures consistency across Web and Native runtimes.
 */
object NexusInput {
    // Directional Keycodes
    const val KEY_UP = 38
    const val KEY_DOWN = 40
    const val KEY_LEFT = 37
    const val KEY_RIGHT = 39

    // Action Keycodes
    const val KEY_Z = 90
    const val KEY_X = 88
    const val KEY_ENTER = 13
    const val KEY_SHIFT = 16
    const val KEY_ESC = 27

    /**
     * Maps an abstract direction to its physical keycode.
     */
    fun getDirectionKeycode(direction: DPadDirection): Int = when (direction) {
        DPadDirection.UP -> KEY_UP
        DPadDirection.DOWN -> KEY_DOWN
        DPadDirection.LEFT -> KEY_LEFT
        DPadDirection.RIGHT -> KEY_RIGHT
    }

    /**
     * Maps an abstract action to its physical keycode.
     */
    fun getActionKeycode(action: ActionKey): Int = when (action) {
        ActionKey.Z -> KEY_Z
        ActionKey.X -> KEY_X
        ActionKey.ENTER -> KEY_ENTER
        ActionKey.SHIFT -> KEY_SHIFT
        ActionKey.ESC -> KEY_ESC
    }
}

enum class DPadDirection { UP, DOWN, LEFT, RIGHT }
enum class ActionKey { Z, X, ENTER, SHIFT, ESC }
