package com.zen.myapplication.nexus.core.vfs

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceResponse
import androidx.documentfile.provider.DocumentFile
import com.zen.myapplication.nexus.data.db.NexusVfsIndex
import com.zen.myapplication.nexus.data.db.VfsIndexEntry
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

class NexusVFS(private val context: Context) {

    private val vfsIndex = NexusVfsIndex(context)

    var hasTranslation = false
        private set

    // Redirection map for runtime script patching
    private val scriptRedirections = mapOf(
        "js/libs/pixi.js" to "ruffle/optimized_pixi_v5.js",
        "js/libs/fpsmeter.js" to "ruffle/nexus_fpsmeter.js"
    )

    // Mapping of standard extensions to potential encrypted extensions
    private val encryptedExtensions = mapOf(
        "png" to listOf("rpgmvp", "rpgmzp", "png_"),
        "ogg" to listOf("rpgmvo", "rpgmzo", "ogg_"),
        "m4a" to listOf("rpgmvm", "rpgmzm", "m4a_")
    )

    /**
     * Specialized asset opener with support for:
     * 1. Script redirection (patches)
     * 2. Early-boot bootstrapper injection
     * 3. On-the-fly XOR decryption
     * 4. Case-insensitive path normalization
     */
    fun openInterceptedAsset(
        gameId: String, 
        path: String, 
        cache: Map<String, Uri>,
        encryptionKey: String? = null
    ): WebResourceResponse? {
        val normalizedPath = path.trimStart('/').lowercase()
        
        // 1. Check for script redirections
        val redirectedPath = scriptRedirections[normalizedPath]
        if (redirectedPath != null) {
            try {
                val assetStream = context.assets.open(redirectedPath)
                return WebResourceResponse(getMimeType(redirectedPath), "UTF-8", assetStream)
            } catch (e: Exception) {
                Log.w("NexusVFS", "Failed to load redirected asset: $redirectedPath")
            }
        }

        // 2. Direct lookup for the requested file
        var uri = cache[normalizedPath]
        var isEncrypted = false

        // 3. If file not found, search for encrypted variants
        if (uri == null && encryptionKey != null) {
            val ext = normalizedPath.substringAfterLast('.', "")
            val base = normalizedPath.substringBeforeLast('.')
            
            encryptedExtensions[ext]?.forEach { encExt ->
                val encPath = "$base.$encExt"
                cache[encPath]?.let {
                    uri = it
                    isEncrypted = true
                    return@forEach
                }
            }
        }

        if (uri == null) return null
        
        return try {
            val stream = context.contentResolver.openInputStream(uri!!) ?: return null
            
            when {
                normalizedPath.endsWith("index.html") -> {
                    val hardenedStream = interceptHtmlAndInjectPolyfills(stream)
                    WebResourceResponse("text/html", "UTF-8", hardenedStream)
                }
                isEncrypted && encryptionKey != null -> {
                    val decryptedStream = decryptStream(stream, encryptionKey)
                    WebResourceResponse(getMimeType(normalizedPath), null, decryptedStream)
                }
                else -> {
                    WebResourceResponse(getMimeType(normalizedPath), null, stream)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Performs on-the-fly XOR decryption for RPG Maker assets.
     */
    private fun decryptStream(inputStream: InputStream, hexKey: String): InputStream {
        val key = hexToBytes(hexKey)
        val headerLength = 16
        val xorLength = 16
        
        return try {
            val rawData = inputStream.readBytes()
            if (rawData.size < headerLength + xorLength) return ByteArrayInputStream(rawData)
            
            val decryptedData = rawData.copyOfRange(headerLength, rawData.size)
            for (i in 0 until xorLength) {
                decryptedData[i] = (decryptedData[i].toInt() xor key[i % key.size].toInt()).toByte()
            }
            ByteArrayInputStream(decryptedData)
        } catch (e: Exception) {
            inputStream
        }
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun interceptHtmlAndInjectPolyfills(inputStream: InputStream): InputStream {
        val rawHtml = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        
        val bootstrapper = """
            <script>
                window.__nexus = { platform: 'android', version: '1.2', hardened: true };
                window.addEventListener('unhandledrejection', function(event) {
                    if(window.NexusNative) NexusNative.reportCrash("Async: " + event.reason);
                });
                window.require = function(mod) {
                    if(['fs', 'path', 'nw.gui', 'crypto'].includes(mod)) {
                        return window.NexusNative ? window.NexusNative.requireMock(mod) : {};
                    }
                    return {};
                };
                console.log("[Nexus] Engine Hardening Injected Early");
            </script>
        """.trimIndent()

        var resultHtml = rawHtml.replaceFirst("<head>", "<head>\n$bootstrapper", ignoreCase = true)

        if (hasTranslation) {
            try {
                val pluginJs = context.assets.open("nexus/NexusTranslationPlugin.js").bufferedReader().use { it.readText() }
                val translationInjection = "<script>\n$pluginJs\n</script>"
                resultHtml = resultHtml.replaceFirst("</script>", "</script>\n$translationInjection", ignoreCase = true)
                Log.d("NexusVFS", "NexusTranslation Engine Injected")
            } catch (e: Exception) {
                Log.e("NexusVFS", "Failed to inject translation plugin", e)
            }
        }

        return ByteArrayInputStream(resultHtml.toByteArray(StandardCharsets.UTF_8))
    }

    suspend fun readAssetAsString(gameId: String, path: String, cache: Map<String, Uri>): String? {
        val uri = cache[path.trimStart('/').lowercase()] ?: return null
        return try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun writeAssetString(gameId: String, path: String, data: String, cache: Map<String, Uri>) {
        val uri = cache[path.trimStart('/').lowercase()] ?: return
        try {
            context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(data) }
        } catch (e: Exception) {
            Log.e("NexusVFS", "Failed to write asset: $path", e)
        }
    }

    suspend fun assetExists(gameId: String, path: String, cache: Map<String, Uri>): Boolean {
        return cache.containsKey(path.trimStart('/').lowercase())
    }

    private fun getMimeType(path: String): String = when {
        path.endsWith(".js") -> "application/javascript"
        path.endsWith(".json") -> "application/json"
        path.endsWith(".png") -> "image/png"
        path.endsWith(".ogg") -> "audio/ogg"
        path.endsWith(".m4a") -> "audio/mp4"
        path.endsWith(".css") -> "text/css"
        path.endsWith(".wasm") -> "application/wasm"
        else -> "application/octet-stream"
    }

    /**
     * Optimized directory indexing with SQLite persistence.
     * Before: 8-15s (recursive SAF walk)
     * After: <0.5s (warm boot SQL bulk load)
     */
    suspend fun indexDirectory(gameId: String, root: Uri, cache: MutableMap<String, Uri>) {
        cache.clear()
        hasTranslation = false
        val gamePath = root.toString()
        
        val rootDoc = DocumentFile.fromTreeUri(context, root) ?: return
        if (rootDoc.findFile("translations.json") != null || rootDoc.findFile("translations.nexus.json") != null) {
            hasTranslation = true
        }

        val dataDir = rootDoc.findFile("data") ?: rootDoc.findFile("www")?.findFile("data")
        val systemJson = dataDir?.findFile("System.json")
        val currentMtime = systemJson?.lastModified() ?: 0L
        
        if (vfsIndex.isIndexValid(gamePath, currentMtime)) {
            if (vfsIndex.loadIndexIntoCache(gamePath, cache)) {
                Log.d("NexusVFS", "Warm boot: Index loaded from SQLite (${cache.size} files)")
                return
            }
        }

        Log.d("NexusVFS", "Cold boot: Building new index for $gamePath")
        val entries = mutableListOf<VfsIndexEntry>()
        buildCacheRecursively("", rootDoc, cache, entries, gamePath)
        vfsIndex.saveIndex(entries)
    }

    private fun buildCacheRecursively(
        path: String, 
        directory: DocumentFile, 
        cache: MutableMap<String, Uri>,
        entries: MutableList<VfsIndexEntry>,
        gamePath: String
    ) {
        directory.listFiles().forEach { file ->
            val name = file.name ?: return@forEach
            val fullPath = if (path.isEmpty()) name else "$path/$name"
            if (file.isDirectory) {
                buildCacheRecursively(fullPath, file, cache, entries, gamePath)
            } else {
                val lowercasePath = fullPath.lowercase()
                cache[lowercasePath] = file.uri
                entries.add(VfsIndexEntry(
                    gamePath = gamePath,
                    relativePath = lowercasePath,
                    realPath = file.uri.toString(),
                    fileSize = file.length(),
                    lastModified = file.lastModified(),
                    isEncrypted = if (isEncryptedExt(name)) 1 else 0,
                    priorityTier = getPriorityTier(fullPath)
                ))
            }
        }
    }

    private fun isEncryptedExt(name: String): Boolean {
        return name.endsWith(".rpgmvp") || name.endsWith(".rpgmvo") || 
               name.endsWith(".rpgmzp") || name.endsWith(".rpgmzo") ||
               name.endsWith(".png_") || name.endsWith(".ogg_")
    }

    private fun getPriorityTier(path: String): Int {
        val p = path.lowercase()
        return when {
            p.endsWith("system.json") || p.endsWith("index.html") || 
            p.endsWith("package.json") || p.contains("rpg_core.js") || 
            p.contains("rpg_managers.js") -> 0
            p.contains("js/") || p.contains("data/") || p.contains("img/system/") -> 1
            else -> 2
        }
    }

    suspend fun clearVfsIndex(gameId: String) {
        vfsIndex.clearIndex(gameId)
    }
}
