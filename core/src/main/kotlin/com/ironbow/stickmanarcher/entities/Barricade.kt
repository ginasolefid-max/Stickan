package com.ironbow.stickmanarcher.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.ironbow.stickmanarcher.assets.AssetManager
import com.ironbow.stickmanarcher.config.GameConfig

class Barricade {
    var x = GameConfig.BARRICADE_X; var y = GameConfig.GROUND_Y
    var width = GameConfig.BARRICADE_WIDTH; var height = 96f
    var hp = GameConfig.BASE_BARRICADE_HP; var maxHp = GameConfig.BASE_BARRICADE_HP
    var armorReduction = 0f; var armorLevel = 0; var hurtFlashTimer = 0f

    fun reset(maxHealth: Float, reduction: Float, level: Int) {
        maxHp = maxHealth; hp = maxHealth; armorReduction = reduction; armorLevel = level; hurtFlashTimer = 0f
    }

    fun isIntact(): Boolean = hp > 0f

    fun applyDamage(rawDamage: Float) {
        if (hp <= 0f) return
        hp = (hp - rawDamage * (1f - armorReduction)).coerceAtLeast(0f)
        hurtFlashTimer = 0.16f
    }

    fun repair(fraction: Float) { hp = (hp + maxHp * fraction).coerceAtMost(maxHp) }
    fun update(dt: Float) { if (hurtFlashTimer > 0f) hurtFlashTimer = (hurtFlashTimer - dt).coerceAtLeast(0f) }

    fun render(batch: SpriteBatch, assets: AssetManager) {
        if (hp <= 0f) {
            val rubbleColor = Color(0.36f, 0.24f, 0.14f, 0.85f)
            assets.drawLimb(batch, x, y, x + 22f, y + 14f, 6f, rubbleColor)
            assets.drawLimb(batch, x + 18f, y, x + 38f, y + 10f, 5f, rubbleColor)
            return
        }
        val hpPct = (hp / maxHp).coerceIn(0f, 1f)
        val wood = if (armorLevel >= 5) Color(0.45f, 0.50f, 0.56f, 1f) else Color(0.48f, 0.30f, 0.16f, 1f)
        val darkWood = Color(wood).mul(0.75f).apply { a = 1f }
        if (hurtFlashTimer > 0f) wood.lerp(Color.WHITE, 0.55f)
        val currentH = height * (0.45f + 0.55f * hpPct)
        assets.drawRect(batch, x, y, 12f, currentH, darkWood)
        assets.drawRect(batch, x + 16f, y, 14f, currentH * 0.92f, wood)
        assets.drawRect(batch, x - 4f, y + currentH * 0.32f, width, 10f, darkWood)
        assets.drawRect(batch, x - 4f, y + currentH * 0.68f, width, 10f, wood)
        val spikeCol = if (armorLevel >= 3) Color(0.78f, 0.82f, 0.88f, 1f) else Color(0.62f, 0.45f, 0.25f, 1f)
        assets.drawLimb(batch, x + 24f, y + 26f, x + 48f, y + 38f, 5f, spikeCol)
        assets.drawLimb(batch, x + 24f, y + 58f, x + 50f, y + 72f, 5f, spikeCol)
    }
}
