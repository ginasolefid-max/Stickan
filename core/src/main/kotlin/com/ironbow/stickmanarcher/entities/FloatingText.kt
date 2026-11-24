package com.ironbow.stickmanarcher.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Pool
import com.ironbow.stickmanarcher.assets.AssetManager

class FloatingText : Pool.Poolable {
    var active = false; var text = ""; var x = 0f; var y = 0f; var vy = 52f
    var life = 0.9f; var maxLife = 0.9f; var large = false
    val color = Color(Color.WHITE)

    fun init(msg: String, px: Float, py: Float, col: Color, isLarge: Boolean = false, duration: Float = 0.95f) {
        active = true; text = msg; x = px; y = py; vy = if (isLarge) 68f else 48f
        life = duration; maxLife = duration; large = isLarge; color.set(col)
    }

    fun update(dt: Float) {
        if (!active) return
        life -= dt; if (life <= 0f) { active = false; return }
        y += vy * dt; vy *= 0.96f
    }

    fun render(batch: SpriteBatch, assets: AssetManager) {
        if (!active) return
        val alpha = (life / maxLife).coerceIn(0f, 1f)
        val font = if (large) assets.fontLarge else assets.fontMedium
        font.setColor(0f, 0f, 0f, alpha * 0.7f)
        font.draw(batch, text, x + 2f, y - 2f)
        font.setColor(color.r, color.g, color.b, alpha)
        font.draw(batch, text, x, y)
        font.color = Color.WHITE
    }

    override fun reset() { active = false; text = "" }
}
