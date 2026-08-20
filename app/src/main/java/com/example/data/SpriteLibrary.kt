package com.example.data

import com.example.model.*
import java.util.UUID

data class SpriteAsset(
    val id: String,
    val name: String,
    val category: String,
    val shapeKind: ShapeKind,
    val width: Float,
    val height: Float,
    val defaultFill: Long,
    val defaultStroke: Long = 0xFFFFFFFF
)

object SpriteLibrary {

    val CATEGORIES = listOf("All", "Game Hero", "Monsters", "Items & Loot", "FX & Combat", "Basic Shapes")

    val ASSETS = listOf(
        // Hero / Characters
        SpriteAsset("hero_head", "Hero Head / Helmet", "Game Hero", ShapeKind.ROUNDED_RECT, 70f, 70f, 0xFF60A5FA),
        SpriteAsset("hero_body", "Hero Torso Armor", "Game Hero", ShapeKind.ROUNDED_RECT, 80f, 100f, 0xFF3B82F6),
        SpriteAsset("hero_shield", "Knight Shield", "Game Hero", ShapeKind.SHIELD, 60f, 80f, 0xFF38BDF8),
        SpriteAsset("hero_sword", "Energy Blade", "Game Hero", ShapeKind.SWORD, 30f, 110f, 0xFFA855F7),
        SpriteAsset("hero_limb", "Limb / Arm / Leg", "Game Hero", ShapeKind.ROUNDED_RECT, 24f, 60f, 0xFF2563EB),

        // Monsters
        SpriteAsset("slime_green", "Bouncy Slime", "Monsters", ShapeKind.SLIME, 90f, 80f, 0xFF22C55E),
        SpriteAsset("slime_purple", "Shadow Slime", "Monsters", ShapeKind.SLIME, 90f, 80f, 0xFFA855F7),
        SpriteAsset("monster_eye", "Gazer Eye", "Monsters", ShapeKind.CIRCLE, 50f, 50f, 0xFFEF4444),

        // Items & Loot
        SpriteAsset("gold_coin", "Gold Coin", "Items & Loot", ShapeKind.COIN, 64f, 64f, 0xFFFACC15),
        SpriteAsset("ruby_gem", "Power Ruby", "Items & Loot", ShapeKind.DIAMOND, 60f, 60f, 0xFFF43F5E),
        SpriteAsset("star_bonus", "Star Bonus", "Items & Loot", ShapeKind.STAR, 70f, 70f, 0xFFFBBF24),

        // FX & Combat
        SpriteAsset("slash_fx", "Katana Slash Arc", "FX & Combat", ShapeKind.SLASH_FX, 120f, 120f, 0xFFC084FC),
        SpriteAsset("arrow_proj", "Game Arrow", "FX & Combat", ShapeKind.ARROW, 80f, 30f, 0xFFE2E8F0),
        SpriteAsset("blast_circle", "Shockwave Ring", "FX & Combat", ShapeKind.CIRCLE, 100f, 100f, 0xFF38BDF8),

        // Basic Shapes
        SpriteAsset("shape_box", "Rounded Box", "Basic Shapes", ShapeKind.ROUNDED_RECT, 80f, 80f, 0xFFA855F7),
        SpriteAsset("shape_circle", "Circle", "Basic Shapes", ShapeKind.CIRCLE, 80f, 80f, 0xFFEC4899),
        SpriteAsset("shape_tri", "Triangle", "Basic Shapes", ShapeKind.TRIANGLE, 80f, 80f, 0xFF38BDF8),
        SpriteAsset("shape_star", "5-Star", "Basic Shapes", ShapeKind.STAR, 80f, 80f, 0xFFEAB308)
    )

    fun createLayerFromAsset(asset: SpriteAsset, startX: Float = 0f, startY: Float = 0f): AnimationLayer {
        val id = UUID.randomUUID().toString()
        return AnimationLayer(
            id = id,
            name = asset.name,
            type = LayerType.SPRITE_PRESET,
            shapeKind = asset.shapeKind,
            presetAssetId = asset.id,
            width = asset.width,
            height = asset.height,
            shapeStyle = ShapeStyle(
                fillColor = asset.defaultFill,
                strokeColor = asset.defaultStroke,
                hasFill = true,
                hasStroke = true,
                strokeWidth = 2f,
                cornerRadius = 16f
            ),
            keyframes = listOf(
                Keyframe(
                    frame = 0,
                    x = startX,
                    y = startY,
                    rotation = 0f,
                    scaleX = 1f,
                    scaleY = 1f,
                    opacity = 1f,
                    easing = EasingType.EASE_IN_OUT_CUBIC
                )
            )
        )
    }
}
