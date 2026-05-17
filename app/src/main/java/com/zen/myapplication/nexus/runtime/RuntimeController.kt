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
import com.zen.myapplication.BuildConfig
import com.zen.myapplication.nexus.core.bridge.NexusBridge
import com.zen.myapplication.nexus.core.engine.EngineResolver
import com.zen.myapplication.nexus.core.engine.NativeEngineBridge
import com.zen.myapplication.nexus.core.input.NexusInput
import com.zen.myapplication.nexus.core.storage.SafManager
import com.zen.myapplication.nexus.core.vfs.NexusVFS
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

sealed class RuntimeState {
    object Idle : RuntimeState()
    
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

    private var activeSessionId: String? = null

    private var assetLoader: WebViewAssetLoader? = null
    private var webBridge: NexusBridge? = null
    private var focusRequest: AudioFocusRequest? = null
    val vfsCache = mutableMapOf<String, Uri>()
    
    var input: NexusInput? = null
        private set

    init {
        Log.e("NEXUS_VM", "RuntimeController INIT hash=${hashCode()}")
        Log.e("NEXUS_LIFECYCLE", "RuntimeController Initialized")
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

    private fun updateState(newState: RuntimeState, sessionId: String? = null) {
        if (sessionId != null && sessionId != activeSessionId) {
            Log.w("NEXUS_SESSION", "STALE STATE TRANSITION BLOCKED: Session $sessionId is dead.")
            return
        }
        val oldState = _state.value
        if (oldState != newState) {
            Log.e("NEXUS_STATE", "TRANSITION: ${oldState::class.java.simpleName} -> ${newState::class.java.simpleName}")
            _state.value = newState
        }
    }

    fun getOrCreateWebView(activityContext: Context): WebView {
        Log.e("NEXUS_VM", "getOrCreateWebView: instance=${webViewInstance?.hashCode()}")
        if (webViewInstance == null) {
            Log.e("NEXUS_LIFECYCLE", "Creating persistent WebView instance")
            webViewInstance = WebView(activityContext).apply {
                setBackgroundColor(android.graphics.Color.BLACK) 
                
                // RESTORE HARDWARE ACCELERATION
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, true)
                }
                overScrollMode = View.OVER_SCROLL_NEVER
                isHapticFeedbackEnabled = false
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadsImagesAutomatically = true
                    mediaPlaybackRequiresUserGesture = false
                    allowFileAccess = true
                    allowContentAccess = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                }
                if (BuildConfig.DEBUG) {
                    WebView.setWebContentsDebuggingEnabled(true)
                }
            }
        }
        Log.e("NEXUS_VM", "WebView hash=${webViewInstance?.hashCode()}")
        return webViewInstance!!
    }

    fun startHtml5Game(activityContext: Context, rootUri: Uri) {
        val gameId = rootUri.toString()
        val currentState = _state.value
        
        if (currentState is RuntimeState.Running && currentState.gameId == gameId) {
            Log.e("NEXUS", "Runtime reused for $gameId")
            Log.e("NEXUS", "Session persisted: $activeSessionId")
            return
        }
        
        // If another game is running, stop it first
        if (currentState !is RuntimeState.Idle) {
            if ((currentState as? RuntimeState.Running)?.gameId == gameId) {
                 Log.e("NEXUS", "Runtime reused (session recovery) for $gameId")
                 return
            }
            Log.w("NEXUS", "Different session found. Stopping previous game.")
            stopGame()
        }

        val sessionId = UUID.randomUUID().toString()
        activeSessionId = sessionId
        Log.e("NEXUS_SESSION", "SESSION START: $sessionId")
        
        updateState(RuntimeState.Booting.Initializing, sessionId)
        
        val webView = getOrCreateWebView(activityContext)
        
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.requestFocus()
        webView.requestFocusFromTouch()

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                val message = msg.message()
                val tag = when {
                    message.contains("[NEXUS_RENDER]") || 
                    message.contains("[NEXUS_RENDER_FIX]") || 
                    message.contains("[NEXUS_RENDER_REPORT]") || 
                    message.contains("[NEXUS_RESIZE_TRACE]") ||
                    message.contains("[NEXUS_BOOT]") ||
                    message.contains("[NEXUS_RENDER_VERIFY]") -> "NEXUS_RENDER"
                    message.contains("[NEXUS_INPUT]") ||
                    message.contains("[NEXUS_INPUT_AUDIT]") -> "NEXUS_INPUT"
                    else -> "NEXUS_JS"
                }
                val level = when (msg.messageLevel()) {
                    ConsoleMessage.MessageLevel.ERROR -> "ERROR"
                    ConsoleMessage.MessageLevel.WARNING -> "WARN"
                    else -> "DEBUG"
                }
                Log.e(tag, "S[$sessionId] [$level] $message (${msg.sourceId()}:${msg.lineNumber()})")
                return true
            }
        }

        requestGameAudioFocus()
        webBridge = NexusBridge(webView, vfs, gameId, vfsCache)

        Log.e("NEXUS_INPUT", "Initializing input bridge")
        input = NexusInput { keyCode, isPressed ->
            val script = input?.getWebInjectionScript(keyCode, isPressed)
            if (script != null) {
                Log.e("NEXUS_INPUT", "JS injected: $script")
                webView.post { webView.evaluateJavascript(script, null) }
            }
        }

        viewModelScope.launch(Dispatchers.Main) {
            try {
                if (activeSessionId != sessionId) return@launch

                Log.e("NEXUS_BOOT", "S[$sessionId] STEP 1: Resolving Engine...")
                updateState(RuntimeState.Booting.ResolvingEngine, sessionId)
                val config = engineResolver.resolve(rootUri)

                if (activeSessionId != sessionId) return@launch

                Log.e("NEXUS_BOOT", "S[$sessionId] STEP 2: Mounting VFS...")
                updateState(RuntimeState.Booting.MountingVFS, sessionId)
                
                val mountStart = System.currentTimeMillis()
                withContext(Dispatchers.IO) {
                    vfs.indexDirectory(gameId, rootUri, vfsCache)
                }
                
                if (activeSessionId != sessionId) {
                    Log.w("NEXUS_SESSION", "S[$sessionId] STALE SESSION ABORTED during VFS mount.")
                    return@launch
                }

                Log.e("NEXUS_BOOT", "S[$sessionId] STEP 2 COMPLETE: VFS Mounted in ${System.currentTimeMillis() - mountStart}ms")

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
                    override fun onPageStarted(v: WebView?, u: String?, f: android.graphics.Bitmap?) {
                        super.onPageStarted(v, u, f)
                    }
                    override fun onPageFinished(v: WebView?, u: String?) {
                        if (activeSessionId != sessionId) return
                        
                        // Verify Input Bridge
                        webView.evaluateJavascript("typeof window.__nexus.input") { res -> Log.e("NEXUS_INPUT", "window.__nexus.input type: $res") }
                        webView.evaluateJavascript("typeof window.__nexus.input.dispatch") { res -> Log.e("NEXUS_INPUT", "window.__nexus.input.dispatch type: $res") }
                        webView.evaluateJavascript("!!window.Input") { res -> Log.e("NEXUS_INPUT", "window.Input exists: $res") }

                        webBridge?.injectPolyfills()
                        updateState(RuntimeState.Running(gameId, isNative = false), sessionId)
                    }
                    override fun onReceivedError(v: WebView?, r: WebResourceRequest?, e: WebResourceError?) {
                        super.onReceivedError(v, r, e)
                        Log.e("NEXUS_DEBUG", "S[$sessionId] LOAD ERROR: ${e?.description} URL: ${r?.url}")
                    }
                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                        val isCrash = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            detail?.didCrash() ?: false
                        } else true
                        updateState(RuntimeState.Error("Renderer Process Lost (Crash: $isCrash)"), sessionId)
                        return true
                    }
                }

                Log.e("NEXUS_BOOT", "S[$sessionId] STEP 3: Finalizing...")
                updateState(RuntimeState.Booting.Finalizing, sessionId)
                
                val startPath = if (config.webRoot.isEmpty()) config.mainHtml else "${config.webRoot}/${config.mainHtml}"
                val finalUrl = "https://nexus.local/$startPath"
                
                Log.e("NEXUS_WEB", "INDEX LOAD START: $finalUrl")
                webView.loadUrl(finalUrl)
                
            } catch (c: CancellationException) {
                Log.e("NEXUS_SESSION", "S[$sessionId] Coroutine Cancelled")
            } catch (t: Throwable) {
                Log.e("NEXUS_FATAL", "S[$sessionId] BOOT CRASH", t)
                updateState(RuntimeState.Error("Boot Crash: ${t.message}"), sessionId)
            }
        }
    }


    private fun requestGameAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes).setAcceptsDelayedFocusGain(true).setOnAudioFocusChangeListener(audioFocusChangeListener).build()
            focusRequest?.let { audioManager.requestAudioFocus(it) }
        }
    }

    fun startNativeGame(rootUri: Uri, engineType: SafManager.GameEngine) {
        val gameId = rootUri.toString()
        val currentState = _state.value
        
        if (currentState is RuntimeState.Running && currentState.gameId == gameId && currentState.isNative) {
            Log.e("NEXUS_SESSION", "Native session active for $gameId. Reusing existing runtime.")
            return
        }

        val sessionId = UUID.randomUUID().toString()
        activeSessionId = sessionId
        Log.e("NEXUS_SESSION", "NATIVE SESSION START: $sessionId")

        if (currentState !is RuntimeState.Idle) {
            if ((currentState as? RuntimeState.Running)?.gameId == gameId) {
                return
            }
            Log.w("NEXUS_SESSION", "Different session found. Stopping previous game.")
            stopGame()
        }
        
        updateState(RuntimeState.Booting.Initializing, sessionId)
        
        // Use the new PC-style key dispatcher
        input = NexusInput { keyCode, isPressed -> 
            nativeBridge.sendKeyEvent(keyCode, isPressed) 
        }
        viewModelScope.launch(Dispatchers.IO) {
            if (activeSessionId != sessionId) return@launch
            updateState(RuntimeState.Booting.MountingVFS, sessionId)
            vfs.indexDirectory(gameId, rootUri, vfsCache)
            
            if (activeSessionId != sessionId) return@launch
            updateState(RuntimeState.Booting.ResolvingEngine, sessionId)
            val success = nativeBridge.initEngine(engineType.name, gameId)
            withContext(Dispatchers.Main) {
                if (activeSessionId != sessionId) return@withContext
                if (success) updateState(RuntimeState.Running(gameId, isNative = true), sessionId)
                else updateState(RuntimeState.Error("Native engine failed to initialize"), sessionId)
            }
        }
    }

    fun onSurfaceCreated(surface: Any) { nativeBridge.surfaceCreated(surface) }
    fun onSurfaceChanged(width: Int, height: Int) { nativeBridge.surfaceChanged(width, height) }
    fun onSurfaceDestroyed() { nativeBridge.surfaceDestroyed() }

    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            webViewInstance?.post { webViewInstance?.evaluateJavascript("if(typeof ImageManager !== 'undefined' && ImageManager.clear) { ImageManager.clear(); }", null) }
        }
    }

    private fun executeJs(script: String) {
        webViewInstance?.post { webViewInstance?.evaluateJavascript(script, null) }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {}
    override fun onLowMemory() { onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) }

    fun stopGame() {
        Log.e("NEXUS", "Explicit runtime shutdown")
        Log.e("NEXUS_LIFECYCLE", "stopGame() called")
        activeSessionId = null // Invalidate session
        Log.e("NEXUS_SESSION", "SESSION INVALIDATED")
        
        input?.reset()
        input = null
        
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
        updateState(RuntimeState.Idle)
    }

    override fun onCleared() {
        Log.e("NEXUS_VM", "RuntimeController onCleared: hash=${hashCode()}")
        getApplication<Application>().unregisterComponentCallbacks(this)
        stopGame()
        super.onCleared()
    }
}
