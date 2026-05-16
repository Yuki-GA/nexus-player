package com.zen.myapplication.nexus.core.engine

/**
 * Acts as the bridge between the Android Kotlin lifecycle and the underlying
 * C++ SDL2 / Python / Ruby engine via JNI.
 */
class NativeEngineBridge {

    /**
     * Reports whether this APK currently contains the native runtime core for an engine.
     */
    external fun isRuntimeAvailable(engineName: String): Boolean

    /**
     * Called when the Native Engine is first initialized.
     * @param engineName The native runtime to load.
     * @param dataUri The SAF Uri for the selected game folder.
     */
    external fun initEngine(engineName: String, dataUri: String): Boolean

    /**
     * Called when the Android Surface (usually from a SurfaceView) is ready to be drawn on.
     */
    external fun surfaceCreated(surface: Any)

    /**
     * Called when the dimensions of the surface change (e.g., screen rotation).
     */
    external fun surfaceChanged(width: Int, height: Int)

    /**
     * Called when the surface is destroyed.
     */
    external fun surfaceDestroyed()

    /**
     * Passes a touch or controller event down to the native engine.
     */
    external fun sendInputEvent(action: Int, x: Float, y: Float)

    /**
     * Passes a keyboard-style controller event down to the native engine.
     */
    external fun sendKeyEvent(keyCode: Int, pressed: Boolean)

    /**
     * Called when leaving the native player screen.
     */
    external fun shutdownEngine()

    companion object {
        init {
            // This loads the compiled C++ library "libnexus-engine.so"
            System.loadLibrary("nexus-engine")
        }
    }
}
