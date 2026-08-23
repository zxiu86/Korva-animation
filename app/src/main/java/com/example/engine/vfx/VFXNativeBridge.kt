package com.example.engine.vfx

import android.util.Log

/**
 * Alternative Namespace JNI Native Bridge for Korva VFX C++ Engine (`libkorva_vfx.so`).
 * Matches Java_com_example_engine_vfx_VFXNativeBridge_...
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
            Log.i(TAG, "Successfully linked lib$LIB_NAME.so under com.example.engine.vfx namespace")
        } catch (e: UnsatisfiedLinkError) {
            isNativeLoaded = false
            loadErrorMessage = e.message
            Log.d(TAG, "Native library not loaded in com.example namespace: ${e.message}")
        } catch (e: Exception) {
            isNativeLoaded = false
            loadErrorMessage = e.message
        }
    }

    external fun initEffect(name: String, id: String)
    external fun updateEffect(deltaTime: Float)
    external fun getActiveParticleCount(): Int
}
