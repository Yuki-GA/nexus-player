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
 * Mocks are now handled by the early-boot VFS bootstrapper.
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

    /**
     * DISCONTINUED: Polyfills are now injected early via NexusVFS bootstrapper.
     */
    fun injectPolyfills() {
        Log.d(TAG, "injectPolyfills() called - skipping redundant injection")
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
            webView.evaluateJavascript("if(window.__nexus && window.__nexus.engine) window.__nexus.engine.toggleFps();", null)
        }
    }

    @JavascriptInterface
    fun setResolutionScale(scale: Float) {
        webView.post {
            webView.evaluateJavascript("if(window.__nexus && window.__nexus.engine) window.__nexus.engine.setScale($scale);", null)
        }
    }

    @JavascriptInterface
    fun captureScreenshot() {
        webView.post {
            webView.evaluateJavascript("if(window.__nexus && window.__nexus.engine) window.__nexus.engine.screenshot();") { result ->
                Log.d(TAG, "Screenshot captured (length: ${result?.length})")
            }
        }
    }

    @JavascriptInterface
    fun readSaveSync(path: String): String? = runBlocking { 
        vfs.readAssetAsString(gameId, path, vfsCache) 
    }

    @JavascriptInterface
    fun writeSaveSync(path: String, data: String) {
        runBlocking { vfs.writeAssetString(gameId, path, data, vfsCache) }
    }

    @JavascriptInterface
    fun existsSaveSync(path: String): Boolean = runBlocking { 
        vfs.assetExists(gameId, path, vfsCache) 
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
        return ""
    }

    @JavascriptInterface
    fun reloadTranslation() {
        webView.post { webView.evaluateJavascript("if(window.__nexus && window.__nexus.translation) window.__nexus.translation.reload();", null) }
    }

    @JavascriptInterface
    fun setTranslationEnabled(enabled: Boolean) {
        webView.post { webView.evaluateJavascript("if(window.__nexus && window.__nexus.translation) window.__nexus.translation.setEnabled($enabled);", null) }
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
