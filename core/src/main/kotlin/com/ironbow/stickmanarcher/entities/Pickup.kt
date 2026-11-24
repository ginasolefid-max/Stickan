package com.ironbow.stickmanarcher.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Pool
import com.ironbow.stickmanarcher.assets.AssetManager
import com.ironbow.stickmanarcher.config.GameConfig
import com.ironbow.stickmanarcher.data.ArrowType
import kotlin.math.sin

class Pickup : Pool.Poolable {
    var active = false; var x = 0f; var y = 0f; var vy = 0f; var radius = 24f
    var arrowType = ArrowType.FIRE; var amount = 3; var bonusCoins = 0; var lifeTimer = 14f; var bobTime = 0f

    fun init(spawnX: Float, spawnY: Float, type: ArrowType, qty: Int, coins: Int = 0) {
        active = true; x = spawnX; y = spawnY.coerceAtLeast(GameConfig.GROUND_Y + 26f); vy = -38f
        arrowType = type; amount = qty; bonusCoins = coins; lifeTimer = 14f; bobTime = 0f
    }

    fun update(dt: Float) {
        if (!active) return
        bobTime += dt; lifeTimer -= dt
        if (lifeTimer <= 0f) { active = false; return }
        if (y > GameConfig.GROUND_Y + 26f) {
            y += vy * dt
            if (y < GameConfig.GROUND_Y + 26f) y = GameConfig.GROUND_Y + 26f
        }
    }

    fun render(batch: SpriteBatch, assets: AssetManager) {
        if (!active) return
        val bobY = y + sin((bobTime * 4f).toDouble()).toFloat() * 4f
        val alpha = if (lifeTimer < 2.5f) (lifeTimer / 2.5f).coerceIn(0.2f, 1f) else 1f
        assets.drawGlow(batch, x, bobY, 32f, Color(arrowType.colorHex.toInt()).apply { a = alpha * 0.55f })
        assets.drawRect(batch, x - 16f, bobY - 16f, 32f, 32f, Color(0.55f, 0.38f, 0.20f, alpha))
        assets.drawRect(batch, x - 12f, bobY - 12f, 24f, 24f, Color(0.32f, 0.22f, 0.12f, alpha))
        assets.drawLimb(batch, x - 8f, bobY - 8f, x + 8f, bobY + 8f, 3.5f, Color(arrowType.colorHex.toInt()).apply { a = alpha })
    }

    override fun reset() { active = false }
}
