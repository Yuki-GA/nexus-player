package com.zen.myapplication.nexus.runtime

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewModelScope
import androidx.webkit.WebViewAssetLoader
import com.zen.myapplication.nexus.core.bridge.NexusBridge
import com.zen.myapplication.nexus.core.engine.EngineResolver
import com.zen.myapplication.nexus.core.engine.NativeEngineBridge
import com.zen.myapplication.nexus.core.input.NexusInput
import com.zen.myapplication.nexus.core.storage.SafManager
import com.zen.myapplication.nexus.core.vfs.NexusVFS
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class RuntimeState {
    object Idle : RuntimeState()
    
    // Console-like Boot Phases
    sealed class Booting : RuntimeState() {
        object Initializing : Booting()
        object MountingVFS : Booting()
        object ResolvingEngine : Booting()
        object Finalizing : Booting()
    }
    
    data class Running(val gameId: String, val isNative: Boolean = false) : RuntimeState()
    data class Error(val message: String, val code: Int = 0, val canRetry: Boolean = true) : RuntimeState()
}

class RuntimeController(application: Application) : AndroidViewModel(application), ComponentCallbacks2 {
    private val context = application.applicationContext
    private val _state = MutableStateFlow<RuntimeState>(RuntimeState.Idle)
    val state: StateFlow<RuntimeState> = _state

    private val vfs = NexusVFS(context)
    private val engineResolver = EngineResolver(context)
    private val nativeBridge = NativeEngineBridge()
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    var webViewInstance: WebView? = null
        private set

    private var assetLoader: WebViewAssetLoader? = null

    private var webBridge: NexusBridge? = null
    private var focusRequest: AudioFocusRequest? = null
    
    // Performance: O(1) Lookup Cache to bypass SAF traversal
    val vfsCache = mutableMapOf<String, Uri>()
    
    var input: NexusInput? = null
        private set

    init {
        application.registerComponentCallbacks(this)
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> 
                executeJs("if(window.WebAudio && WebAudio._context) WebAudio._context.suspend();")
            AudioManager.AUDIOFOCUS_GAIN -> 
                executeJs("if(window.WebAudio && WebAudio._context) WebAudio._context.resume();")
        }
    }

    fun startHtml5Game(webView: WebView, rootUri: Uri) {
        if (_state.value !is RuntimeState.Idle) return
        
        _state.value = RuntimeState.Booting.Initializing
        this.webViewInstance = webView
        val gameId = rootUri.toString()

        webView.apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, true)
            }
            overScrollMode = View.OVER_SCROLL_NEVER
            isHapticFeedbackEnabled = false
            setOnLongClickListener { true }

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true
                cacheMode = WebSettings.LOAD_DEFAULT
            }
        }

        requestGameAudioFocus()
        webBridge = NexusBridge(webView, vfs, gameId, vfsCache)

        viewModelScope.launch(Dispatchers.Main) {
            _state.value = RuntimeState.Booting.ResolvingEngine
            val config = engineResolver.resolve(rootUri)

            _state.value = RuntimeState.Booting.MountingVFS
            withContext(Dispatchers.IO) { vfs.indexDirectory(gameId, rootUri, vfsCache) }

            assetLoader = WebViewAssetLoader.Builder()
                .setDomain("nexus.local")
                .setHttpAllowed(false)
                .addPathHandler("/") { path ->
                    vfs.openInterceptedAsset(gameId, path, vfsCache, config.encryptionKey)
                }
                .build()

            webView.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(v: WebView?, req: WebResourceRequest?): WebResourceResponse? {
                    return assetLoader?.shouldInterceptRequest(req?.url ?: return null)
                }

                override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                    val isCrash = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        detail?.didCrash() ?: false
                    } else {
                        true // Assume crash on older devices
                    }
                    _state.value = RuntimeState.Error(
                        message = if (isCrash) "Engine process crashed." else "Engine killed by OS (OOM).",
                        canRetry = true
                    )
                    return true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    webBridge?.injectPolyfills()
                    _state.value = RuntimeState.Running(gameId, isNative = false)
                }
            }

            _state.value = RuntimeState.Booting.Finalizing
            val startPath = if (config.webRoot.isEmpty()) config.mainHtml else "${config.webRoot}/${config.mainHtml}"
            webView.loadUrl("https://nexus.local/$startPath")
        }
    }

    fun startNativeGame(rootUri: Uri, engineType: SafManager.GameEngine) {
        if (_state.value is RuntimeState.Running) return
        _state.value = RuntimeState.Booting.Initializing
        val gameId = rootUri.toString()
        input = NexusInput { intent, isPressed -> nativeBridge.sendKeyEvent(intent.ordinal, isPressed) }
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = RuntimeState.Booting.MountingVFS
            vfs.indexDirectory(gameId, rootUri, vfsCache)
            _state.value = RuntimeState.Booting.ResolvingEngine
            val success = nativeBridge.initEngine(engineType.name, gameId)
            withContext(Dispatchers.Main) {
                if (success) _state.value = RuntimeState.Running(gameId, isNative = true)
                else _state.value = RuntimeState.Error("Native engine failed to initialize")
            }
        }
    }

    fun onSurfaceCreated(surface: Any) { nativeBridge.surfaceCreated(surface) }
    fun onSurfaceChanged(width: Int, height: Int) { nativeBridge.surfaceChanged(width, height) }
    fun onSurfaceDestroyed() { nativeBridge.surfaceDestroyed() }

    private fun requestGameAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes).setAcceptsDelayedFocusGain(true).setOnAudioFocusChangeListener(audioFocusChangeListener).build()
            focusRequest?.let { audioManager.requestAudioFocus(it) }
        }
    }

    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            executeJs("if(typeof ImageManager !== 'undefined' && ImageManager.clear) { ImageManager.clear(); }")
        }
    }

    private fun executeJs(script: String) {
        webViewInstance?.post { webViewInstance?.evaluateJavascript(script, null) }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {}
    override fun onLowMemory() { onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) }

    fun stopGame() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        }
        webViewInstance?.apply {
            (parent as? ViewGroup)?.removeView(this)
            stopLoading()
            loadUrl("about:blank")
            destroy()
        }
        webViewInstance = null
        _state.value = RuntimeState.Idle
    }

    override fun onCleared() {
        getApplication<Application>().unregisterComponentCallbacks(this)
        stopGame()
        super.onCleared()
    }
}
