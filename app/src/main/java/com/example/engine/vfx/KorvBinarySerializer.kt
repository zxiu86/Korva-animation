package com.example.engine.vfx

import com.example.model.vfx.*
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32

/**
 * High-Performance .korv Binary Serializer and Deserializer
 * Strict implementation conforming to Korva Animation VFX Specification 1.0.
 */
object KorvBinarySerializer {

    private const val MAGIC = "KORV"
    private const val VERSION: Short = 0x0100
    private const val CONTENT_TYPE_VFX: Byte = 0x02
    private const val END_MARKER: Long = 0xDEADBEEF

    /**
     * Serializes a VFXEffect into binary .korv format bytes.
     */
    fun serialize(effect: VFXEffect): ByteArray {
        val stream = ByteArrayOutputStream()
        val buffer = ByteBuffer.allocate(1024 * 64).order(ByteOrder.LITTLE_ENDIAN)

        // 1. File Header (16 bytes)
        buffer.put(MAGIC.toByteArray(Charsets.US_ASCII)) // 4 bytes
        buffer.putShort(VERSION)                         // 2 bytes
        buffer.put(CONTENT_TYPE_VFX)                     // 1 byte
        var flags: Byte = 0
        if (effect.looping) flags = (flags.toInt() or 0x01).toByte()
        buffer.put(flags)                                // 1 byte

        val nowSeconds = (System.currentTimeMillis() / 1000).toInt()
        buffer.putInt(nowSeconds)                        // 4 bytes: CreationTime
        buffer.putInt(nowSeconds)                        // 4 bytes: ModificationTime

        // 2. Effect Metadata
        val nameBytes = effect.name.toByteArray(Charsets.UTF_8)
        buffer.putShort(nameBytes.size.toShort())
        buffer.put(nameBytes)

        // 16-byte fixed EffectID
        val idBytes = ByteArray(16)
        val rawId = effect.effectId.toByteArray(Charsets.UTF_8)
        System.arraycopy(rawId, 0, idBytes, 0, minOf(rawId.size, 16))
        buffer.put(idBytes)

        // 16-byte fixed Version
        val verBytes = ByteArray(16)
        val rawVer = effect.version.toByteArray(Charsets.UTF_8)
        System.arraycopy(rawVer, 0, verBytes, 0, minOf(rawVer.size, 16))
        buffer.put(verBytes)

        buffer.putFloat(effect.duration)
        buffer.put(effect.emitters.size.toByte())

        // 3. Emitters
        for (emitter in effect.emitters) {
            val emitterNameBytes = emitter.name.toByteArray(Charsets.UTF_8)
            buffer.putShort(emitterNameBytes.size.toShort())
            buffer.put(emitterNameBytes)

            buffer.put(emitter.shapeType.id.toByte())
            buffer.putFloat(emitter.shapeSize.x)
            buffer.putFloat(emitter.shapeSize.y)

            buffer.putFloat(emitter.spawnRate)
            buffer.putShort(emitter.burstCount.toShort())
            buffer.putFloat(emitter.burstInterval)

            buffer.putFloat(emitter.particleLifetime)
            buffer.putFloat(emitter.speedMin)
            buffer.putFloat(emitter.speedMax)
            buffer.putFloat(emitter.spreadAngle)

            // Modules
            buffer.putShort(emitter.modules.size.toShort())
            for (module in emitter.modules) {
                when (module) {
                    is GravityModule -> {
                        buffer.put(ModuleTypeId.GRAVITY.id.toByte())
                        buffer.putShort(12.toShort()) // Data length: 12 bytes
                        buffer.putFloat(module.gravity)
                        buffer.putFloat(module.damping)
                        buffer.putFloat(0.0f) // Reserved
                    }
                    is ScaleModule -> {
                        buffer.put(ModuleTypeId.SCALE_OVER_LIFETIME.id.toByte())
                        val dataLen = 1 + 2 + module.scaleCurve.keyframes.size * 8
                        buffer.putShort(dataLen.toShort())
                        buffer.put(module.scaleCurve.interpolation.id.toByte())
                        buffer.putShort(module.scaleCurve.keyframes.size.toShort())
                        for (kf in module.scaleCurve.keyframes) {
                            buffer.putFloat(kf.time)
                            buffer.putFloat(kf.value)
                        }
                    }
                    is ColorModule -> {
                        buffer.put(ModuleTypeId.COLOR_OVER_LIFETIME.id.toByte())
                        val dataLen = 1 + 2 + module.colorGradient.keys.size * 8
                        buffer.putShort(dataLen.toShort())
                        buffer.put(module.colorGradient.interpolation.id.toByte())
                        buffer.putShort(module.colorGradient.keys.size.toShort())
                        for (key in module.colorGradient.keys) {
                            buffer.putFloat(key.time)
                            buffer.put(key.color.r.toByte())
                            buffer.put(key.color.g.toByte())
                            buffer.put(key.color.b.toByte())
                            val alphaByte = (key.color.a.coerceIn(0f, 1f) * 255).toInt().toByte()
                            buffer.put(alphaByte)
                        }
                    }
                    is AlphaModule -> {
                        buffer.put(ModuleTypeId.ALPHA_OVER_LIFETIME.id.toByte())
                        val dataLen = 1 + 2 + module.alphaCurve.keyframes.size * 8
                        buffer.putShort(dataLen.toShort())
                        buffer.put(module.alphaCurve.interpolation.id.toByte())
                        buffer.putShort(module.alphaCurve.keyframes.size.toShort())
                        for (kf in module.alphaCurve.keyframes) {
                            buffer.putFloat(kf.time)
                            buffer.putFloat(kf.value)
                        }
                    }
                    is LifetimeModule -> {
                        buffer.put(ModuleTypeId.LIFETIME.id.toByte())
                        buffer.putShort(0.toShort())
                    }
                    is VelocityModule -> {
                        buffer.put(ModuleTypeId.VELOCITY.id.toByte())
                        buffer.putShort(0.toShort())
                    }
                    is RotationModule -> {
                        buffer.put(ModuleTypeId.ROTATION.id.toByte())
                        buffer.putShort(0.toShort())
                    }
                }
            }

            // Texture Reference Block
            val atlasBytes = emitter.textureAtlas.toByteArray(Charsets.UTF_8)
            buffer.putShort(atlasBytes.size.toShort())
            buffer.put(atlasBytes)
            buffer.putFloat(emitter.textureUVRect.uvX)
            buffer.putFloat(emitter.textureUVRect.uvY)
            buffer.putFloat(emitter.textureUVRect.uvWidth)
            buffer.putFloat(emitter.textureUVRect.uvHeight)
        }

        // Get byte payload for CRC calculation
        val length = buffer.position()
        val dataBytes = ByteArray(length)
        buffer.flip()
        buffer.get(dataBytes)

        // Calculate CRC32
        val crc = CRC32()
        crc.update(dataBytes)
        val crcValue = crc.value.toInt()

        // Append Checksum & End Marker (8 bytes)
        val finalBuffer = ByteBuffer.allocate(length + 8).order(ByteOrder.LITTLE_ENDIAN)
        finalBuffer.put(dataBytes)
        finalBuffer.putInt(crcValue)
        finalBuffer.putInt(END_MARKER.toInt())

        return finalBuffer.array()
    }

    /**
     * Validates binary .korv format integrity.
     */
    fun validate(bytes: ByteArray): ValidationResult {
        if (bytes.size < 24) {
            return ValidationResult(false, "File too small (${bytes.size} bytes). Minimum size is 24 bytes.")
        }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // Check Magic
        val magicBytes = ByteArray(4)
        buffer.get(magicBytes)
        val magic = String(magicBytes, Charsets.US_ASCII)
        if (magic != MAGIC) {
            return ValidationResult(false, "Invalid Magic Header '$magic'. Expected 'KORV'.")
        }

        // Check End Marker
        val endMarker = buffer.getInt(bytes.size - 4)
        if (endMarker != END_MARKER.toInt()) {
            return ValidationResult(false, "Invalid End Marker 0x${Integer.toHexString(endMarker).uppercase()}. Expected 0xDEADBEEF.")
        }

        // Check CRC32
        val fileCrc = buffer.getInt(bytes.size - 8)
        val crc = CRC32()
        crc.update(bytes, 0, bytes.size - 8)
        val calculatedCrc = crc.value.toInt()

        if (fileCrc != calculatedCrc) {
            return ValidationResult(false, "CRC32 Checksum mismatch! File: 0x${Integer.toHexString(fileCrc).uppercase()}, Computed: 0x${Integer.toHexString(calculatedCrc).uppercase()}")
        }

        return ValidationResult(true, "Valid .korv binary file (CRC32: 0x${Integer.toHexString(fileCrc).uppercase()})")
    }

    /**
     * Deserializes binary .korv format into a VFXEffect object.
     */
    fun deserialize(bytes: ByteArray): VFXEffect? {
        val validation = validate(bytes)
        if (!validation.isValid) return null

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // Header
        buffer.position(4) // Skip MAGIC
        val version = buffer.short
        val contentType = buffer.get()
        val flags = buffer.get()
        val looping = (flags.toInt() and 0x01) != 0
        val creationTime = buffer.int
        val modTime = buffer.int

        // Metadata
        val nameLen = buffer.short.toInt() and 0xFFFF
        val nameBytes = ByteArray(nameLen)
        buffer.get(nameBytes)
        val effectName = String(nameBytes, Charsets.UTF_8)

        val idBytes = ByteArray(16)
        buffer.get(idBytes)
        val effectId = String(idBytes, Charsets.UTF_8).trimEnd('\u0000')

        val verBytes = ByteArray(16)
        buffer.get(verBytes)
        val verStr = String(verBytes, Charsets.UTF_8).trimEnd('\u0000')

        val duration = buffer.float
        val emitterCount = buffer.get().toInt() and 0xFF

        val effect = VFXEffect(
            name = effectName,
            effectId = effectId,
            version = verStr,
            duration = duration,
            looping = looping,
            blendMode = BlendMode.ADDITIVE
        )

        // Emitters
        for (e in 0 until emitterCount) {
            val emitterNameLen = buffer.short.toInt() and 0xFFFF
            val emitterNameBytes = ByteArray(emitterNameLen)
            buffer.get(emitterNameBytes)
            val emitterName = String(emitterNameBytes, Charsets.UTF_8)

            val shapeType = ShapeType.fromId(buffer.get().toInt() and 0xFF)
            val sizeX = buffer.float
            val sizeY = buffer.float

            val spawnRate = buffer.float
            val burstCount = buffer.short.toInt() and 0xFFFF
            val burstInterval = buffer.float

            val lifetime = buffer.float
            val speedMin = buffer.float
            val speedMax = buffer.float
            val spreadAngle = buffer.float

            val emitter = VFXEmitter(
                name = emitterName,
                shapeType = shapeType,
                shapeSize = Vector2(sizeX, sizeY),
                spawnRate = spawnRate,
                burstCount = burstCount,
                burstInterval = burstInterval,
                particleLifetime = lifetime,
                speedMin = speedMin,
                speedMax = speedMax,
                spreadAngle = spreadAngle
            )

            // Modules
            val moduleCount = buffer.short.toInt() and 0xFFFF
            for (m in 0 until moduleCount) {
                val modType = ModuleTypeId.fromId(buffer.get().toInt() and 0xFF)
                val dataLen = buffer.short.toInt() and 0xFFFF
                when (modType) {
                    ModuleTypeId.GRAVITY -> {
                        val gravity = buffer.float
                        val damping = buffer.float
                        buffer.float // Reserved
                        emitter.modules.add(GravityModule(gravity, damping))
                    }
                    ModuleTypeId.SCALE_OVER_LIFETIME -> {
                        val interp = InterpolationType.fromId(buffer.get().toInt() and 0xFF)
                        val kfCount = buffer.short.toInt() and 0xFFFF
                        val curve = VFXCurve(interpolation = interp)
                        for (k in 0 until kfCount) {
                            val time = buffer.float
                            val value = buffer.float
                            curve.keyframes.add(CurveKeyframe(time, value))
                        }
                        emitter.modules.add(ScaleModule(curve))
                    }
                    ModuleTypeId.COLOR_OVER_LIFETIME -> {
                        val interp = InterpolationType.fromId(buffer.get().toInt() and 0xFF)
                        val keyCount = buffer.short.toInt() and 0xFFFF
                        val gradient = VFXGradient(interpolation = interp)
                        for (k in 0 until keyCount) {
                            val time = buffer.float
                            val r = buffer.get().toInt() and 0xFF
                            val g = buffer.get().toInt() and 0xFF
                            val b = buffer.get().toInt() and 0xFF
                            val a = (buffer.get().toInt() and 0xFF).toFloat() / 255f
                            gradient.keys.add(GradientColorKey(time, ColorRGBA(r, g, b, a)))
                        }
                        emitter.modules.add(ColorModule(gradient))
                    }
                    ModuleTypeId.ALPHA_OVER_LIFETIME -> {
                        val interp = InterpolationType.fromId(buffer.get().toInt() and 0xFF)
                        val kfCount = buffer.short.toInt() and 0xFFFF
                        val curve = VFXCurve(interpolation = interp)
                        for (k in 0 until kfCount) {
                            val time = buffer.float
                            val value = buffer.float
                            curve.keyframes.add(CurveKeyframe(time, value))
                        }
                        emitter.modules.add(AlphaModule(curve))
                    }
                    ModuleTypeId.LIFETIME -> emitter.modules.add(LifetimeModule())
                    ModuleTypeId.VELOCITY -> emitter.modules.add(VelocityModule())
                    ModuleTypeId.ROTATION -> emitter.modules.add(RotationModule())
                }
            }

            // Texture Reference
            val atlasLen = buffer.short.toInt() and 0xFFFF
            val atlasBytes = ByteArray(atlasLen)
            buffer.get(atlasBytes)
            emitter.textureAtlas = String(atlasBytes, Charsets.UTF_8)
            emitter.textureUVRect = TextureRect(
                uvX = buffer.float,
                uvY = buffer.float,
                uvWidth = buffer.float,
                uvHeight = buffer.float
            )

            effect.emitters.add(emitter)
        }

        return effect
    }

    data class ValidationResult(val isValid: Boolean, val message: String)

    fun formatHexDump(bytes: ByteArray, maxLines: Int = 16): String {
        val sb = StringBuilder()
        val limit = minOf(bytes.size, maxLines * 16)
        for (i in 0 until limit step 16) {
            sb.append(String.format("%04X:  ", i))
            val chunkLen = minOf(16, bytes.size - i)
            for (j in 0 until 16) {
                if (j < chunkLen) {
                    sb.append(String.format("%02X ", bytes[i + j]))
                } else {
                    sb.append("   ")
                }
                if (j == 7) sb.append(" ")
            }
            sb.append(" |")
            for (j in 0 until chunkLen) {
                val b = bytes[i + j].toInt().toChar()
                if (b in ' '..'~') sb.append(b) else sb.append('.')
            }
            sb.append("|\n")
        }
        if (bytes.size > limit) {
            sb.append(String.format("... (%d more bytes)\n", bytes.size - limit))
        }
        return sb.toString()
    }
}
