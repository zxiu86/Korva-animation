package com.example.engine

import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject

object KorExporter {

    fun exportToKorJson(project: KorProject): String {
        val root = JSONObject()
        root.put("format", "korva-animation")
        root.put("version", project.version)
        root.put("id", project.id)
        root.put("name", project.name)
        root.put("createdAt", project.createdAt)
        root.put("fps", project.fps)
        root.put("totalFrames", project.totalFrames)
        root.put("currentFrame", project.currentFrame)
        root.put("loopMode", project.loopMode.name)
        root.put("speedMultiplier", project.speedMultiplier)
        root.put("accelerationType", project.accelerationType.name)
        root.put("resolution", project.resolution.id)
        root.put("backgroundColor", project.backgroundColor)

        val layersArray = JSONArray()
        for (layer in project.layers) {
            val layerObj = JSONObject()
            layerObj.put("id", layer.id)
            layerObj.put("name", layer.name)
            layerObj.put("type", layer.type.name)
            layerObj.put("shapeKind", layer.shapeKind.name)
            layerObj.put("isVisible", layer.isVisible)
            layerObj.put("isLocked", layer.isLocked)
            layerObj.put("opacity", layer.opacity)
            layerObj.put("zIndex", layer.zIndex)
            layerObj.put("width", layer.width)
            layerObj.put("height", layer.height)
            layerObj.put("pivotX", layer.pivotX)
            layerObj.put("pivotY", layer.pivotY)
            layerObj.put("textContent", layer.textContent)
            layer.customImageUri?.let { layerObj.put("customImageUri", it) }
            layer.presetAssetId?.let { layerObj.put("presetAssetId", it) }
            layer.parentId?.let { layerObj.put("parentId", it) }

            // Shape Style
            val styleObj = JSONObject()
            styleObj.put("fillColor", layer.shapeStyle.fillColor)
            styleObj.put("strokeColor", layer.shapeStyle.strokeColor)
            styleObj.put("strokeWidth", layer.shapeStyle.strokeWidth)
            styleObj.put("cornerRadius", layer.shapeStyle.cornerRadius)
            styleObj.put("hasFill", layer.shapeStyle.hasFill)
            styleObj.put("hasStroke", layer.shapeStyle.hasStroke)
            layerObj.put("shapeStyle", styleObj)

            // Keyframes
            val kfArray = JSONArray()
            for (kf in layer.keyframes) {
                val kfObj = JSONObject()
                kfObj.put("frame", kf.frame)
                kfObj.put("x", kf.x)
                kfObj.put("y", kf.y)
                kfObj.put("rotation", kf.rotation)
                kfObj.put("scaleX", kf.scaleX)
                kfObj.put("scaleY", kf.scaleY)
                kfObj.put("opacity", kf.opacity)
                kfObj.put("easing", kf.easing.name)
                kf.colorTint?.let { kfObj.put("colorTint", it) }
                kfArray.put(kfObj)
            }
            layerObj.put("keyframes", kfArray)
            layersArray.put(layerObj)
        }
        root.put("layers", layersArray)

        return root.toString(2)
    }

    fun importFromKorJson(jsonStr: String): KorProject? {
        return try {
            val root = JSONObject(jsonStr)
            val id = root.optString("id", java.util.UUID.randomUUID().toString())
            val name = root.optString("name", "Imported Korva Animation")
            val version = root.optString("version", "1.0.0")
            val createdAt = root.optLong("createdAt", System.currentTimeMillis())
            val fps = root.optInt("fps", 24)
            val totalFrames = root.optInt("totalFrames", 48)
            val currentFrame = root.optInt("currentFrame", 0)
            val loopModeStr = root.optString("loopMode", LoopMode.REPEAT.name)
            val loopMode = try { LoopMode.valueOf(loopModeStr) } catch (e: Exception) { LoopMode.REPEAT }
            val speedMultiplier = root.optDouble("speedMultiplier", 1.0).toFloat()
            val accelStr = root.optString("accelerationType", AccelerationType.CONSTANT.name)
            val accelerationType = try { AccelerationType.valueOf(accelStr) } catch (e: Exception) { AccelerationType.CONSTANT }
            val resId = root.optString("resolution", "720p")
            val resolution = ResolutionPreset.fromId(resId)
            val backgroundColor = root.optLong("backgroundColor", 0xFF12161F)

            val layersList = mutableListOf<AnimationLayer>()
            val layersArray = root.optJSONArray("layers")
            if (layersArray != null) {
                for (i in 0 until layersArray.length()) {
                    val layerObj = layersArray.getJSONObject(i)
                    val layerId = layerObj.optString("id", java.util.UUID.randomUUID().toString())
                    val layerName = layerObj.optString("name", "Layer $i")
                    val typeStr = layerObj.optString("type", LayerType.VECTOR_SHAPE.name)
                    val layerType = try { LayerType.valueOf(typeStr) } catch (e: Exception) { LayerType.VECTOR_SHAPE }
                    val shapeStr = layerObj.optString("shapeKind", ShapeKind.ROUNDED_RECT.name)
                    val shapeKind = try { ShapeKind.valueOf(shapeStr) } catch (e: Exception) { ShapeKind.ROUNDED_RECT }
                    val isVisible = layerObj.optBoolean("isVisible", true)
                    val isLocked = layerObj.optBoolean("isLocked", false)
                    val opacity = layerObj.optDouble("opacity", 1.0).toFloat()
                    val zIndex = layerObj.optInt("zIndex", i)
                    val width = layerObj.optDouble("width", 100.0).toFloat()
                    val height = layerObj.optDouble("height", 100.0).toFloat()
                    val pivotX = layerObj.optDouble("pivotX", 0.5).toFloat()
                    val pivotY = layerObj.optDouble("pivotY", 0.5).toFloat()
                    val textContent = layerObj.optString("textContent", "Text")
                    val customImageUri = if (layerObj.has("customImageUri")) layerObj.getString("customImageUri") else null
                    val presetAssetId = if (layerObj.has("presetAssetId")) layerObj.getString("presetAssetId") else null
                    val parentId = if (layerObj.has("parentId")) layerObj.getString("parentId") else null

                    // Shape style
                    val styleObj = layerObj.optJSONObject("shapeStyle")
                    val shapeStyle = if (styleObj != null) {
                        ShapeStyle(
                            fillColor = styleObj.optLong("fillColor", 0xFFA855F7),
                            strokeColor = styleObj.optLong("strokeColor", 0xFFFFFFFF),
                            strokeWidth = styleObj.optDouble("strokeWidth", 2.0).toFloat(),
                            cornerRadius = styleObj.optDouble("cornerRadius", 12.0).toFloat(),
                            hasFill = styleObj.optBoolean("hasFill", true),
                            hasStroke = styleObj.optBoolean("hasStroke", false)
                        )
                    } else ShapeStyle()

                    // Keyframes
                    val kfList = mutableListOf<Keyframe>()
                    val kfArray = layerObj.optJSONArray("keyframes")
                    if (kfArray != null) {
                        for (k in 0 until kfArray.length()) {
                            val kfObj = kfArray.getJSONObject(k)
                            val frame = kfObj.optInt("frame", 0)
                            val x = kfObj.optDouble("x", 0.0).toFloat()
                            val y = kfObj.optDouble("y", 0.0).toFloat()
                            val rotation = kfObj.optDouble("rotation", 0.0).toFloat()
                            val scaleX = kfObj.optDouble("scaleX", 1.0).toFloat()
                            val scaleY = kfObj.optDouble("scaleY", 1.0).toFloat()
                            val kfOpacity = kfObj.optDouble("opacity", 1.0).toFloat()
                            val easingStr = kfObj.optString("easing", EasingType.EASE_IN_OUT_CUBIC.name)
                            val easing = try { EasingType.valueOf(easingStr) } catch (e: Exception) { EasingType.EASE_IN_OUT_CUBIC }
                            val colorTint = if (kfObj.has("colorTint")) kfObj.getLong("colorTint") else null

                            kfList.add(Keyframe(frame, x, y, rotation, scaleX, scaleY, kfOpacity, easing, colorTint))
                        }
                    }
                    if (kfList.isEmpty()) {
                        kfList.add(Keyframe(frame = 0, x = 0f, y = 0f))
                    }

                    layersList.add(
                        AnimationLayer(
                            id = layerId,
                            name = layerName,
                            type = layerType,
                            shapeKind = shapeKind,
                            isVisible = isVisible,
                            isLocked = isLocked,
                            opacity = opacity,
                            zIndex = zIndex,
                            customImageUri = customImageUri,
                            presetAssetId = presetAssetId,
                            shapeStyle = shapeStyle,
                            textContent = textContent,
                            keyframes = kfList,
                            pivotX = pivotX,
                            pivotY = pivotY,
                            parentId = parentId,
                            width = width,
                            height = height
                        )
                    )
                }
            }

            KorProject(
                id = id,
                name = name,
                version = version,
                createdAt = createdAt,
                fps = fps,
                totalFrames = totalFrames,
                currentFrame = currentFrame,
                loopMode = loopMode,
                speedMultiplier = speedMultiplier,
                accelerationType = accelerationType,
                resolution = resolution,
                layers = layersList,
                backgroundColor = backgroundColor
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportToGodotAnimationJson(project: KorProject): String {
        val root = JSONObject()
        root.put("engine", "godot_compatible_2d")
        root.put("animation_name", project.name.lowercase().replace(" ", "_"))
        root.put("fps", project.fps)
        root.put("length_seconds", project.totalFrames.toFloat() / project.fps.toFloat())
        root.put("loop_mode", if (project.loopMode == LoopMode.REPEAT) "linear" else "none")

        val tracks = JSONArray()
        for (layer in project.layers) {
            val track = JSONObject()
            track.put("path", "Node2D/" + layer.name)
            track.put("type", "value")

            val keys = JSONArray()
            for (kf in layer.keyframes) {
                val key = JSONObject()
                key.put("time", kf.frame.toFloat() / project.fps.toFloat())
                key.put("pos_x", kf.x)
                key.put("pos_y", kf.y)
                key.put("rotation_deg", kf.rotation)
                key.put("scale_x", kf.scaleX)
                key.put("scale_y", kf.scaleY)
                key.put("easing", kf.easing.name)
                keys.put(key)
            }
            track.put("keys", keys)
            tracks.put(track)
        }
        root.put("tracks", tracks)
        return root.toString(2)
    }
}
