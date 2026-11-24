package com.ironbow.stickmanarcher.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Pool
import com.ironbow.stickmanarcher.assets.AssetManager
import com.ironbow.stickmanarcher.config.GameConfig
import kotlin.math.cos
import kotlin.math.sin

class RagdollPiece : Pool.Poolable {
    var active = false; var x = 0f; var y = 0f; var vx = 0f; var vy = 0f
    var angle = 0f; var angularVel = 0f; var length = 24f; var thickness = 6f
    var isHead = false; var life = 2.2f; var maxLife = 2.2f
    val color = Color(Color.WHITE)

    fun init(px: Float, py: Float, velX: Float, velY: Float, rotVel: Float, len: Float, thick: Float, head: Boolean, col: Color) {
        active = true; x = px; y = py; vx = velX; vy = velY; angle = 0f; angularVel = rotVel
        length = len; thickness = thick; isHead = head; life = 2.2f; maxLife = 2.2f; color.set(col)
    }

    fun update(dt: Float) {
        if (!active) return
        life -= dt; if (life <= 0f) { active = false; return }
        vy += GameConfig.GRAVITY * dt; x += vx * dt; y += vy * dt; angle += angularVel * dt
        if (y <= GameConfig.GROUND_Y + 4f && vy < 0f) {
            y = GameConfig.GROUND_Y + 4f; vy *= -0.36f; vx *= 0.65f; angularVel *= 0.55f
        }
    }

    fun render(batch: SpriteBatch, assets: AssetManager) {
        if (!active) return
        color.a = (life / 0.8f).coerceIn(0f, 1f)
        if (isHead) assets.drawCircle(batch, x, y, length * 0.5f, color)
        else {
            val dx = cos(angle.toDouble()).toFloat() * length * 0.5f
            val dy = sin(angle.toDouble()).toFloat() * length * 0.5f
            assets.drawLimb(batch, x - dx, y - dy, x + dx, y + dy, thickness, color)
        }
    }

    override fun reset() { active = false }
}
