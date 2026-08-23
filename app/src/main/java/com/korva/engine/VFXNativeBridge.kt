package com.korva.engine

import android.util.Log

/**
 * Official JNI Native Bridge for Korva VFX C++ Engine (`libkorva_vfx.so`).
 * Matches:
 *  - Java_com_korva_engine_VFXNativeBridge_initEffect
 *  - Java_com_korva_engine_VFXNativeBridge_updateEffect
 *  - Java_com_korva_engine_VFXNativeBridge_getActiveParticleCount
 *  - Java_com_korva_engine_VFXNativeBridge_restartEffect
 *  - Java_com_korva_engine_VFXNativeBridge_releaseEffect
 */
object VFXNativeBridge {

    private const val TAG = "VFXNativeBridge"
    private const val LIB_NAME = "korva_vfx"

    var isNativeLoaded: Boolean = false
        private set

    var loadErrorMessage: String? = null
        private set

    init {
        try {
            System.loadLibrary(LIB_NAME)
            isNativeLoaded = true
            Log.i(TAG, "Successfully loaded native shared library: lib$LIB_NAME.so")
        } catch (e: UnsatisfiedLinkError) {
            isNativeLoaded = false
            loadErrorMessage = e.message
            Log.w(TAG, "Native library lib$LIB_NAME.so not found in jniLibs. Using Kotlin VFX Runtime Engine: ${e.message}")
        } catch (e: Exception) {
            isNativeLoaded = false
            loadErrorMessage = e.message
            Log.w(TAG, "Failed loading lib$LIB_NAME.so: ${e.message}")
        }
    }

    // Native C++ JNI function declarations
    external fun initEffect(name: String, id: String)
    external fun updateEffect(deltaTime: Float)
    external fun getActiveParticleCount(): Int
    external fun restartEffect()
    external fun releaseEffect()
}
