package com.ironbow.stickmanarcher.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Pool
import com.ironbow.stickmanarcher.assets.AssetManager
import com.ironbow.stickmanarcher.config.GameConfig
import com.ironbow.stickmanarcher.data.ArrowTrail
import com.ironbow.stickmanarcher.data.ArrowType
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class Arrow : Pool.Poolable {

    var active = false
    var x = 0f; var y = 0f; var prevX = 0f; var prevY = 0f
    var vx = 0f; var vy = 0f; var angleRad = 0f

    var type = ArrowType.NORMAL
    var trail = ArrowTrail.NONE
    var damage = 25f
    var isCrit = false

    var stuck = false
    var stuckInGround = false
    var stuckZombieId = -1
    var stuckOffsetX = 0f
    var stuckOffsetY = 0f
    var stuckRelAngle = 0f

    var pierceRemaining = 0
    val hitZombieIds = IntArray(8)
    var hitCount = 0

    var spawnX = 0f; var flightTime = 0f; var lifeTimer = 0f; var trailTimer = 0f

    private val shaftColor = Color(0.84f, 0.70f, 0.45f, 1f)
    private val tipColor = Color(0.88f, 0.90f, 0.96f, 1f)
    private val fletchColor = Color(0.88f, 0.24f, 0.24f, 1f)
    private val auraColor = Color()

    fun init(startX: Float, startY: Float, velX: Float, velY: Float, arrowType: ArrowType, arrowTrail: ArrowTrail, dmg: Float, crit: Boolean) {
        active = true
        x = startX; y = startY; prevX = startX; prevY = startY; spawnX = startX
        vx = velX; vy = velY
        angleRad = atan2(velY.toDouble(), velX.toDouble()).toFloat()
        type = arrowType; trail = arrowTrail; damage = dmg; isCrit = crit
        stuck = false; stuckInGround = false; stuckZombieId = -1
        pierceRemaining = if (arrowType == ArrowType.PIERCING) 3 else 0
        hitCount = 0
        for (i in hitZombieIds.indices) hitZombieIds[i] = -1
        flightTime = 0f; lifeTimer = 0f; trailTimer = 0f
    }

    fun hasHitZombie(zombieId: Int): Boolean {
        for (i in 0 until hitCount) if (hitZombieIds[i] == zombieId) return true
        return false
    }

    fun markZombieHit(zombieId: Int) {
        if (hitCount < hitZombieIds.size) hitZombieIds[hitCount++] = zombieId
    }

    fun stickToGround(groundY: Float) {
        stuck = true; stuckInGround = true; y = groundY + 2f; vx = 0f; vy = 0f; lifeTimer = 5.5f
    }

    fun stickToZombie(zombie: Zombie, hitX: Float, hitY: Float) {
        stuck = true; stuckInGround = false; stuckZombieId = zombie.id
        stuckOffsetX = hitX - zombie.x; stuckOffsetY = hitY - zombie.y; stuckRelAngle = angleRad
        vx = 0f; vy = 0f; lifeTimer = 4.0f
    }

    fun update(dt: Float, windX: Float) {
        if (!active) return
        if (stuck) {
            lifeTimer -= dt
            if (lifeTimer <= 0f) active = false
            return
        }
        prevX = x; prevY = y; flightTime += dt
        vx += windX * dt
        vy += GameConfig.GRAVITY * dt
        x += vx * dt; y += vy * dt
        if (vx != 0f || vy != 0f) angleRad = atan2(vy.toDouble(), vx.toDouble()).toFloat()
        if (x > GameConfig.WORLD_WIDTH + 240f || x < -200f || y < -120f) active = false
    }

    fun render(batch: SpriteBatch, assets: AssetManager) {
        if (!active) return
        val dirX = cos(angleRad.toDouble()).toFloat()
        val dirY = sin(angleRad.toDouble()).toFloat()
        val tailX = x - dirX * 38f; val tailY = y - dirY * 38f
        val alpha = if (stuck && lifeTimer < 1.0f) lifeTimer.coerceIn(0f, 1f) else 1f
        shaftColor.a = alpha
        assets.drawLimb(batch, tailX, tailY, x, y, 3.2f, shaftColor)

        fletchColor.set(Color(type.colorHex.toInt())); fletchColor.a = alpha
        val perpX = -dirY; val perpY = dirX
        assets.drawLimb(batch, tailX, tailY, tailX - dirX * 7f + perpX * 5f, tailY - dirY * 7f + perpY * 5f, 2.4f, fletchColor)
        assets.drawLimb(batch, tailX, tailY, tailX - dirX * 7f - perpX * 5f, tailY - dirY * 7f - perpY * 5f, 2.4f, fletchColor)

        tipColor.set(when (type) {
            ArrowType.FIRE -> Color(1f, 0.45f, 0.15f, alpha)
            ArrowType.ICE -> Color(0.45f, 0.90f, 1f, alpha)
            ArrowType.EXPLOSIVE -> Color(1f, 0.22f, 0.25f, alpha)
            ArrowType.POISON -> Color(0.40f, 0.95f, 0.30f, alpha)
            ArrowType.PIERCING -> Color(0.88f, 0.92f, 1f, alpha)
            else -> Color(0.86f, 0.88f, 0.92f, alpha)
        })
        assets.drawLimb(batch, x - dirX * 6f, y - dirY * 6f, x + dirX * 5f, y + dirY * 5f, 5.2f, tipColor)

        if (!stuck && type != ArrowType.NORMAL) {
            auraColor.set(tipColor.r, tipColor.g, tipColor.b, 0.45f)
            assets.drawGlow(batch, x, y, 16f, auraColor)
        }
    }

    override fun reset() {
        active = false; stuck = false; stuckZombieId = -1; hitCount = 0; pierceRemaining = 0
    }
}
