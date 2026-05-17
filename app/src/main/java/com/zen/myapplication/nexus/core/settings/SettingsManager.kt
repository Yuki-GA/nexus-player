package com.zen.myapplication.nexus.core.settings

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nexus_settings")

/**
 * Manages advanced user preferences persistently using Jetpack DataStore.
 * Final production-grade control center backend.
 */
class SettingsManager(private val context: Context) {

    companion object {
        // --- APPEARANCE ---
        val ACCENT_COLOR = intPreferencesKey("accent_color")
        val UI_DENSITY = stringPreferencesKey("ui_density")
        val CORNER_RADIUS = intPreferencesKey("corner_radius")
        val BLUR_INTENSITY = floatPreferencesKey("blur_intensity")
        val GLASS_TRANSPARENCY = floatPreferencesKey("glass_transparency")
        val AMOLED_MODE = booleanPreferencesKey("amoled_mode")
        val ANIMATION_SPEED = floatPreferencesKey("animation_speed")

        // --- LAYOUT ---
        val SIDEBAR_COMPACT = booleanPreferencesKey("sidebar_compact")
        val SHOW_TELEMETRY = booleanPreferencesKey("show_telemetry")
        val CARD_SPACING = intPreferencesKey("card_spacing")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")

        // --- RUNTIME ---
        val FORCE_HARDWARE_ACCEL = booleanPreferencesKey("force_hardware_accel")
        val FORCE_WEBGL = booleanPreferencesKey("force_webgl")
        val STABILIZATION_ENABLED = booleanPreferencesKey("stabilization_enabled")
        val FPS_LIMIT = intPreferencesKey("fps_limit")
        val DEBUG_MODE = booleanPreferencesKey("debug_mode")

        // --- INPUT ---
        val CONTROLLER_OPACITY = floatPreferencesKey("controller_opacity")
        val CONTROLLER_SIZE = floatPreferencesKey("controller_size")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val ANALOG_MODE = booleanPreferencesKey("analog_mode")
        val LEFT_HANDED = booleanPreferencesKey("left_handed")
        val CONTROLLER_PRESET = stringPreferencesKey("controller_preset")
        val SHOW_FPS = booleanPreferencesKey("show_fps")

        // --- LIBRARY ---
        val SORT_MODE = stringPreferencesKey("sort_mode")
        val SHOW_HIDDEN = booleanPreferencesKey("show_hidden")
        val FETCH_ARTWORK = booleanPreferencesKey("fetch_artwork")

        // --- OVERLAY POSITIONING ---
        val DPAD_OFFSET_X = floatPreferencesKey("dpad_offset_x")
        val DPAD_OFFSET_Y = floatPreferencesKey("dpad_offset_y")
        val ACTION_OFFSET_X = floatPreferencesKey("action_offset_x")
        val ACTION_OFFSET_Y = floatPreferencesKey("action_offset_y")
    }

    // --- FLOWS ---
    val dpadOffset: Flow<Pair<Float, Float>> = context.dataStore.data.map { 
        (it[DPAD_OFFSET_X] ?: 0f) to (it[DPAD_OFFSET_Y] ?: 0f) 
    }
    val actionOffset: Flow<Pair<Float, Float>> = context.dataStore.data.map { 
        (it[ACTION_OFFSET_X] ?: 0f) to (it[ACTION_OFFSET_Y] ?: 0f) 
    }

    val accentColor: Flow<Int> = context.dataStore.data.map { it[ACCENT_COLOR] ?: 0xFF00E5FF.toInt() }
    val amoledMode: Flow<Boolean> = context.dataStore.data.map { it[AMOLED_MODE] ?: true }
    val glassFx: Flow<Boolean> = context.dataStore.data.map { it[GLASS_TRANSPARENCY] != 0f } // Simplified check
    val cornerRadius: Flow<Int> = context.dataStore.data.map { it[CORNER_RADIUS] ?: 20 }
    val sidebarCompact: Flow<Boolean> = context.dataStore.data.map { it[SIDEBAR_COMPACT] ?: false }
    val showTelemetry: Flow<Boolean> = context.dataStore.data.map { it[SHOW_TELEMETRY] ?: true }
    
    val controllerOpacity: Flow<Float> = context.dataStore.data.map { it[CONTROLLER_OPACITY] ?: 0.6f }
    val controllerSize: Flow<Float> = context.dataStore.data.map { it[CONTROLLER_SIZE] ?: 1.0f }
    val hapticFeedback: Flow<Boolean> = context.dataStore.data.map { it[HAPTIC_FEEDBACK] ?: true }
    
    val forceHardwareAccel: Flow<Boolean> = context.dataStore.data.map { it[FORCE_HARDWARE_ACCEL] ?: true }
    val fpsLimit: Flow<Int> = context.dataStore.data.map { it[FPS_LIMIT] ?: 60 }
    val debugMode: Flow<Boolean> = context.dataStore.data.map { it[DEBUG_MODE] ?: false }

    // --- SETTERS ---
    suspend fun setAccentColor(color: Color) { context.dataStore.edit { it[ACCENT_COLOR] = color.toArgb() } }
    suspend fun setAmoledMode(enabled: Boolean) { context.dataStore.edit { it[AMOLED_MODE] = enabled } }
    suspend fun setGlassFx(enabled: Boolean) { context.dataStore.edit { it[GLASS_TRANSPARENCY] = if (enabled) 0.15f else 0f } }
    suspend fun setCornerRadius(radius: Int) { context.dataStore.edit { it[CORNER_RADIUS] = radius } }
    suspend fun setSidebarCompact(compact: Boolean) { context.dataStore.edit { it[SIDEBAR_COMPACT] = compact } }
    suspend fun setShowTelemetry(show: Boolean) { context.dataStore.edit { it[SHOW_TELEMETRY] = show } }
    
    suspend fun setControllerOpacity(opacity: Float) { context.dataStore.edit { it[CONTROLLER_OPACITY] = opacity.coerceIn(0.1f, 1.0f) } }
    suspend fun setControllerSize(size: Float) { context.dataStore.edit { it[CONTROLLER_SIZE] = size.coerceIn(0.5f, 1.5f) } }
    suspend fun setHapticFeedback(enabled: Boolean) { context.dataStore.edit { it[HAPTIC_FEEDBACK] = enabled } }
    
    suspend fun setForceHardwareAccel(enabled: Boolean) { context.dataStore.edit { it[FORCE_HARDWARE_ACCEL] = enabled } }
    suspend fun setFpsLimit(limit: Int) { context.dataStore.edit { it[FPS_LIMIT] = limit } }
    suspend fun setDebugMode(enabled: Boolean) { context.dataStore.edit { it[DEBUG_MODE] = enabled } }

    suspend fun setDpadOffset(x: Float, y: Float) {
        context.dataStore.edit { 
            it[DPAD_OFFSET_X] = x
            it[DPAD_OFFSET_Y] = y
        }
    }

    suspend fun setActionOffset(x: Float, y: Float) {
        context.dataStore.edit { 
            it[ACTION_OFFSET_X] = x
            it[ACTION_OFFSET_Y] = y
        }
    }

    suspend fun resetCategory(category: String) {
        context.dataStore.edit { prefs ->
            when(category) {
                "Appearance" -> { prefs.remove(ACCENT_COLOR); prefs.remove(AMOLED_MODE) }
                "Input" -> { prefs.remove(CONTROLLER_OPACITY); prefs.remove(CONTROLLER_SIZE); prefs.remove(HAPTIC_FEEDBACK) }
                "Runtime" -> { prefs.remove(FORCE_HARDWARE_ACCEL); prefs.remove(FPS_LIMIT) }
            }
        }
    }
}
