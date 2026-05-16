package com.zen.myapplication.nexus.core.storage

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

/**
 * SaveManager: Orchestrates game save mirroring between WebView and SAF.
 * Ensures saves persist across app clearing and provide atomic write safety.
 */
class SaveManager(private val context: Context) {

    companion object {
        private const val TAG = "SaveManager"
        private const val SAVE_DIR_NAME = "save"
    }

    /**
     * Persists a save file to the SAF game directory.
     * @param rootUri The root URI of the game.
     * @param fileName The name of the save file (e.g., "file1.rpgsave").
     * @param data The save data bytes.
     */
    suspend fun persistSave(rootUri: Uri, fileName: String, data: ByteArray) = withContext(Dispatchers.IO) {
        try {
            val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return@withContext
            
            // Ensure 'save' directory exists
            var saveDir = rootDoc.findFile(SAVE_DIR_NAME)
            if (saveDir == null || !saveDir.isDirectory) {
                saveDir = rootDoc.createDirectory(SAVE_DIR_NAME)
            }
            
            if (saveDir == null) {
                Log.e(TAG, "Failed to create save directory.")
                return@withContext
            }

            // Phase E: Atomic Save Writes (Corruption Fallback)
            val tempFileName = "$fileName.tmp"
            var tempSaveFile = saveDir.findFile(tempFileName)
            
            // Clean up old temp file if a previous write crashed
            if (tempSaveFile != null) {
                tempSaveFile.delete()
            }
            
            tempSaveFile = saveDir.createFile("application/octet-stream", tempFileName)

            if (tempSaveFile != null) {
                context.contentResolver.openOutputStream(tempSaveFile.uri, "wt")?.use { output ->
                    output.write(data)
                    output.flush()
                }
                
                // Atomic commit: rename .tmp to final name
                // DocumentFile renameTo fails if target exists, so we delete existing first
                val existingSave = saveDir.findFile(fileName)
                existingSave?.delete()
                
                val success = tempSaveFile.renameTo(fileName)
                if (success) {
                    Log.i(TAG, "Successfully persisted save atomically: $fileName")
                } else {
                    Log.e(TAG, "Failed to commit atomic save (rename failed): $fileName")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist save: $fileName", e)
        }
    }

    /**
     * Lists all saves available in the SAF directory.
     */
    suspend fun listSaves(rootUri: Uri): List<String> = withContext(Dispatchers.IO) {
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return@withContext emptyList()
        val saveDir = rootDoc.findFile(SAVE_DIR_NAME) ?: return@withContext emptyList()
        
        return@withContext saveDir.listFiles()
            .filter { it.isFile }
            .mapNotNull { it.name }
    }

    /**
     * Reads a save file from the SAF directory.
     */
    suspend fun readSave(rootUri: Uri, fileName: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return@withContext null
            val saveDir = rootDoc.findFile(SAVE_DIR_NAME) ?: return@withContext null
            val saveFile = saveDir.findFile(fileName) ?: return@withContext null
            
            return@withContext context.contentResolver.openInputStream(saveFile.uri)?.use { input ->
                input.readBytes()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read save: $fileName", e)
            null
        }
    }
}
