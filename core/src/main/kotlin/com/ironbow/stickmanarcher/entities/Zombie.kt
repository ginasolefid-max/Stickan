package com.ironbow.stickmanarcher.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Pool
import com.ironbow.stickmanarcher.assets.AssetManager
import com.ironbow.stickmanarcher.config.GameConfig
import com.ironbow.stickmanarcher.data.ZombieType
import kotlin.math.cos
import kotlin.math.sin

class Zombie : Pool.Poolable {

    var active = false; var id = 0; var type = ZombieType.WALKER
    var x = 0f; var y = GameConfig.GROUND_Y; var vy = 0f
    var hp = 50f; var maxHp = 50f; var speed = 50f; var attackDamage = 10f; var scale = 1.0f

    var walkCycle = 0f; var attackCooldown = 0f; var hurtFlashTimer = 0f
    var isAirborne = false; var jumpCooldown = 2.0f; var spitCooldown = 2.4f

    var legSlowTimer = 0f; var freezeTimer = 0f; var burnTimer = 0f; var poisonTimer = 0f; var statusTickAccum = 0f
    var bossPatternTimer = 3.5f; var bossPhase = 0

    private val bodyColor = Color()
    private val limbColor = Color()
    private val detailColor = Color()
    private val hpBgColor = Color(0.12f, 0.12f, 0.16f, 0.85f)
    private val hpFillColor = Color(0.88f, 0.22f, 0.24f, 0.95f)

    fun init(uniqueId: Int, zombieType: ZombieType, spawnX: Float, waveScale: Float) {
        active = true; id = uniqueId; type = zombieType; x = spawnX; y = GameConfig.GROUND_Y; vy = 0f
        maxHp = zombieType.baseHp * waveScale; hp = maxHp
        speed = zombieType.baseSpeed * (1f + ((waveScale - 1f) * 0.12f).coerceAtMost(0.35f))
        attackDamage = zombieType.attackDamage * (1f + (waveScale - 1f) * 0.18f)
        scale = zombieType.scale
        walkCycle = (uniqueId % 7) * 0.6f
        attackCooldown = 0.4f; hurtFlashTimer = 0f; isAirborne = false
        jumpCooldown = 1.8f; spitCooldown = 2.2f
        legSlowTimer = 0f; freezeTimer = 0f; burnTimer = 0f; poisonTimer = 0f; statusTickAccum = 0f
        bossPatternTimer = 4.0f; bossPhase = 0
    }

    fun currentSpeed(): Float {
        var mult = 1f
        if (freezeTimer > 0f) mult *= GameConfig.ICE_SLOW_FACTOR
        if (legSlowTimer > 0f) mult *= GameConfig.LEG_SLOW_FACTOR
        return speed * mult
    }

    fun getHeadCenter(out: Vector2): Vector2 {
        val bob = sin((walkCycle * 2f).toDouble()).toFloat() * 3f * scale
        val lean = if (type == ZombieType.RUNNER) -9f else -4f
        return out.set(x + lean * scale, y + 84f * scale + bob)
    }

    fun getHeadRadius(): Float = 15.5f * scale

    fun getBodyBounds(out: Rectangle): Rectangle {
        val w = 28f * scale; val h = 38f * scale
        return out.set(x - w * 0.5f, y + 32f * scale, w, h)
    }

    fun getLegBounds(out: Rectangle): Rectangle {
        val w = 30f * scale; val h = 32f * scale
        return out.set(x - w * 0.5f, y, w, h)
    }

    fun applyDamage(rawDamage: Float, piercing: Boolean): Float {
        var finalDamage = rawDamage
        if (type == ZombieType.TANK && !piercing) finalDamage *= 0.68f
        hp -= finalDamage
        hurtFlashTimer = 0.14f
        return finalDamage
    }

    fun updateTimers(dt: Float) {
        if (hurtFlashTimer > 0f) hurtFlashTimer = (hurtFlashTimer - dt).coerceAtLeast(0f)
        if (attackCooldown > 0f) attackCooldown = (attackCooldown - dt).coerceAtLeast(0f)
        if (legSlowTimer > 0f) legSlowTimer = (legSlowTimer - dt).coerceAtLeast(0f)
        if (freezeTimer > 0f) freezeTimer = (freezeTimer - dt).coerceAtLeast(0f)
        if (burnTimer > 0f) burnTimer = (burnTimer - dt).coerceAtLeast(0f)
        if (poisonTimer > 0f) poisonTimer = (poisonTimer - dt).coerceAtLeast(0f)
        if (jumpCooldown > 0f) jumpCooldown = (jumpCooldown - dt).coerceAtLeast(0f)
        if (spitCooldown > 0f) spitCooldown = (spitCooldown - dt).coerceAtLeast(0f)
        if (type == ZombieType.BOSS) bossPatternTimer -= dt

        if (isAirborne) {
            vy += GameConfig.GRAVITY * dt
            y += vy * dt
            if (y <= GameConfig.GROUND_Y) { y = GameConfig.GROUND_Y; vy = 0f; isAirborne = false }
        }
    }

    fun render(batch: SpriteBatch, assets: AssetManager) {
        if (!active) return
        val s = scale
        val stride = sin(walkCycle.toDouble()).toFloat() * 14f * s
        val armSwing = cos(walkCycle.toDouble()).toFloat() * 8f * s
        val bob = sin((walkCycle * 2f).toDouble()).toFloat() * 3f * s

        when (type) {
            ZombieType.WALKER -> bodyColor.set(0.32f, 0.56f, 0.34f, 1f)
            ZombieType.RUNNER -> bodyColor.set(0.70f, 0.24f, 0.24f, 1f)
            ZombieType.TANK -> bodyColor.set(0.36f, 0.42f, 0.48f, 1f)
            ZombieType.JUMPER -> bodyColor.set(0.56f, 0.34f, 0.68f, 1f)
            ZombieType.SPITTER -> bodyColor.set(0.42f, 0.78f, 0.22f, 1f)
            ZombieType.EXPLODER -> bodyColor.set(0.88f, 0.42f, 0.16f, 1f)
            ZombieType.BOSS -> bodyColor.set(0.46f, 0.12f, 0.22f, 1f)
        }
        if (freezeTimer > 0f) bodyColor.lerp(Color.CYAN, 0.52f)
        else if (poisonTimer > 0f) bodyColor.lerp(Color.LIME, 0.35f)
        else if (burnTimer > 0f) bodyColor.lerp(Color.ORANGE, 0.40f)
        if (hurtFlashTimer > 0f) bodyColor.lerp(Color.WHITE, 0.75f)

        limbColor.set(bodyColor).mul(0.86f); limbColor.a = 1f
        val hipX = x; val hipY = y + 34f * s + bob
        val shoulderX = x - 4f * s; val shoulderY = y + 68f * s + bob
        val headX = shoulderX - 2f * s; val headY = y + 84f * s + bob
        val thick = (if (type == ZombieType.TANK || type == ZombieType.BOSS) 8.5f else 5.8f) * s

        assets.drawCircle(batch, x, GameConfig.GROUND_Y + 2f, 18f * s, Color(0f, 0f, 0f, 0.28f))
        assets.drawLimb(batch, hipX, hipY, x - stride, y + 2f, thick, limbColor)
        assets.drawLimb(batch, hipX, hipY, x + stride, y + 2f, thick, bodyColor)
        assets.drawLimb(batch, hipX, hipY, shoulderX, shoulderY, thick * 1.2f, bodyColor)
        assets.drawLimb(batch, shoulderX, shoulderY, shoulderX - 24f * s, shoulderY - 6f * s + armSwing * 0.5f, thick * 0.85f, limbColor)
        assets.drawLimb(batch, shoulderX, shoulderY - 3f * s, shoulderX - 28f * s, shoulderY - 12f * s - armSwing * 0.5f, thick * 0.85f, bodyColor)
        assets.drawCircle(batch, headX, headY, 12.5f * s, bodyColor)

        detailColor.set(when (type) {
            ZombieType.SPITTER -> Color.GREEN; ZombieType.EXPLODER -> Color.YELLOW; ZombieType.BOSS -> Color.RED
            else -> Color(1f, 0.35f, 0.25f, 1f)
        })
        assets.drawCircle(batch, headX - 5f * s, headY + 2f * s, 2.7f * s, detailColor)

        if (type == ZombieType.TANK) {
            assets.drawLimb(batch, headX - 11f * s, headY + 7f * s, headX + 10f * s, headY + 7f * s, 6f * s, Color(0.65f, 0.72f, 0.78f, 1f))
            assets.drawRect(batch, x - 16f * s, y + 38f * s, 8f * s, 30f * s, Color(0.55f, 0.62f, 0.70f, 1f))
        } else if (type == ZombieType.EXPLODER) {
            assets.drawGlow(batch, shoulderX + 2f, shoulderY - 12f * s, 18f * s, Color(1f, 0.55f, 0.1f, 0.55f))
        } else if (type == ZombieType.BOSS) {
            val crown = Color(0.95f, 0.72f, 0.15f, 1f)
            assets.drawLimb(batch, headX - 6f * s, headY + 10f * s, headX - 12f * s, headY + 22f * s, 4f * s, crown)
            assets.drawLimb(batch, headX + 4f * s, headY + 10f * s, headX + 8f * s, headY + 22f * s, 4f * s, crown)
        }

        if (type != ZombieType.BOSS && hp < maxHp) {
            val barW = 42f * s; val bx = x - barW * 0.5f; val by = headY + 20f * s
            assets.drawRect(batch, bx - 1f, by - 1f, barW + 2f, 7f, hpBgColor)
            assets.drawRect(batch, bx, by, barW * (hp / maxHp).coerceIn(0f, 1f), 5f, hpFillColor)
        }
    }

    override fun reset() {
        active = false; isAirborne = false; vy = 0f; hurtFlashTimer = 0f
        freezeTimer = 0f; burnTimer = 0f; poisonTimer = 0f; legSlowTimer = 0f
    }
}
