#include <android/log.h>
#include <android/native_window_jni.h>
#include <jni.h>
#include <string>

#define LOG_TAG "NexusNativeEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

ANativeWindow *window = nullptr;
std::string currentEngine;
std::string currentDataUri;

bool runtimeAvailableFor(const std::string &engineName) {
  (void)engineName;
  // Flip this once mkxp-z/Ren'Py native cores are bundled and linked.
  return false;
}

std::string toString(JNIEnv *env, jstring value) {
  if (value == nullptr) {
    return "";
  }

  const char *chars = env->GetStringUTFChars(value, nullptr);
  if (chars == nullptr) {
    return "";
  }

  std::string result(chars);
  env->ReleaseStringUTFChars(value, chars);
  return result;
}

} // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_com_zen_myapplication_nexus_core_engine_NativeEngineBridge_isRuntimeAvailable(
    JNIEnv *env, jobject /* this */, jstring engineName) {

  const std::string nativeEngineName = toString(env, engineName);
  return runtimeAvailableFor(nativeEngineName) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_zen_myapplication_nexus_core_engine_NativeEngineBridge_initEngine(
    JNIEnv *env, jobject /* this */, jstring engineName, jstring dataUri) {

  currentEngine = toString(env, engineName);
  currentDataUri = toString(env, dataUri);

  LOGI("Initializing native runtime. engine=%s, uri=%s", currentEngine.c_str(),
       currentDataUri.c_str());

  if (!runtimeAvailableFor(currentEngine)) {
    LOGE("Native runtime core is not bundled for engine=%s", currentEngine.c_str());
    return JNI_FALSE;
  }

  // Runtime hooks are intentionally split here:
  // - RENPY needs a bundled Python/Ren'Py runtime.
  // - RPG_MAKER_XP_VX_ACE needs an RGSS-compatible runtime such as mkxp-z.
  // This bridge owns the Android lifecycle and input path those engines attach to.

  return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_zen_myapplication_nexus_core_engine_NativeEngineBridge_surfaceCreated(
    JNIEnv *env, jobject /* this */, jobject surface) {

  if (surface != nullptr) {
    window = ANativeWindow_fromSurface(env, surface);
    LOGI("Surface created successfully for engine=%s.", currentEngine.c_str());
    // TODO: Pass the window to SDL_CreateWindowFrom() after SDL is bundled.
  }
}

extern "C" JNIEXPORT void JNICALL
Java_com_zen_myapplication_nexus_core_engine_NativeEngineBridge_surfaceChanged(
    JNIEnv *env, jobject /* this */, jint width, jint height) {

  LOGI("Surface changed. New dimensions: %dx%d", width, height);
  // TODO: Handle resizing in the SDL2 / Native renderer
}

extern "C" JNIEXPORT void JNICALL
Java_com_zen_myapplication_nexus_core_engine_NativeEngineBridge_surfaceDestroyed(
    JNIEnv *env, jobject /* this */) {

  LOGI("Surface destroyed.");
  if (window != nullptr) {
    ANativeWindow_release(window);
    window = nullptr;
  }
  // TODO: Notify SDL2 that the surface is gone
}

extern "C" JNIEXPORT void JNICALL
Java_com_zen_myapplication_nexus_core_engine_NativeEngineBridge_sendInputEvent(
    JNIEnv *env, jobject /* this */, jint action, jfloat x, jfloat y) {

  (void)env;
  LOGI("Pointer input: action=%d, x=%f, y=%f", action, x, y);
  // TODO: Translate to SDL_Event and push to SDL's event queue.
}

extern "C" JNIEXPORT void JNICALL
Java_com_zen_myapplication_nexus_core_engine_NativeEngineBridge_sendKeyEvent(
    JNIEnv *env, jobject /* this */, jint keyCode, jboolean pressed) {

  (void)env;
  LOGI("Key input: keyCode=%d, pressed=%s", keyCode,
       pressed == JNI_TRUE ? "true" : "false");
  // TODO: Map Android-style key codes to SDL scancodes/native runtime input.
}

extern "C" JNIEXPORT void JNICALL
Java_com_zen_myapplication_nexus_core_engine_NativeEngineBridge_shutdownEngine(
    JNIEnv *env, jobject /* this */) {

  (void)env;
  LOGI("Shutting down native runtime. engine=%s", currentEngine.c_str());
  if (window != nullptr) {
    ANativeWindow_release(window);
    window = nullptr;
  }
  currentEngine.clear();
  currentDataUri.clear();
}
