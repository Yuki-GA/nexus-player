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

    private val scriptRedirections = mapOf<String, String>()

    private val encryptedExtensions = mapOf(
        "png" to listOf("rpgmvp", "rpgmzp", "png_"),
        "ogg" to listOf("rpgmvo", "rpgmzo", "ogg_"),
        "m4a" to listOf("rpgmvm", "rpgmzm", "m4a_")
    )

    fun openInterceptedAsset(
        gameId: String, 
        path: String, 
        cache: Map<String, Uri>,
        encryptionKey: String? = null
    ): WebResourceResponse? {
        val normalizedPath = path.trimStart('/').lowercase()
        
        if (normalizedPath.contains("vorbisdecoder")) {
            return WebResourceResponse("text/javascript", "UTF-8", ByteArrayInputStream("".toByteArray()))
        }

        val redirectedPath = scriptRedirections[normalizedPath]
        if (redirectedPath != null) {
            try {
                val assetStream = context.assets.open(redirectedPath)
                return WebResourceResponse(getMimeType(redirectedPath), "UTF-8", assetStream)
            } catch (e: Exception) {
                Log.w("NexusVFS", "Failed to load redirected asset: $redirectedPath")
            }
        }

        var uri = cache[normalizedPath]
        var isEncrypted = false

        if (uri == null && normalizedPath.endsWith(".m4a")) {
            val oggPath = normalizedPath.replace(".m4a", ".ogg")
            cache[oggPath]?.let { uri = it }
        }

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
                    WebResourceResponse(getMimeType(normalizedPath), "UTF-8", decryptedStream)
                }
                else -> {
                    val mime = getMimeType(normalizedPath)
                    val encoding = if (isTextType(normalizedPath)) "UTF-8" else null
                    WebResourceResponse(mime, encoding, stream)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun isTextType(path: String): Boolean {
        val p = path.lowercase()
        return p.endsWith(".js") || p.endsWith(".json") || 
               p.endsWith(".css") || p.endsWith(".html") ||
               p.endsWith(".txt")
    }

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
                (function() {
                    if (window.__Nexus_Engine_Active) return;
                    window.__Nexus_Engine_Active = true;

                    // --- NEXUS LIFECYCLE STABILIZATION v14.0 ---
                    console.error("[NEXUS_BOOT] Lifecycle engine active");

                    // 1. INPUT BRIDGE
                    console.error("[NEXUS_INPUT] Initializing input bridge");
                    window.__nexus = window.__nexus || { platform: 'android', version: '4.0' };
                    window.__nexus.input = {
                        state: {},
                        dispatch: function(keyCode, isPressed) {
                            this.state[keyCode] = isPressed;
                            if (window.Input && Input.keyMapper) {
                                const button = Input.keyMapper[keyCode];
                                if (button) {
                                    Input._currentState[button] = isPressed;
                                    if (isPressed) {
                                        Input._latestButton = button;
                                        Input._pressedTime = 0;
                                        Input._date = Date.now();
                                    }
                                }
                            }
                            const type = isPressed ? 'keydown' : 'keyup';
                            const ev = new KeyboardEvent(type, { keyCode: keyCode, which: keyCode, bubbles: true });
                            window.dispatchEvent(ev);
                        }
                    };

                    // --- NATIVE BRIDGE & POLYFILLS ---
                    const __pathShim = {
                        sep: "/",
                        dirname: function(p) { if (!p) return "."; p = String(p).replace(/\\/g, "/"); const idx = p.lastIndexOf("/"); return idx <= 0 ? "." : p.substring(0, idx); },
                        basename: function(p) { p = String(p).replace(/\\/g, "/"); return p.split("/").pop(); },
                        extname: function(p) { const b = this.basename(p); const i = b.lastIndexOf("."); return i < 0 ? "" : b.substring(i); },
                        normalize: function(p) { return String(p).replace(/\\/g, "/"); },
                        join: function() { return Array.from(arguments).filter(a => !!a).join("/").replace(/\/+/g, "/"); },
                        resolve: function() { return this.normalize(Array.from(arguments).join("/")); }
                    };

                    const __fsShim = {
                        readFileSync: (p) => window.NexusNative ? window.NexusNative.readSaveSync(p) : null,
                        writeFileSync: (p, d) => window.NexusNative ? window.NexusNative.writeSaveSync(p, d) : null,
                        existsSync: (p) => window.NexusNative ? window.NexusNative.existsSaveSync(p) : false,
                        mkdirSync: () => true, readdirSync: () => [], statSync: () => ({ isDirectory: () => false, isFile: () => true })
                    };

                    window.path = __pathShim;
                    window.fs = __fsShim;
                    window.require = function(mod) {
                        if (mod === 'path') return __pathShim;
                        if (mod === 'fs') return __fsShim;
                        if (mod === 'nw.gui') return window.nw || {};
                        return {};
                    };

                    // --- REAL-TIME TELEMETRY BRIDGE ---
                    let _lastFrameTime = performance.now();
                    let _frameCount = 0;
                    function updateFps() {
                        _frameCount++;
                        const now = performance.now();
                        if (now - _lastFrameTime >= 1000) {
                            if (window.NexusNative) NexusNative.updateFps(_frameCount);
                            _frameCount = 0;
                            _lastFrameTime = now;
                        }
                        requestAnimationFrame(updateFps);
                    }
                    updateFps();

                    window.WebAudio = window.WebAudio || {};
                    WebAudio._canPlayOgg = function() { return false; }; 

                    window.onerror = function(msg, url, line, col, error) {
                        const trace = error ? error.stack : "NO_STACK";
                        console.error('[NEXUS_FATAL]', msg, '(' + url + ':' + line + ')', trace);
                        return false;
                    };

                    window.process = { platform: 'win32', env: { NODE_ENV: 'production' }, versions: { node: '14.17.0', v8: '8.4.371.23', nw: '0.50.2' }, mainModule: { filename: 'index.html' }, nextTick: (f) => setTimeout(f, 0) };
                    window.nw = {
                        Window: { get: () => ({ show: () => {}, close: () => NexusNative.exitGame(), isFullscreen: true, focus: () => {} }) },
                        App: { quit: () => NexusNative.exitGame(), argv: [], dataPath: 'nexus://save' }
                    };

                    console.log("[Nexus] Hardened Bootstrapper v14.0 (Stabilized)");
                })();
            </script>
        """.trimIndent()

        var resultHtml = rawHtml.replaceFirst("<head>", "<head>\n$bootstrapper", ignoreCase = true)

        if (hasTranslation) {
            try {
                val pluginJs = context.assets.open("nexus/NexusTranslationPlugin.js").bufferedReader().use { it.readText() }
                resultHtml = resultHtml.replaceFirst("</script>", "</script>\n<script>\n$pluginJs\n</script>", ignoreCase = true)
            } catch (e: Exception) {}
        }

        val milestones = """
            <script>
                (function() {
                    // --- NEXUS SURGICAL STABILIZATION (v14.0) ---
                    
                    if (window.SceneManager) {
                        // 1. RESIZE SUSPENSION
                        const _updateMain = SceneManager.updateMain;
                        let _suspended = false;
                        
                        SceneManager.updateMain = function() {
                            const canvas = Graphics._canvas || (Graphics.app ? Graphics.app.view : null);
                            const collapsed = !window.Graphics || 
                                              Graphics.width <= 0 || 
                                              Graphics.height <= 0 || 
                                              (canvas && (canvas.clientWidth <= 0 || canvas.clientHeight <= 0));
                            
                            if (collapsed) {
                                if (!_suspended) {
                                    console.error("[NEXUS_BOOT_GATE] LOOP_SUSPENDED");
                                    _suspended = true;
                                }
                                return; // Block all updates and rendering
                            }

                            if (_suspended) {
                                console.error("[NEXUS_BOOT_GATE] LOOP_RESUMED");
                                _suspended = false;
                                
                                // 2. RECOVERY PASS
                                setTimeout(() => {
                                    window.focus();
                                    if (canvas) {
                                        canvas.focus();
                                        console.error("[NEXUS_BOOT_GATE] CANVAS_FOCUS_RESTORED");
                                    }
                                    
                                    const scene = SceneManager._scene;
                                    if (scene && scene._tilemap && scene._tilemap.refresh) {
                                        console.error("[NEXUS_BOOT_GATE] TILEMAP_REFRESHED");
                                        scene._tilemap.refresh();
                                    }
                                }, 100);
                            }

                            _updateMain.apply(this, arguments);
                        };

                        // 3. BOOT GATE
                        const _run = SceneManager.run;
                        SceneManager.run = function(sceneClass) {
                            const checkDimensions = () => {
                                const stable = window.innerWidth > 0 && 
                                             window.innerHeight > 0 && 
                                             window.Graphics && 
                                             Graphics.boxWidth > 0;
                                
                                if (stable) {
                                    console.error("[NEXUS_BOOT_GATE] dimensions_stable, starting engine");
                                    _run.call(SceneManager, sceneClass);
                                } else {
                                    requestAnimationFrame(checkDimensions);
                                }
                            };
                            checkDimensions();
                        };
                    }

                    // 4. INPUT HOOKS (RPG Maker Integration)
                    if (window.Input) {
                        const _Input_update = Input.update;
                        Input.update = function() {
                            if (window.__nexus && __nexus.input) {
                                for (const key in __nexus.input.state) {
                                    const pressed = __nexus.input.state[key];
                                    const mapper = Input.keyMapper[key];
                                    if (mapper) Input._currentState[mapper] = pressed;
                                }
                            }
                            _Input_update.call(this);
                        };
                    }

                    const style = document.createElement('style');
                    style.innerHTML = `
                        body, html { margin: 0; padding: 0; overflow: hidden; background: black; width: 100%; height: 100%; }
                        canvas { width: 100vw !important; height: 100vh !important; object-fit: contain !important; image-rendering: pixelated !important; }
                    `;
                    document.head.appendChild(style);

                    console.log('[Nexus] RPGMAKER BOOT OK');
                })();
            </script>
        """.trimIndent()

        resultHtml = resultHtml.replaceFirst("</body>", "$milestones\n</body>", ignoreCase = true)

        return ByteArrayInputStream(resultHtml.toByteArray(StandardCharsets.UTF_8))
    }

    suspend fun readAssetAsString(gameId: String, path: String, cache: Map<String, Uri>): String? {
        val uri = cache[path.trimStart('/').lowercase()] ?: return null
        return try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } catch (e: Exception) { null }
    }

    suspend fun writeAssetString(gameId: String, path: String, data: String, cache: Map<String, Uri>) {
        val uri = cache[path.trimStart('/').lowercase()] ?: return
        try {
            context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(data) }
        } catch (e: Exception) { Log.e("NexusVFS", "Failed to write asset: $path", e) }
    }

    suspend fun assetExists(gameId: String, path: String, cache: Map<String, Uri>): Boolean {
        return cache.containsKey(path.trimStart('/').lowercase())
    }

    private fun getMimeType(path: String): String = when {
        path.endsWith(".js") -> "text/javascript"
        path.endsWith(".json") -> "application/json"
        path.endsWith(".png") -> "image/png"
        path.endsWith(".ogg") -> "audio/ogg"
        path.endsWith(".m4a") -> "audio/mp4"
        path.endsWith(".css") -> "text/css"
        path.endsWith(".wasm") -> "application/wasm"
        else -> "application/octet-stream"
    }

    suspend fun indexDirectory(gameId: String, root: Uri, cache: MutableMap<String, Uri>) {
        Log.e("NEXUS_VFS", "MOUNT START for $gameId")
        val startTime = System.currentTimeMillis()
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
                Log.e("NEXUS_VFS", "MOUNT COMPLETE (Warm Boot) in ${System.currentTimeMillis() - startTime}ms")
                return
            }
        }
        val scanStart = System.currentTimeMillis()
        val entries = mutableListOf<VfsIndexEntry>()
        buildCacheRecursively("", rootDoc, cache, entries, gamePath)
        vfsIndex.saveIndex(entries)
        Log.e("NEXUS_VFS", "MOUNT COMPLETE (Cold Boot) in ${System.currentTimeMillis() - startTime}ms")
    }

    private fun buildCacheRecursively(path: String, directory: DocumentFile, cache: MutableMap<String, Uri>, entries: MutableList<VfsIndexEntry>, gamePath: String) {
        directory.listFiles().forEach { file ->
            val name = file.name ?: return@forEach
            val fullPath = if (path.isEmpty()) name else "$path/$name"
            if (file.isDirectory) {
                buildCacheRecursively(fullPath, file, cache, entries, gamePath)
            } else {
                val lowercasePath = fullPath.lowercase()
                cache[lowercasePath] = file.uri
                entries.add(VfsIndexEntry(gamePath = gamePath, relativePath = lowercasePath, realPath = file.uri.toString(), fileSize = file.length(), lastModified = file.lastModified(), isEncrypted = if (name.endsWith(".rpgmvp") || name.endsWith(".rpgmvo") || name.endsWith(".rpgmzp") || name.endsWith(".rpgmzo") || name.endsWith(".png_") || name.endsWith(".ogg_")) 1 else 0, priorityTier = getPriorityTier(fullPath)))
            }
        }
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

    suspend fun clearVfsIndex(gameId: String) { vfsIndex.clearIndex(gameId) }
}
