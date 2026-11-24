package com.ironbow.stickmanarcher.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Pool
import com.ironbow.stickmanarcher.assets.AssetManager
import com.ironbow.stickmanarcher.config.GameConfig

class AcidProjectile : Pool.Poolable {
    var active = false; var x = 0f; var y = 0f; var vx = 0f; var vy = 0f; var damage = 12f; var radius = 8f
    private val coreColor = Color(0.45f, 0.96f, 0.22f, 1f)
    private val glowColor = Color(0.35f, 0.90f, 0.18f, 0.50f)

    fun init(startX: Float, startY: Float, velX: Float, velY: Float, dmg: Float) {
        active = true; x = startX; y = startY; vx = velX; vy = velY; damage = dmg
    }

    fun update(dt: Float) {
        if (!active) return
        vy += GameConfig.GRAVITY * 0.55f * dt; x += vx * dt; y += vy * dt
        if (y <= GameConfig.GROUND_Y || x < -40f) active = false
    }

    fun render(batch: SpriteBatch, assets: AssetManager) {
        if (!active) return
        assets.drawGlow(batch, x, y, 16f, glowColor)
        assets.drawCircle(batch, x, y, radius, coreColor)
    }

    override fun reset() { active = false }
}
