package com.zen.myapplication.nexus.core.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets

/**
 * Manages Storage Access Framework (SAF) interactions.
 * Handles selecting a game directory, persisting URI permissions,
 * and detecting the game engine based on file structures.
 */
class SafManager(private val context: Context) {

    enum class GameEngine {
        RPG_MAKER_MV,
        RPG_MAKER_MZ,
        RENPY_WEB,
        RENPY,
        RPG_MAKER_XP_VX_ACE,
        TYRANOBUILDER,
        CONSTRUCT,
        UNITY_WEBGL,
        GODOT_HTML5,
        TWINE,
        BITSY,
        FLASH,
        HTML5_CUSTOM,
        UNSUPPORTED
    }

    /**
     * Call this after receiving the Uri from ACTION_OPEN_DOCUMENT_TREE.
     */
    fun takePersistableUriPermission(uri: Uri) {
        val takeFlags: Int = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, takeFlags)
    }

    /**
     * Scans the selected SAF directory to detect the game engine.
     */
    suspend fun detectEngine(treeUri: Uri): GameEngine = withContext(Dispatchers.IO) {
        val rootDir = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext GameEngine.UNSUPPORTED

        val wwwDir = rootDir.findFileIgnoreCase("www")?.takeIf { it.isDirectory }
        val webRoots = listOfNotNull(rootDir, wwwDir).distinctBy { it.uri.toString() }
        val hasIndexHtml = webRoots.any { it.findFileIgnoreCase("index.html")?.isFile == true }

        if (hasIndexHtml && webRoots.any { it.looksLikeRenpyWebExport() }) {
            return@withContext GameEngine.RENPY_WEB
        }

        // --- FLASH DETECTION ---
        if (!hasIndexHtml && rootDir.anyFileMatching(maxDepth = 3) { name ->
                name.endsWith(".swf", ignoreCase = true)
            }
        ) {
            return@withContext GameEngine.FLASH
        }

        // --- REN'PY DETECTION ---
        val gameFolder = rootDir.findFileIgnoreCase("game")
        val hasRenpyFolder = rootDir.findFileIgnoreCase("renpy") != null
        val hasRenpyScript = gameFolder?.anyFileMatching(maxDepth = 1) { name ->
            name.endsWith(".rpy", ignoreCase = true) ||
                    name.endsWith(".rpyc", ignoreCase = true)
        } == true
        if (hasRenpyFolder || hasRenpyScript) {
            return@withContext GameEngine.RENPY
        }

        // --- RPG MAKER XP/VX/VX ACE DETECTION ---
        val hasGameIni = rootDir.findFileIgnoreCase("Game.ini") != null
        val hasRgssArchive = rootDir.findFileIgnoreCase("Game.rgss3a") != null ||
                rootDir.findFileIgnoreCase("Game.rgss2a") != null ||
                rootDir.findFileIgnoreCase("Game.rgssad") != null
        val dataDir = rootDir.findFileIgnoreCase("Data")
        val hasRgssData = dataDir?.anyFileMatching(maxDepth = 1) { name ->
            name.endsWith(".rxdata", ignoreCase = true) ||
                    name.endsWith(".rvdata", ignoreCase = true) ||
                    name.endsWith(".rvdata2", ignoreCase = true)
        } == true
        if (hasGameIni || hasRgssArchive || hasRgssData) {
            return@withContext GameEngine.RPG_MAKER_XP_VX_ACE
        }

        // --- HTML5 BASED ENGINES (MV/MZ, Tyrano, Construct, Custom) ---
        if (hasIndexHtml || rootDir.findFileIgnoreCase("package.json") != null || wwwDir != null) {
            for (baseDir in webRoots) {
                detectHtmlEngine(baseDir)?.let { engine ->
                    return@withContext engine
                }
            }

            return@withContext GameEngine.HTML5_CUSTOM
        }

        return@withContext GameEngine.UNSUPPORTED
    }

    /**
     * Recursively indexes all files in the game directory into the VFS database.
     */
    suspend fun indexGameAssets(treeUri: Uri, onProgress: (Int) -> Unit = {}): Unit = withContext(Dispatchers.IO) {
        val db = VfsDatabase.getDatabase(context)
        val dao = db.assetDao()
        val rootDir = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext
        val gameId = treeUri.toString()

        dao.clearGameAssets(gameId)

        val assetBuffer = mutableListOf<AssetEntry>()
        var totalIndexed = 0

        fun walk(dir: DocumentFile, relativePath: String) {
            val files = dir.listFiles()
            for (file in files) {
                val name = file.name ?: continue
                val newRelativePath = if (relativePath.isEmpty()) name else "$relativePath/$name"

                if (file.isDirectory) {
                    walk(file, newRelativePath)
                } else if (file.isFile) {
                    assetBuffer.add(
                        AssetEntry(
                            gameId = gameId,
                            relativePath = newRelativePath.lowercase(),
                            assetUri = file.uri.toString(),
                            lastModified = file.lastModified()
                        )
                    )
                    totalIndexed++

                    if (assetBuffer.size >= 100) {
                        val batch = assetBuffer.toList()
                        assetBuffer.clear()
                        kotlinx.coroutines.runBlocking { dao.insertAssets(batch) }
                        onProgress(totalIndexed)
                    }
                }
            }
        }

        walk(rootDir, "")
        if (assetBuffer.isNotEmpty()) {
            dao.insertAssets(assetBuffer)
            onProgress(totalIndexed)
        }
    }

    suspend fun clearVfs(treeUri: Uri) = withContext(Dispatchers.IO) {
        VfsDatabase.getDatabase(context).assetDao().clearGameAssets(treeUri.toString())
    }

    suspend fun getVfsAssetCount(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        VfsDatabase.getDatabase(context).assetDao().getAssetCount(treeUri.toString())
    }

    private fun detectHtmlEngine(baseDir: DocumentFile): GameEngine? {
        val jsDir = baseDir.findFileIgnoreCase("js")
        if (jsDir != null) {
            if (jsDir.findFileIgnoreCase("rmmz_core.js") != null) return GameEngine.RPG_MAKER_MZ
            if (jsDir.findFileIgnoreCase("rpg_core.js") != null) return GameEngine.RPG_MAKER_MV
        }

        if (baseDir.looksLikeUnityWebGl()) return GameEngine.UNITY_WEBGL
        if (baseDir.looksLikeGodotHtml5()) return GameEngine.GODOT_HTML5

        if (baseDir.findFileIgnoreCase("tyrano") != null ||
            baseDir.findFileIgnoreCase("data")?.findFileIgnoreCase("scenario") != null
        ) {
            return GameEngine.TYRANOBUILDER
        }

        if (baseDir.looksLikeConstructExport()) return GameEngine.CONSTRUCT

        val indexPrefix = baseDir.findFileIgnoreCase("index.html")?.readTextPrefix().orEmpty()
        val normalizedIndex = indexPrefix.lowercase()
        if (normalizedIndex.contains("<tw-storydata") ||
            normalizedIndex.contains("sugarcube") ||
            normalizedIndex.contains("harlowe") ||
            normalizedIndex.contains("chapbook")
        ) {
            return GameEngine.TWINE
        }

        if (baseDir.findFileIgnoreCase("bitsy.js") != null ||
            (normalizedIndex.contains("bitsy") && normalizedIndex.contains("exportedgamedata"))
        ) {
            return GameEngine.BITSY
        }

        return null
    }

    private fun DocumentFile.findFileIgnoreCase(name: String): DocumentFile? {
        return findFile(name) ?: listFiles().firstOrNull { file ->
            file.name.equals(name, ignoreCase = true)
        }
    }

    private fun DocumentFile.anyFileMatching(
        maxDepth: Int,
        predicate: (String) -> Boolean
    ): Boolean {
        if (maxDepth < 0 || !isDirectory) return false

        for (file in listFiles()) {
            val name = file.name.orEmpty()
            if (file.isFile && predicate(name)) return true
            if (file.isDirectory && file.anyFileMatching(maxDepth - 1, predicate)) return true
        }

        return false
    }

    private fun DocumentFile.looksLikeConstructExport(): Boolean {
        val scriptsDir = findFileIgnoreCase("scripts")
        val hasConstructRuntime = findFileIgnoreCase("c2runtime.js") != null ||
                findFileIgnoreCase("c3runtime.js") != null ||
                scriptsDir?.findFileIgnoreCase("c3runtime.js") != null
        val hasConstructData = findFileIgnoreCase("data.js") != null ||
                findFileIgnoreCase("offline.js") != null ||
                findFileIgnoreCase("offlineClient.js") != null

        return hasConstructRuntime || hasConstructData
    }

    private fun DocumentFile.looksLikeUnityWebGl(): Boolean {
        val buildDir = findFileIgnoreCase("Build")
        val files = buildDir?.listFiles().orEmpty()
        val hasLoader = files.any { file ->
            file.name.orEmpty().endsWith(".loader.js", ignoreCase = true)
        }
        val hasFramework = files.any { file ->
            val name = file.name.orEmpty()
            name.endsWith(".framework.js", ignoreCase = true) ||
                    name.endsWith(".framework.js.gz", ignoreCase = true) ||
                    name.endsWith(".framework.js.br", ignoreCase = true) ||
                    name.endsWith(".framework.js.unityweb", ignoreCase = true)
        }
        val hasData = files.any { file ->
            val name = file.name.orEmpty()
            name.endsWith(".data", ignoreCase = true) ||
                    name.endsWith(".data.gz", ignoreCase = true) ||
                    name.endsWith(".data.br", ignoreCase = true) ||
                    name.endsWith(".data.unityweb", ignoreCase = true)
        }
        val hasWasm = files.any { file ->
            val name = file.name.orEmpty()
            name.endsWith(".wasm", ignoreCase = true) ||
                    name.endsWith(".wasm.gz", ignoreCase = true) ||
                    name.endsWith(".wasm.br", ignoreCase = true) ||
                    name.endsWith(".wasm.unityweb", ignoreCase = true)
        }

        return hasLoader || (hasFramework && hasData && hasWasm)
    }

    private fun DocumentFile.looksLikeGodotHtml5(): Boolean {
        val files = listFiles()
        val hasPck = files.any { file ->
            file.name.orEmpty().endsWith(".pck", ignoreCase = true)
        }
        val hasGodotRuntime = files.any { file ->
            val name = file.name.orEmpty()
            name.contains("godot", ignoreCase = true) &&
                    (name.endsWith(".js", ignoreCase = true) ||
                            name.endsWith(".wasm", ignoreCase = true))
        }
        val indexPrefix = findFileIgnoreCase("index.html")?.readTextPrefix().orEmpty()
        val indexMentionsGodot = indexPrefix.contains("godot", ignoreCase = true) ||
                indexPrefix.contains("Engine.load", ignoreCase = true)

        return hasPck && (hasGodotRuntime || indexMentionsGodot)
    }

    private fun DocumentFile.looksLikeRenpyWebExport(): Boolean {
        val files = listFiles()
        return files.any { file ->
            val name = file.name.orEmpty().lowercase()
            name == "game.zip" ||
                    name == "game.data" ||
                    name == "game.data.gz" ||
                    name == "index.wasm" ||
                    name == "index.wasm.gz" ||
                    name == "renpy-pre.js" ||
                    name == "renpy-pre.js.gz"
        }
    }

    private fun DocumentFile.readTextPrefix(maxBytes: Int = 256 * 1024): String? {
        if (!isFile) return null

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(maxBytes)
                val bytesRead = input.read(buffer)
                if (bytesRead <= 0) "" else String(buffer, 0, bytesRead, StandardCharsets.UTF_8)
            }
        } catch (_: Exception) {
            null
        }
    }
}
