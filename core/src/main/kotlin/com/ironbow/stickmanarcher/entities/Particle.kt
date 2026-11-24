package com.ironbow.stickmanarcher.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Pool
import com.ironbow.stickmanarcher.assets.AssetManager
import com.ironbow.stickmanarcher.config.GameConfig

class Particle : Pool.Poolable {
    var active = false; var x = 0f; var y = 0f; var vx = 0f; var vy = 0f
    var gravityScale = 0.5f; var size = 5f; var life = 0.5f; var maxLife = 0.5f; var glow = false
    val color = Color(Color.WHITE)

    fun init(px: Float, py: Float, velX: Float, velY: Float, pSize: Float, duration: Float, pColor: Color, gravScale: Float = 0.5f, isGlow: Boolean = false) {
        active = true; x = px; y = py; vx = velX; vy = velY; size = pSize; life = duration; maxLife = duration
        color.set(pColor); gravityScale = gravScale; glow = isGlow
    }

    fun update(dt: Float) {
        if (!active) return
        life -= dt; if (life <= 0f) { active = false; return }
        vy += GameConfig.GRAVITY * gravityScale * dt; x += vx * dt; y += vy * dt
        if (y < GameConfig.GROUND_Y && vy < 0f) { y = GameConfig.GROUND_Y; vy *= -0.35f; vx *= 0.72f }
    }

    fun render(batch: SpriteBatch, assets: AssetManager) {
        if (!active) return
        val alpha = (life / maxLife).coerceIn(0f, 1f)
        color.a = alpha
        if (glow) assets.drawGlow(batch, x, y, size * (0.6f + 0.4f * alpha), color)
        else assets.drawCircle(batch, x, y, size * 0.5f, color)
    }

    override fun reset() { active = false }
}
