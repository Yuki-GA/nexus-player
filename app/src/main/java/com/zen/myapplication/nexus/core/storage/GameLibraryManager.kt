package com.zen.myapplication.nexus.core.storage

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val Context.gameLibraryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "nexus_game_library"
)

data class GameEntry(
    val name: String,
    val uri: String,
    val engine: SafManager.GameEngine
)

class GameLibraryManager(private val context: Context) {
    private val gamesKey = stringSetPreferencesKey("games")

    val games: Flow<List<GameEntry>> = context.gameLibraryDataStore.data.map { preferences ->
        preferences[gamesKey]
            .orEmpty()
            .mapNotNull(::decodeEntry)
            .sortedBy { it.name.lowercase() }
    }

    suspend fun addGame(uri: Uri, engine: SafManager.GameEngine) {
        val safManager = SafManager(context)
        val displayName = DocumentFile.fromTreeUri(context, uri)?.name ?: "Imported Game"
        val entry = GameEntry(displayName, uri.toString(), engine)

        context.gameLibraryDataStore.edit { preferences ->
            val updatedEntries = preferences[gamesKey].orEmpty()
                .mapNotNull(::decodeEntry)
                .filterNot { it.uri == entry.uri }
                .plus(entry)
                .map(::encodeEntry)
                .toSet()

            preferences[gamesKey] = updatedEntries
        }

        // Trigger indexing after adding to library
        safManager.indexGameAssets(uri)
    }

    suspend fun removeGame(uri: String) {
        val safManager = SafManager(context)
        context.gameLibraryDataStore.edit { preferences ->
            preferences[gamesKey] = preferences[gamesKey]
                .orEmpty()
                .mapNotNull(::decodeEntry)
                .filterNot { it.uri == uri }
                .map(::encodeEntry)
                .toSet()
        }
        
        // Clear VFS index for the removed game
        safManager.clearVfs(Uri.parse(uri))
    }

    private fun encodeEntry(entry: GameEntry): String {
        return listOf(entry.name, entry.uri, entry.engine.name)
            .joinToString(separator = "|") { value ->
                URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
            }
    }

    private fun decodeEntry(raw: String): GameEntry? {
        val parts = raw.split("|")
        if (parts.size != 3) return null

        return try {
            GameEntry(
                name = URLDecoder.decode(parts[0], StandardCharsets.UTF_8.toString()),
                uri = URLDecoder.decode(parts[1], StandardCharsets.UTF_8.toString()),
                engine = SafManager.GameEngine.valueOf(
                    URLDecoder.decode(parts[2], StandardCharsets.UTF_8.toString())
                )
            )
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
