================================================================================
KORVA VFX NATIVE SHARED LIBRARY DIRECTORY
================================================================================

Place your pre-compiled shared library file:
  libkorva_vfx.so

into this exact folder:
  /app/src/main/jniLibs/arm64-v8a/libkorva_vfx.so

The application's VFXNativeBridge will automatically detect and link to:
  System.loadLibrary("korva_vfx")

Supported ABIs:
  - arm64-v8a (Recommended for modern Android 64-bit devices)
  - armeabi-v7a (32-bit ARM)
  - x86_64 (64-bit Emulator / ChromeOS)

JNI Class bindings supported:
  1) com.korva.engine.VFXNativeBridge
  2) com.example.engine.vfx.VFXNativeBridge

When the .so file is present, the app will execute physics simulation and particle
rendering through native C++ for extreme performance. When absent, the app seamlessly
uses the built-in high-performance Kotlin particle engine.
