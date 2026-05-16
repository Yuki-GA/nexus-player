package com.zen.myapplication.nexus.core.bridge

import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.zen.myapplication.nexus.core.vfs.NexusVFS
import kotlinx.coroutines.runBlocking
import android.net.Uri

/**
 * NexusBridge: The production-grade JS-Native bridge.
 * Mocks a full NW.js environment to support advanced MV/MZ plugins.
 */
class NexusBridge(
    private val webView: WebView,
    private val vfs: NexusVFS,
    private val gameId: String,
    private val vfsCache: Map<String, Uri>
) {
    companion object {
        private const val TAG = "NexusBridge"
        const val INTERFACE_NAME = "NexusNative"
    }

    init {
        webView.addJavascriptInterface(this, INTERFACE_NAME)
    }

    fun injectPolyfills() {
        val script = """
            (function() {
                if (window.__NexusInjected) return;
                window.__NexusInjected = true;

                window.nw = {
                    Window: { get: () => ({ 
                        show: () => {}, close: () => ${INTERFACE_NAME}.exitGame(),
                        isFullscreen: true, enterFullscreen: () => {}, 
                        leaveFullscreen: () => {}, focus: () => {} 
                    }) },
                    App: { quit: () => ${INTERFACE_NAME}.exitGame(), argv: [], dataPath: 'nexus://save' }
                };

                window.process = { platform: 'win32', env: { NODE_ENV: 'production' }, mainModule: { filename: 'index.html' } };
                
                window.require = function(mod) {
                    if (mod === 'fs') return {
                        readFileSync: (path) => ${INTERFACE_NAME}.readSaveSync(path),
                        writeFileSync: (path, data) => ${INTERFACE_NAME}.writeSaveSync(path, data),
                        existsSync: (path) => ${INTERFACE_NAME}.existsSaveSync(path),
                        mkdirSync: () => true,
                        readdirSync: () => [],
                        statSync: () => ({ isDirectory: () => false, isFile: () => true })
                    };
                    if (mod === 'path') return {
                        join: (...args) => args.join('/').replace(/\/+/g, '/'),
                        sep: '/'
                    };
                    return {};
                };

                if (window.SceneManager) {
                    SceneManager.catchException = (e) => {
                        console.error(e);
                        ${INTERFACE_NAME}.reportCrash(e.message);
                    };
                }

                // 4. Nexus Engine Controls
                window.__nexus.engine = {
                    toggleFps: () => {
                        // RPG Maker F2 simulation
                        const event = new KeyboardEvent('keydown', { keyCode: 113, key: 'F2' });
                        window.dispatchEvent(event);
                    },
                    setScale: (scale) => {
                        document.body.style.zoom = scale;
                        window.dispatchEvent(new Event('resize'));
                    },
                    screenshot: () => {
                        const canvas = document.getElementById('upperCanvas') || document.getElementsByTagName('canvas')[0];
                        return canvas ? canvas.toDataURL('image/png') : null;
                    }
                };
                
                console.log("[Nexus] Engine Polyfills Active");
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    @JavascriptInterface
    fun sendKeyEvent(keyCode: Int, action: String) {
        val type = if (action == "down") "keydown" else "keyup"
        webView.post {
            webView.evaluateJavascript("""
                (function() {
                    const event = new KeyboardEvent('$type', { 
                        keyCode: $keyCode, 
                        which: $keyCode,
                        bubbles: true 
                    });
                    window.dispatchEvent(event);
                })();
            """.trimIndent(), null)
        }
    }

    @JavascriptInterface
    fun toggleFpsDisplay() {
        webView.post {
            webView.evaluateJavascript("if(window.__nexus.engine) window.__nexus.engine.toggleFps();", null)
        }
    }

    @JavascriptInterface
    fun setResolutionScale(scale: Float) {
        webView.post {
            webView.evaluateJavascript("if(window.__nexus.engine) window.__nexus.engine.setScale($scale);", null)
        }
    }

    @JavascriptInterface
    fun captureScreenshot() {
        webView.post {
            webView.evaluateJavascript("if(window.__nexus.engine) window.__nexus.engine.screenshot();") { result ->
                // In a real app we'd save the base64 string to storage
                Log.d(TAG, "Screenshot captured (length: ${result?.length})")
            }
        }
    }

    @JavascriptInterface
    fun readSaveSync(path: String): String? {
        return runBlocking { vfs.readAssetAsString(gameId, path, vfsCache) }
    }

    @JavascriptInterface
    fun writeSaveSync(path: String, data: String) {
        runBlocking { vfs.writeAssetString(gameId, path, data, vfsCache) }
    }

    @JavascriptInterface
    fun existsSaveSync(path: String): Boolean {
        return runBlocking { vfs.assetExists(gameId, path, vfsCache) }
    }

    @JavascriptInterface
    fun reportCrash(msg: String) {
        Log.e(TAG, "Game Crash Intercepted: $msg")
    }

    @JavascriptInterface
    fun clearVfsIndex() {
        runBlocking { vfs.clearVfsIndex(gameId) }
    }

    @JavascriptInterface
    fun getTranslationStatus(): String {
        return "" // In a real app we'd fetch this from window.__nexus.translation
    }

    @JavascriptInterface
    fun reloadTranslation() {
        webView.post { webView.evaluateJavascript("if(window.__nexus.translation) window.__nexus.translation.reload();", null) }
    }

    @JavascriptInterface
    fun setTranslationEnabled(enabled: Boolean) {
        webView.post { webView.evaluateJavascript("if(window.__nexus.translation) window.__nexus.translation.setEnabled($enabled);", null) }
    }

    @JavascriptInterface
    fun requireMock(mod: String): String {
        return "{}"
    }

    @JavascriptInterface
    fun exitGame() {
        webView.post { (webView.context as? android.app.Activity)?.finish() }
    }

    // Legacy support
    @JavascriptInterface
    fun readFileSync(path: String): String? {
        return readSaveSync(path)
    }

    @JavascriptInterface
    fun existsSync(path: String): Boolean {
        return existsSaveSync(path)
    }
}
