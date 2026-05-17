package com.zen.myapplication.nexus.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nexus_settings")

/**
 * Manages advanced user preferences persistently using Jetpack DataStore.
 */
class SettingsManager(private val context: Context) {

    companion object {
        // --- UI & Controller ---
        val CONTROLLER_OPACITY = floatPreferencesKey("controller_opacity")
        val CONTROLLER_SIZE = floatPreferencesKey("controller_size")
        val SHOW_FPS = booleanPreferencesKey("show_fps")

        // --- Engine & Compatibility ---
        val FORCE_HARDWARE_ACCEL = booleanPreferencesKey("force_hardware_accel")
        val ENABLE_WEBGL_2 = booleanPreferencesKey("enable_webgl_2")
        val USER_AGENT_SPOOF = stringPreferencesKey("user_agent_spoof")
        val FPS_LIMIT = intPreferencesKey("fps_limit")
        val RENDERER_MODE = stringPreferencesKey("renderer_mode")

        // --- AI & Translation ---
        val TRANSLATION_ENABLED = booleanPreferencesKey("translation_enabled")
        val TARGET_LANGUAGE = stringPreferencesKey("target_language")
    }

    // Default Values & Flows
    val controllerOpacity: Flow<Float> = context.dataStore.data.map { it[CONTROLLER_OPACITY] ?: 0.6f }
    val controllerSize: Flow<Float> = context.dataStore.data.map { it[CONTROLLER_SIZE] ?: 1.0f }
    val showFps: Flow<Boolean> = context.dataStore.data.map { it[SHOW_FPS] ?: false }
    
    val forceHardwareAccel: Flow<Boolean> = context.dataStore.data.map { it[FORCE_HARDWARE_ACCEL] ?: true }
    val fpsLimit: Flow<Int> = context.dataStore.data.map { it[FPS_LIMIT] ?: 60 }
    val rendererMode: Flow<String> = context.dataStore.data.map { it[RENDERER_MODE] ?: "auto" }
    
    val translationEnabled: Flow<Boolean> = context.dataStore.data.map { it[TRANSLATION_ENABLED] ?: true }
    val targetLanguage: Flow<String> = context.dataStore.data.map { it[TARGET_LANGUAGE] ?: "English" }

    suspend fun setControllerOpacity(opacity: Float) {
        context.dataStore.edit { it[CONTROLLER_OPACITY] = opacity.coerceIn(0.2f, 1.0f) }
    }

    suspend fun setControllerSize(size: Float) {
        context.dataStore.edit { it[CONTROLLER_SIZE] = size.coerceIn(0.75f, 1.35f) }
    }

    suspend fun setShowFps(enabled: Boolean) {
        context.dataStore.edit { it[SHOW_FPS] = enabled }
    }

    suspend fun setRendererMode(mode: String) {
        context.dataStore.edit { it[RENDERER_MODE] = mode }
    }

    suspend fun setTranslationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[TRANSLATION_ENABLED] = enabled }
    }

    suspend fun setForceHardwareAccel(enabled: Boolean) {
        context.dataStore.edit { it[FORCE_HARDWARE_ACCEL] = enabled }
    }

    suspend fun setFpsLimit(limit: Int) {
        context.dataStore.edit { it[FPS_LIMIT] = limit.coerceIn(30, 120) }
    }

    suspend fun setTargetLanguage(language: String) {
        context.dataStore.edit { it[TARGET_LANGUAGE] = language }
    }
}
