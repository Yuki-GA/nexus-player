package com.zen.myapplication.nexus.ui.player

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.documentfile.provider.DocumentFile
import androidx.webkit.WebViewAssetLoader
import com.zen.myapplication.nexus.core.ai.TranslationPipeline
import com.zen.myapplication.nexus.core.engine.LocalGameAssetLoader
import com.zen.myapplication.nexus.core.input.NexusInput
import com.zen.myapplication.nexus.core.settings.SettingsManager
import com.zen.myapplication.nexus.core.storage.SafManager
import com.zen.myapplication.nexus.core.storage.displayName
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Html5RuntimeScreen(
    gameFolderUri: Uri,
    engineType: SafManager.GameEngine,
    translationPipeline: TranslationPipeline = remember { TranslationPipeline() }
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }

    val opacity by settingsManager.controllerOpacity.collectAsState(initial = 0.6f)
    val controllerSize by settingsManager.controllerSize.collectAsState(initial = 1.0f)
    val hardwareAccel by settingsManager.forceHardwareAccel.collectAsState(initial = true)
    val showFps by settingsManager.showFps.collectAsState(initial = false)
    val fpsLimit by settingsManager.fpsLimit.collectAsState(initial = 60)
    val isTranslationEnabled by settingsManager.translationEnabled.collectAsState(initial = true)

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var runtimeMessage by remember(gameFolderUri, engineType) {
        mutableStateOf<String?>("Loading ${engineType.displayName}...")
    }

    val runtimeBridge = remember {
        RuntimeEventBridge { message ->
            runtimeMessage = message
        }
    }
    val latestWebView by rememberUpdatedState(webViewRef)

    LaunchedEffect(isTranslationEnabled) {
        translationPipeline.toggleTranslation(isTranslationEnabled)
    }

    DisposableEffect(Unit) {
        onDispose {
            latestWebView?.apply {
                stopLoading()
                loadUrl("about:blank")
                onPause()
                pauseTimers()
                removeJavascriptInterface("NexusAI")
                removeJavascriptInterface("NexusRuntime")
                removeAllViews()
                destroy()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    resumeTimers()
                    keepScreenOn = true
                    overScrollMode = View.OVER_SCROLL_NEVER
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    isFocusable = true
                    isFocusableInTouchMode = true
                    importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
                    setOnLongClickListener { true }

                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.setSupportZoom(false)
                    settings.builtInZoomControls = false
                    settings.displayZoomControls = false
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.defaultTextEncodingName = "utf-8"
                    settings.loadsImagesAutomatically = true
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.setSupportMultipleWindows(false)
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.textZoom = 100
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        settings.safeBrowsingEnabled = false
                        setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true)
                    }

                    setLayerType(
                        if (hardwareAccel) View.LAYER_TYPE_HARDWARE else View.LAYER_TYPE_SOFTWARE,
                        null
                    )
                    setBackgroundColor(0xFF000000.toInt())
                    requestFocus()

                    addJavascriptInterface(translationPipeline, "NexusAI")
                    addJavascriptInterface(runtimeBridge, "NexusRuntime")

                    val assetLoader = WebViewAssetLoader.Builder()
                        .setDomain("appassets.androidplatform.net")
                        .addPathHandler("/", LocalGameAssetLoader(context, gameFolderUri))
                        .build()

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            val message = consoleMessage?.message().orEmpty()
                            if (engineType.isRpgMakerWebRuntime &&
                                consoleMessage?.messageLevel() == ConsoleMessage.MessageLevel.ERROR &&
                                message.looksFatalConsoleMessage()
                            ) {
                                runtimeMessage = "Script error: ${message.take(160)}"
                            }
                            return false
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            return request?.url?.let(assetLoader::shouldInterceptRequest)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            view?.requestFocus()
                            view?.let {
                                installControllerInputShim(it)
                                configureRpgMakerRuntime(it, engineType, showFps, fpsLimit)
                            }
                            if (runtimeMessage?.startsWith("Loading") == true) {
                                runtimeMessage = null
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) {
                                runtimeMessage = "Could not load ${engineType.displayName}: ${error?.description ?: "unknown error"}"
                            }
                        }

                        override fun onRenderProcessGone(
                            view: WebView?,
                            detail: RenderProcessGoneDetail?
                        ): Boolean {
                            runtimeMessage = if (detail?.didCrash() == true) {
                                "The WebView renderer crashed. Reopen the game or lower the FPS limit."
                            } else {
                                "The WebView renderer was reclaimed. Reopen the game to continue."
                            }
                            webViewRef = null
                            view?.destroy()
                            return true
                        }
                    }

                    val url = if (engineType == SafManager.GameEngine.FLASH) {
                        val swfPath = findFirstSwfPath(context, gameFolderUri)
                        val encodedSwfPath = URLEncoder.encode(
                            swfPath.orEmpty(),
                            StandardCharsets.UTF_8.toString()
                        )
                        "https://appassets.androidplatform.net/ruffle/ruffle_player.html?swf=$encodedSwfPath"
                    } else {
                        val entryPath = findHtmlEntryPath(context, gameFolderUri)
                        "https://appassets.androidplatform.net$entryPath"
                    }

                    loadUrl(url)
                    webViewRef = this
                }
            },
            update = { webView ->
                webView.setLayerType(
                    if (hardwareAccel) View.LAYER_TYPE_HARDWARE else View.LAYER_TYPE_SOFTWARE,
                    null
                )
                installControllerInputShim(webView)
                configureRpgMakerRuntime(webView, engineType, showFps, fpsLimit)
            },
            modifier = Modifier.fillMaxSize()
        )

        runtimeMessage?.let { message ->
            RuntimeStatusOverlay(
                message = message,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        VirtualControllerOverlay(
            opacity = opacity,
            sizeScale = controllerSize,
            onDPadInput = { direction, isPressed ->
                injectKeyEvent(webViewRef, NexusInput.getDirectionKeycode(direction), isPressed)
            },
            onActionInput = { action, isPressed ->
                injectKeyEvent(webViewRef, NexusInput.getActionKeycode(action), isPressed)
            }
        )
    }
}

@Composable
private fun RuntimeStatusOverlay(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(16.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (message.startsWith("Loading")) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private class RuntimeEventBridge(
    private val onMessage: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun reportCrash(message: String?) {
        val cleanMessage = message
            ?.replace('\n', ' ')
            ?.replace('\r', ' ')
            ?.take(180)
            ?: "Runtime error"
        mainHandler.post {
            onMessage(cleanMessage)
        }
    }
}

private fun installControllerInputShim(webView: WebView) {
    webView.evaluateJavascript(
        """
        (function() {
            if (window.__nexusDispatchKey) return;

            var keyNames = {
                ${NexusInput.KEY_ENTER}: ['Enter', 'Enter'],
                ${NexusInput.KEY_SHIFT}: ['Shift', 'ShiftLeft'],
                ${NexusInput.KEY_ESC}: ['Escape', 'Escape'],
                ${NexusInput.KEY_LEFT}: ['ArrowLeft', 'ArrowLeft'],
                ${NexusInput.KEY_UP}: ['ArrowUp', 'ArrowUp'],
                ${NexusInput.KEY_RIGHT}: ['ArrowRight', 'ArrowRight'],
                ${NexusInput.KEY_DOWN}: ['ArrowDown', 'ArrowDown'],
                ${NexusInput.KEY_X}: ['x', 'KeyX'],
                ${NexusInput.KEY_Z}: ['z', 'KeyZ']
            };

            var rpgMakerSymbols = {
                ${NexusInput.KEY_ENTER}: 'ok',
                ${NexusInput.KEY_SHIFT}: 'shift',
                ${NexusInput.KEY_ESC}: 'escape',
                ${NexusInput.KEY_LEFT}: 'left',
                ${NexusInput.KEY_UP}: 'up',
                ${NexusInput.KEY_RIGHT}: 'right',
                ${NexusInput.KEY_DOWN}: 'down',
                ${NexusInput.KEY_X}: 'escape',
                ${NexusInput.KEY_Z}: 'ok'
            };

            function buildEvent(type, keyCode) {
                var keyInfo = keyNames[keyCode] || ['', ''];
                return new KeyboardEvent(type, {
                    key: keyInfo[0],
                    code: keyInfo[1],
                    keyCode: keyCode,
                    which: keyCode,
                    bubbles: true,
                    cancelable: true,
                    composed: true,
                    repeat: false
                });
            }

            function dispatchTo(target, eventType, keyCode) {
                if (!target || !target.dispatchEvent) return;
                target.dispatchEvent(buildEvent(eventType, keyCode));
            }

            function resumeAudio() {
                try {
                    if (window.WebAudio && WebAudio._context && WebAudio._context.state === 'suspended') {
                        WebAudio._context.resume();
                    }
                } catch (e) {}
                try {
                    if (window.AudioManager && AudioManager._context && AudioManager._context.state === 'suspended') {
                        AudioManager._context.resume();
                    }
                } catch (e) {}
            }

            window.__nexusDispatchKey = function(keyCode, pressed) {
                resumeAudio();
                var eventType = pressed ? 'keydown' : 'keyup';
                dispatchTo(window, eventType, keyCode);
                dispatchTo(document, eventType, keyCode);
                dispatchTo(document.activeElement, eventType, keyCode);
                dispatchTo(document.body, eventType, keyCode);

                var symbol = rpgMakerSymbols[keyCode];
                if (symbol && window.Input && window.Input._currentState) {
                    window.Input._currentState[symbol] = !!pressed;
                }

                if (window.Graphics && window.Graphics._canvas && window.Graphics._canvas.focus) {
                    window.Graphics._canvas.focus();
                }

                var rufflePlayer = document.querySelector('ruffle-player');
                if (rufflePlayer && rufflePlayer.focus) {
                    rufflePlayer.focus();
                }
            };
        })();
        """.trimIndent(),
        null
    )
}

private fun configureRpgMakerRuntime(
    webView: WebView,
    engineType: SafManager.GameEngine,
    showFps: Boolean,
    fpsLimit: Int
) {
    if (!engineType.isRpgMakerWebRuntime) return

    webView.evaluateJavascript(
        """
        (function(showFps, fpsLimit) {
            var runtime = window.__nexusRpgMakerRuntime || {};
            if (!runtime.installed) {
                runtime.installed = true;
                runtime.nativeRaf = window.requestAnimationFrame.bind(window);
                runtime.nativeCancel = window.cancelAnimationFrame.bind(window);

                runtime.report = function(prefix, detail) {
                    try {
                        if (window.NexusRuntime && NexusRuntime.reportCrash) {
                            NexusRuntime.reportCrash(prefix + ': ' + (detail || 'unknown error'));
                        }
                    } catch (e) {}
                };

                window.addEventListener('error', function(event) {
                    runtime.report('Runtime error', event.message);
                });
                window.addEventListener('unhandledrejection', function(event) {
                    var reason = event.reason;
                    runtime.report('Runtime promise error', reason && (reason.message || reason.toString()));
                });

                runtime.focusCanvas = function() {
                    try {
                        var canvas = window.Graphics && window.Graphics._canvas;
                        if (canvas && canvas.focus) {
                            canvas.tabIndex = 0;
                            canvas.focus();
                        }
                    } catch (e) {}
                };

                runtime.unlockAudio = function() {
                    try {
                        if (window.WebAudio && WebAudio._context && WebAudio._context.state === 'suspended') {
                            WebAudio._context.resume();
                        }
                    } catch (e) {}
                    try {
                        if (window.AudioManager && AudioManager._context && AudioManager._context.state === 'suspended') {
                            AudioManager._context.resume();
                        }
                    } catch (e) {}
                };

                ['touchstart', 'pointerdown', 'mousedown', 'keydown'].forEach(function(type) {
                    window.addEventListener(type, function() {
                        runtime.unlockAudio();
                        runtime.focusCanvas();
                    }, { capture: true, passive: true });
                });
            }

            runtime.setFpsLimit = function(limit) {
                var normalized = Math.max(30, Math.min(120, Number(limit) || 60));
                if (normalized >= 55) {
                    window.requestAnimationFrame = runtime.nativeRaf;
                    window.cancelAnimationFrame = runtime.nativeCancel;
                    return;
                }

                var minFrameMs = 1000 / normalized;
                var lastFrameAt = 0;
                window.requestAnimationFrame = function(callback) {
                    var handle = { id: 0, cancelled: false };
                    function tick(timestamp) {
                        if (handle.cancelled) return;
                        if (!lastFrameAt || timestamp - lastFrameAt >= minFrameMs) {
                            lastFrameAt = timestamp;
                            callback(timestamp);
                        } else {
                            handle.id = runtime.nativeRaf(tick);
                        }
                    }
                    handle.id = runtime.nativeRaf(tick);
                    return handle;
                };
                window.cancelAnimationFrame = function(handle) {
                    if (handle && typeof handle === 'object') {
                        handle.cancelled = true;
                        runtime.nativeCancel(handle.id);
                    } else {
                        runtime.nativeCancel(handle);
                    }
                };
            };

            runtime.setFpsOverlay = function(enabled) {
                var overlay = document.getElementById('nexus-fps-overlay');
                if (!overlay) {
                    overlay = document.createElement('div');
                    overlay.id = 'nexus-fps-overlay';
                    overlay.style.cssText = 'position:fixed;top:10px;right:10px;z-index:2147483647;padding:5px 8px;border-radius:8px;background:rgba(0,0,0,.72);color:white;font:12px sans-serif;pointer-events:none';
                    document.documentElement.appendChild(overlay);
                }
                overlay.style.display = enabled ? 'block' : 'none';

                if (!runtime.fpsLoopStarted) {
                    runtime.fpsLoopStarted = true;
                    runtime.frames = 0;
                    runtime.lastSample = performance.now();
                    function sample(now) {
                        runtime.frames++;
                        if (now - runtime.lastSample >= 1000) {
                            overlay.textContent = Math.round(runtime.frames * 1000 / (now - runtime.lastSample)) + ' FPS';
                            runtime.frames = 0;
                            runtime.lastSample = now;
                        }
                        runtime.nativeRaf(sample);
                    }
                    runtime.nativeRaf(sample);
                }
            };

            runtime.focusCanvas();
            runtime.setFpsLimit(fpsLimit);
            runtime.setFpsOverlay(!!showFps);
            window.__nexusRpgMakerRuntime = runtime;
        })($showFps, $fpsLimit);
        """.trimIndent(),
        null
    )
}

private fun findHtmlEntryPath(context: android.content.Context, gameFolderUri: Uri): String {
    val rootFolder = DocumentFile.fromTreeUri(context, gameFolderUri) ?: return "/index.html"
    if (rootFolder.findFileIgnoreCase("index.html")?.isFile == true) return "/index.html"

    val wwwFolder = rootFolder.findFileIgnoreCase("www")
    if (wwwFolder?.findFileIgnoreCase("index.html")?.isFile == true) return "/www/index.html"

    return findIndexPath(rootFolder, maxDepth = 2) ?: "/index.html"
}

private fun findIndexPath(folder: DocumentFile, maxDepth: Int, prefix: String = ""): String? {
    if (maxDepth < 0) return null

    for (file in folder.listFiles()) {
        val name = file.name ?: continue
        val path = "$prefix/$name"

        if (file.isFile && name.equals("index.html", ignoreCase = true)) {
            return path
        }

        if (file.isDirectory) {
            val nestedPath = findIndexPath(file, maxDepth - 1, path)
            if (nestedPath != null) return nestedPath
        }
    }

    return null
}

private fun findFirstSwfPath(context: android.content.Context, gameFolderUri: Uri): String? {
    val rootFolder = DocumentFile.fromTreeUri(context, gameFolderUri) ?: return null
    return findFirstSwfPath(rootFolder, maxDepth = 3)
}

private fun findFirstSwfPath(folder: DocumentFile, maxDepth: Int, prefix: String = ""): String? {
    if (maxDepth < 0) return null

    for (file in folder.listFiles()) {
        val name = file.name ?: continue
        val path = "$prefix/$name"

        if (file.isFile && name.endsWith(".swf", ignoreCase = true)) {
            return path
        }

        if (file.isDirectory) {
            val nestedPath = findFirstSwfPath(file, maxDepth - 1, path)
            if (nestedPath != null) return nestedPath
        }
    }

    return null
}

private fun DocumentFile.findFileIgnoreCase(name: String): DocumentFile? {
    return findFile(name) ?: listFiles().firstOrNull { file ->
        file.name.equals(name, ignoreCase = true)
    }
}

private val SafManager.GameEngine.isRpgMakerWebRuntime: Boolean
    get() = this == SafManager.GameEngine.RPG_MAKER_MV || this == SafManager.GameEngine.RPG_MAKER_MZ

private fun String.looksFatalConsoleMessage(): Boolean {
    return contains("uncaught", ignoreCase = true) ||
            contains("typeerror", ignoreCase = true) ||
            contains("referenceerror", ignoreCase = true) ||
            contains("syntaxerror", ignoreCase = true) ||
            contains("failed to load", ignoreCase = true)
}

private fun injectKeyEvent(webView: WebView?, keyCode: Int, isPressed: Boolean) {
    val eventType = if (isPressed) "keydown" else "keyup"
    webView?.evaluateJavascript(
        """
        (function() {
            if (window.__nexusDispatchKey) {
                window.__nexusDispatchKey($keyCode, $isPressed);
                return;
            }

            var keyNames = {
                ${NexusInput.KEY_ENTER}: ['Enter', 'Enter'],
                ${NexusInput.KEY_SHIFT}: ['Shift', 'ShiftLeft'],
                ${NexusInput.KEY_ESC}: ['Escape', 'Escape'],
                ${NexusInput.KEY_LEFT}: ['ArrowLeft', 'ArrowLeft'],
                ${NexusInput.KEY_UP}: ['ArrowUp', 'ArrowUp'],
                ${NexusInput.KEY_RIGHT}: ['ArrowRight', 'ArrowRight'],
                ${NexusInput.KEY_DOWN}: ['ArrowDown', 'ArrowDown'],
                ${NexusInput.KEY_X}: ['x', 'KeyX'],
                ${NexusInput.KEY_Z}: ['z', 'KeyZ']
            };
            var keyInfo = keyNames[$keyCode] || ['', ''];
            var eventOptions = {
                key: keyInfo[0],
                code: keyInfo[1],
                keyCode: $keyCode,
                which: $keyCode,
                bubbles: true,
                cancelable: true,
                composed: true,
                repeat: false
            };
            var event = new KeyboardEvent('$eventType', eventOptions);
            window.dispatchEvent(event);
            document.dispatchEvent(event);
            if (document.body) {
                document.body.dispatchEvent(new KeyboardEvent('$eventType', eventOptions));
            }
        })();
        """.trimIndent(),
        null
    )
}
