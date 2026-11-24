package com.ironbow.stickmanarcher.systems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.ironbow.stickmanarcher.assets.AssetManager
import com.ironbow.stickmanarcher.config.GameConfig
import com.ironbow.stickmanarcher.data.BiomeType
import kotlin.math.cos
import kotlin.math.sin

class BiomeRenderer {

    private var timeOfDay = 0.22f
    private var fogOffset = 0f
    private val skyTop = Color()
    private val skyBottom = Color()
    private val hillFar = Color()
    private val hillNear = Color()
    private val groundCol = Color()

    fun reset(biome: BiomeType) {
        timeOfDay = when (biome) {
            BiomeType.GRAVEYARD -> 0.72f
            BiomeType.FOREST -> 0.38f
            BiomeType.CITY_RUINS -> 0.56f
            BiomeType.SNOW -> 0.22f
        }
        fogOffset = 0f
    }

    fun update(dt: Float) {
        timeOfDay = (timeOfDay + dt * 0.012f) % 1.0f
        fogOffset = (fogOffset + dt * 26f) % GameConfig.WORLD_WIDTH
    }

    fun render(batch: SpriteBatch, assets: AssetManager, biome: BiomeType, cameraOffsetX: Float) {
        val nightFactor = ((sin((timeOfDay * 6.283f - 1.57f).toDouble()).toFloat() + 1f) * 0.5f)
        when (biome) {
            BiomeType.GRAVEYARD -> {
                skyTop.set(0.10f, 0.12f, 0.20f, 1f).lerp(Color(0.03f, 0.04f, 0.09f, 1f), nightFactor)
                skyBottom.set(0.24f, 0.20f, 0.32f, 1f).lerp(Color(0.10f, 0.12f, 0.20f, 1f), nightFactor)
                hillFar.set(0.14f, 0.16f, 0.22f, 1f); hillNear.set(0.09f, 0.11f, 0.16f, 1f); groundCol.set(0.12f, 0.14f, 0.13f, 1f)
            }
            BiomeType.FOREST -> {
                skyTop.set(0.16f, 0.36f, 0.48f, 1f).lerp(Color(0.04f, 0.08f, 0.15f, 1f), nightFactor)
                skyBottom.set(0.45f, 0.58f, 0.55f, 1f).lerp(Color(0.08f, 0.15f, 0.18f, 1f), nightFactor)
                hillFar.set(0.12f, 0.24f, 0.20f, 1f); hillNear.set(0.08f, 0.18f, 0.14f, 1f); groundCol.set(0.15f, 0.24f, 0.14f, 1f)
            }
            BiomeType.CITY_RUINS -> {
                skyTop.set(0.32f, 0.26f, 0.24f, 1f).lerp(Color(0.06f, 0.06f, 0.10f, 1f), nightFactor)
                skyBottom.set(0.62f, 0.36f, 0.24f, 1f).lerp(Color(0.16f, 0.12f, 0.16f, 1f), nightFactor)
                hillFar.set(0.20f, 0.18f, 0.20f, 1f); hillNear.set(0.14f, 0.13f, 0.15f, 1f); groundCol.set(0.22f, 0.20f, 0.19f, 1f)
            }
            BiomeType.SNOW -> {
                skyTop.set(0.24f, 0.44f, 0.66f, 1f).lerp(Color(0.05f, 0.10f, 0.20f, 1f), nightFactor)
                skyBottom.set(0.68f, 0.80f, 0.90f, 1f).lerp(Color(0.16f, 0.24f, 0.38f, 1f), nightFactor)
                hillFar.set(0.55f, 0.68f, 0.78f, 1f).lerp(Color(0.18f, 0.26f, 0.38f, 1f), nightFactor)
                hillNear.set(0.42f, 0.55f, 0.66f, 1f).lerp(Color(0.14f, 0.20f, 0.30f, 1f), nightFactor)
                groundCol.set(0.84f, 0.90f, 0.96f, 1f).lerp(Color(0.42f, 0.52f, 0.64f, 1f), nightFactor)
            }
        }

        val bands = 10
        val bandH = (GameConfig.WORLD_HEIGHT - GameConfig.GROUND_Y) / bands
        val tmpSky = Color()
        for (i in 0 until bands) {
            tmpSky.set(skyBottom).lerp(skyTop, i.toFloat() / (bands - 1))
            assets.drawRect(batch, -80f, GameConfig.GROUND_Y + i * bandH, GameConfig.WORLD_WIDTH + 160f, bandH + 2f, tmpSky)
        }

        if (nightFactor > 0.35f) {
            val starCol = Color(1f, 1f, 0.92f, ((nightFactor - 0.35f) * 1.4f).coerceIn(0f, 0.9f))
            for (s in 0 until 24) assets.drawCircle(batch, ((s * 173) % 1220) + 30f, 360f + ((s * 97) % 330), if (s % 4 == 0) 2.2f else 1.4f, starCol)
        }

        val celestialAngle = timeOfDay * 6.283f
        val cx = GameConfig.WORLD_WIDTH * 0.5f + cos(celestialAngle.toDouble()).toFloat() * 440f
        val cy = GameConfig.GROUND_Y + 260f + sin(celestialAngle.toDouble()).toFloat() * 210f
        val bodyCol = if (nightFactor > 0.5f) Color(0.90f, 0.94f, 1f, 0.92f) else Color(1f, 0.86f, 0.45f, 0.95f)
        assets.drawGlow(batch, cx, cy, 72f, Color(bodyCol.r, bodyCol.g, bodyCol.b, 0.25f))
        assets.drawCircle(batch, cx, cy, 26f, bodyCol)

        val farShift = -cameraOffsetX * 0.15f
        for (i in -1..7) assets.drawCircle(batch, i * 220f + farShift + 110f, GameConfig.GROUND_Y - 20f, 180f, hillFar)

        val midShift = -cameraOffsetX * 0.35f
        for (i in 0..8) {
            val px = i * 165f + midShift
            when (biome) {
                BiomeType.GRAVEYARD -> {
                    assets.drawRect(batch, px + 40f, GameConfig.GROUND_Y, 22f, 44f, hillNear)
                    assets.drawCircle(batch, px + 51f, GameConfig.GROUND_Y + 44f, 11f, hillNear)
                    if (i % 2 == 0) {
                        assets.drawLimb(batch, px + 100f, GameConfig.GROUND_Y, px + 100f, GameConfig.GROUND_Y + 110f, 8f, hillNear)
                        assets.drawLimb(batch, px + 100f, GameConfig.GROUND_Y + 70f, px + 76f, GameConfig.GROUND_Y + 95f, 4f, hillNear)
                        assets.drawLimb(batch, px + 100f, GameConfig.GROUND_Y + 85f, px + 124f, GameConfig.GROUND_Y + 108f, 4f, hillNear)
                    }
                }
                BiomeType.FOREST -> {
                    assets.drawRect(batch, px + 50f, GameConfig.GROUND_Y, 12f, 95f, hillNear)
                    assets.drawCircle(batch, px + 56f, GameConfig.GROUND_Y + 115f, 38f, hillNear)
                    assets.drawCircle(batch, px + 56f, GameConfig.GROUND_Y + 145f, 26f, hillNear)
                }
                BiomeType.CITY_RUINS -> {
                    val h = 110f + ((i * 53) % 130)
                    assets.drawRect(batch, px + 20f, GameConfig.GROUND_Y, 72f, h, hillNear)
                    assets.drawRect(batch, px + 34f, GameConfig.GROUND_Y + h - 30f, 16f, 18f, skyTop)
                }
                BiomeType.SNOW -> {
                    assets.drawLimb(batch, px, GameConfig.GROUND_Y, px + 75f, GameConfig.GROUND_Y + 155f, 24f, hillNear)
                    assets.drawLimb(batch, px + 75f, GameConfig.GROUND_Y + 155f, px + 150f, GameConfig.GROUND_Y, 24f, hillNear)
                }
            }
        }

        assets.drawRect(batch, -80f, 0f, GameConfig.WORLD_WIDTH + 160f, GameConfig.GROUND_Y, groundCol)
        assets.drawRect(batch, -80f, GameConfig.GROUND_Y - 5f, GameConfig.WORLD_WIDTH + 160f, 6f, Color(groundCol).mul(0.75f).apply { a = 1f })

        val fogCol = Color(0.82f, 0.88f, 0.95f, if (biome == BiomeType.GRAVEYARD) 0.14f else 0.08f)
        for (i in -1..5) {
            val fx = ((i * 300f + fogOffset) % (GameConfig.WORLD_WIDTH + 300f)) - 150f
            assets.drawGlow(batch, fx, GameConfig.GROUND_Y + 28f, 140f, fogCol)
        }
    }
}
