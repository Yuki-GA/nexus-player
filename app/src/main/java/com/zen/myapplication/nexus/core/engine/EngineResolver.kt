package com.zen.myapplication.nexus.core.engine

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import org.json.JSONObject
import java.nio.charset.StandardCharsets

enum class EngineType {
    RPG_MAKER_MV,
    RPG_MAKER_MZ,
    UNKNOWN
}

data class EngineConfig(
    val type: EngineType,
    val webRoot: String, // Path to the directory containing index.html
    val mainHtml: String = "index.html",
    val encryptionKey: String? = null, // XOR key for encrypted assets
    val version: String = "unknown"
)

/**
 * EngineResolver: Identifies the game engine and its configuration.
 */
class EngineResolver(private val context: Context) {

    fun resolve(rootUri: Uri): EngineConfig {
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return EngineConfig(EngineType.UNKNOWN, "")

        // 1. Check for MZ (typically root-level)
        val mzCore = rootDoc.findFile("js")?.findFile("rmmz_core.js")
        if (mzCore != null) {
            val mainHtml = parseMainFromPackageJson(rootDoc) ?: "index.html"
            val encryptionKey = parseEncryptionKey(rootDoc)
            return EngineConfig(EngineType.RPG_MAKER_MZ, "", mainHtml, encryptionKey)
        }

        // 2. Check for MV (typically inside www/)
        val wwwDir = rootDoc.findFile("www")
        if (wwwDir != null && wwwDir.isDirectory) {
            val mvCore = wwwDir.findFile("js")?.findFile("rpg_core.js")
            if (mvCore != null) {
                val mainHtml = parseMainFromPackageJson(wwwDir) ?: "index.html"
                val encryptionKey = parseEncryptionKey(wwwDir)
                return EngineConfig(EngineType.RPG_MAKER_MV, "www", mainHtml, encryptionKey)
            }
        }

        // 3. Fallback check for root-level MV
        val rootMvCore = rootDoc.findFile("js")?.findFile("rpg_core.js")
        if (rootMvCore != null) {
            val mainHtml = parseMainFromPackageJson(rootDoc) ?: "index.html"
            val encryptionKey = parseEncryptionKey(rootDoc)
            return EngineConfig(EngineType.RPG_MAKER_MV, "", mainHtml, encryptionKey)
        }

        return EngineConfig(EngineType.UNKNOWN, "")
    }

    /**
     * Parses the encryptionKey from data/System.json.
     */
    private fun parseEncryptionKey(directory: DocumentFile): String? {
        val dataDir = directory.findFile("data") ?: return null
        val systemFile = dataDir.findFile("System.json") ?: return null
        return try {
            val jsonString = context.contentResolver.openInputStream(systemFile.uri)?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }
            if (jsonString != null) {
                val json = JSONObject(jsonString)
                json.optString("encryptionKey").takeIf { it.isNotEmpty() }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parses the 'main' field from NW.js package.json if it exists.
     */
    private fun parseMainFromPackageJson(directory: DocumentFile): String? {
        val packageFile = directory.findFile("package.json") ?: return null
        return try {
            val jsonString = context.contentResolver.openInputStream(packageFile.uri)?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }
            if (jsonString != null) {
                val json = JSONObject(jsonString)
                val main = json.optString("main")
                if (main.isNotEmpty()) main else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
