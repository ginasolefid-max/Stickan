package com.ironbow.stickmanarcher.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.ironbow.stickmanarcher.assets.AssetManager
import com.ironbow.stickmanarcher.config.GameConfig
import com.ironbow.stickmanarcher.data.AllyType
import kotlin.math.sin

class Ally(val type: AllyType) {
    var x = when (type) { AllyType.GUNNER -> 178f; AllyType.TRAP_LAYER -> 205f; AllyType.DOG -> 255f }
    var y = GameConfig.GROUND_Y; var actionTimer = 1.5f; var animTime = 0f; var dogReturning = false

    fun render(batch: SpriteBatch, assets: AssetManager) {
        when (type) {
            AllyType.GUNNER -> {
                val c = Color(0.24f, 0.36f, 0.48f, 1f)
                assets.drawLimb(batch, x, y + 30f, x - 8f, y + 2f, 5f, c)
                assets.drawLimb(batch, x, y + 30f, x + 8f, y + 2f, 5f, c)
                assets.drawLimb(batch, x, y + 30f, x, y + 60f, 6f, c)
                assets.drawCircle(batch, x, y + 73f, 10f, c)
                assets.drawLimb(batch, x + 2f, y + 54f, x + 32f, y + 56f, 4.5f, Color(0.22f, 0.24f, 0.28f, 1f))
            }
            AllyType.TRAP_LAYER -> {
                val c = Color(0.36f, 0.48f, 0.28f, 1f)
                assets.drawLimb(batch, x, y + 28f, x - 8f, y + 2f, 5f, c)
                assets.drawLimb(batch, x, y + 28f, x + 8f, y + 2f, 5f, c)
                assets.drawLimb(batch, x, y + 28f, x, y + 56f, 6f, c)
                assets.drawCircle(batch, x, y + 69f, 9.5f, c)
            }
            AllyType.DOG -> {
                val c = Color(0.58f, 0.40f, 0.24f, 1f)
                val stride = sin((animTime * 14f).toDouble()).toFloat() * 7f
                assets.drawLimb(batch, x - 14f, y + 18f, x + 14f, y + 18f, 8f, c)
                assets.drawLimb(batch, x - 10f, y + 16f, x - 10f - stride, y + 2f, 4f, c)
                assets.drawLimb(batch, x + 10f, y + 16f, x + 10f + stride, y + 2f, 4f, c)
                assets.drawCircle(batch, x + 18f, y + 24f, 7.5f, c)
                assets.drawLimb(batch, x + 15f, y + 29f, x + 12f, y + 36f, 3.5f, c)
                assets.drawLimb(batch, x - 14f, y + 20f, x - 22f, y + 27f, 3.5f, c)
            }
        }
    }
}

data class SpikeTrap(var x: Float = 0f, var charges: Int = 3, var cooldown: Float = 0f, var active: Boolean = true)
data class AllyBullet(var x: Float = 0f, var y: Float = 0f, var vx: Float = 0f, var vy: Float = 0f, var damage: Float = 18f, var active: Boolean = true)
