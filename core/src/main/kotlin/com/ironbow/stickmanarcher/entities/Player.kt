package com.ironbow.stickmanarcher.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.ironbow.stickmanarcher.assets.AssetManager
import com.ironbow.stickmanarcher.config.GameConfig
import com.ironbow.stickmanarcher.data.BowSkin
import com.ironbow.stickmanarcher.data.BowType
import com.ironbow.stickmanarcher.data.StickmanSkin
import kotlin.math.cos
import kotlin.math.sin

class Player {

    enum class AnimState { IDLE, DRAW, RELEASE, HURT, DIE }

    var x = GameConfig.PLAYER_X; var y = GameConfig.GROUND_Y
    var hp = GameConfig.BASE_PLAYER_HP; var maxHp = GameConfig.BASE_PLAYER_HP
    var state = AnimState.IDLE
    var aimAngleRad = 0.35f; var drawPower = 0f; var swayTime = 0f; var stateTimer = 0f; var hurtFlashTimer = 0f
    var skin = StickmanSkin.DEFAULT; var bowType = BowType.WOODEN; var bowSkin = BowSkin.CLASSIC

    fun resetForMatch(maxHealth: Float, equippedSkin: StickmanSkin, equippedBow: BowType, equippedBowSkin: BowSkin) {
        maxHp = maxHealth; hp = maxHealth; skin = equippedSkin; bowType = equippedBow; bowSkin = equippedBowSkin
        state = AnimState.IDLE; aimAngleRad = 0.35f; drawPower = 0f; stateTimer = 0f; hurtFlashTimer = 0f
    }

    fun getEffectiveAimAngle(): Float = aimAngleRad + sin((swayTime * 4.2f).toDouble()).toFloat() * ((1f - bowType.accuracy) * 0.14f)

    fun applyDamage(amount: Float) {
        if (state == AnimState.DIE) return
        hp = (hp - amount).coerceAtLeast(0f); hurtFlashTimer = 0.25f
        if (hp <= 0f) { state = AnimState.DIE; stateTimer = 0f }
        else if (state != AnimState.DRAW) { state = AnimState.HURT; stateTimer = 0.22f }
    }

    fun triggerRelease() { state = AnimState.RELEASE; stateTimer = 0.18f; drawPower = 0f }

    fun update(dt: Float) {
        swayTime += dt
        if (hurtFlashTimer > 0f) hurtFlashTimer = (hurtFlashTimer - dt).coerceAtLeast(0f)
        if (state == AnimState.RELEASE || state == AnimState.HURT) {
            stateTimer -= dt
            if (stateTimer <= 0f && hp > 0f) state = AnimState.IDLE
        } else if (state == AnimState.DIE) {
            stateTimer = (stateTimer + dt).coerceAtMost(1f)
        }
    }

    fun render(batch: SpriteBatch, assets: AssetManager) {
        val bodyCol = when (skin) {
            StickmanSkin.DEFAULT -> Color(0.14f, 0.16f, 0.20f, 1f)
            StickmanSkin.NINJA -> Color(0.10f, 0.10f, 0.14f, 1f)
            StickmanSkin.KNIGHT -> Color(0.55f, 0.60f, 0.68f, 1f)
            StickmanSkin.ROBOT -> Color(0.24f, 0.64f, 0.78f, 1f)
            StickmanSkin.PIRATE -> Color(0.28f, 0.20f, 0.16f, 1f)
        }
        if (hurtFlashTimer > 0f) bodyCol.lerp(Color.RED, 0.65f)

        val breath = sin((swayTime * 2.4f).toDouble()).toFloat() * 1.8f
        val dieCollapse = if (state == AnimState.DIE) stateTimer else 0f
        val hurtTilt = if (state == AnimState.HURT) -8f else 0f

        val hipX = x; val hipY = y + 36f * (1f - dieCollapse * 0.75f)
        val shoulderX = x + hurtTilt - drawPower * 6f + dieCollapse * 28f
        val shoulderY = y + (72f + breath) * (1f - dieCollapse * 0.78f)
        val headX = shoulderX + 2f; val headY = shoulderY + 17f

        assets.drawCircle(batch, x, GameConfig.GROUND_Y + 2f, 20f, Color(0f, 0f, 0f, 0.30f))
        assets.drawLimb(batch, hipX, hipY, x - 12f, y + 2f, 6.2f, bodyCol)
        assets.drawLimb(batch, hipX, hipY, x + 13f, y + 2f, 6.2f, bodyCol)
        assets.drawLimb(batch, hipX, hipY, shoulderX, shoulderY, 7.2f, bodyCol)
        assets.drawCircle(batch, headX, headY, 12.5f, bodyCol)

        when (skin) {
            StickmanSkin.NINJA -> {
                val band = Color(0.90f, 0.18f, 0.20f, 1f)
                assets.drawLimb(batch, headX - 11f, headY + 4f, headX + 11f, headY + 4f, 4.5f, band)
                assets.drawLimb(batch, headX - 10f, headY + 4f, headX - 24f, headY - 2f + breath, 3.5f, band)
            }
            StickmanSkin.KNIGHT -> assets.drawRect(batch, headX - 4f, headY - 2f, 14f, 5f, Color(0.84f, 0.88f, 0.94f, 1f))
            StickmanSkin.ROBOT -> assets.drawGlow(batch, headX + 5f, headY + 2f, 8f, Color(0.2f, 1f, 0.95f, 0.9f))
            StickmanSkin.PIRATE -> {
                val hat = Color(0.15f, 0.12f, 0.14f, 1f)
                assets.drawLimb(batch, headX - 15f, headY + 9f, headX + 15f, headY + 9f, 6f, hat)
                assets.drawCircle(batch, headX, headY + 13f, 7f, hat)
            }
            StickmanSkin.DEFAULT -> assets.drawLimb(batch, shoulderX - 4f, shoulderY + 3f, shoulderX - 18f, shoulderY - 6f + breath, 4.5f, Color(0.85f, 0.35f, 0.20f, 1f))
        }

        if (state == AnimState.DIE) return

        val angle = if (state == AnimState.DRAW) getEffectiveAimAngle() else aimAngleRad
        val dirX = cos(angle.toDouble()).toFloat(); val dirY = sin(angle.toDouble()).toFloat()
        val perpX = -dirY; val perpY = dirX
        val bowCenterX = shoulderX + dirX * 26f; val bowCenterY = shoulderY + dirY * 26f
        assets.drawLimb(batch, shoulderX, shoulderY, bowCenterX, bowCenterY, 5.2f, bodyCol)

        val bowHalfSpan = when (bowType) { BowType.CROSSBOW -> 14f; BowType.LEGENDARY -> 26f; else -> 22f }
        val curveBack = 7f + drawPower * 5f
        val topTipX = bowCenterX + perpX * bowHalfSpan - dirX * curveBack
        val topTipY = bowCenterY + perpY * bowHalfSpan - dirY * curveBack
        val botTipX = bowCenterX - perpX * bowHalfSpan - dirX * curveBack
        val botTipY = bowCenterY - perpY * bowHalfSpan - dirY * curveBack
        val pullDist = 8f + drawPower * 22f
        val stringMidX = bowCenterX - dirX * (curveBack + pullDist)
        val stringMidY = bowCenterY - dirY * (curveBack + pullDist)

        assets.drawLimb(batch, shoulderX, shoulderY - 2f, stringMidX, stringMidY, 5.0f, bodyCol)
        val stringColor = Color(0.92f, 0.92f, 0.95f, 0.9f)
        assets.drawLimb(batch, topTipX, topTipY, stringMidX, stringMidY, 1.8f, stringColor)
        assets.drawLimb(batch, botTipX, botTipY, stringMidX, stringMidY, 1.8f, stringColor)

        val bowColor = when (bowSkin) {
            BowSkin.CLASSIC -> Color(0.58f, 0.36f, 0.18f, 1f)
            BowSkin.CRIMSON -> Color(0.84f, 0.18f, 0.18f, 1f)
            BowSkin.SHADOW -> Color(0.18f, 0.18f, 0.24f, 1f)
            BowSkin.EMERALD -> Color(0.18f, 0.72f, 0.42f, 1f)
            BowSkin.GOLD_RUNE -> Color(0.95f, 0.76f, 0.22f, 1f)
        }
        assets.drawLimb(batch, topTipX, topTipY, bowCenterX + dirX * 4f, bowCenterY + dirY * 4f, 4.6f, bowColor)
        assets.drawLimb(batch, botTipX, botTipY, bowCenterX + dirX * 4f, bowCenterY + dirY * 4f, 4.6f, bowColor)

        if (state == AnimState.DRAW) {
            assets.drawLimb(batch, stringMidX, stringMidY, stringMidX + dirX * 36f, stringMidY + dirY * 36f, 3f, Color(0.92f, 0.80f, 0.50f, 1f))
        }
    }
}
