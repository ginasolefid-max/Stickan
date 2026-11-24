package com.ironbow.stickmanarcher.systems

import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.ironbow.stickmanarcher.config.GameConfig
import com.ironbow.stickmanarcher.data.HitZone
import com.ironbow.stickmanarcher.entities.Arrow
import com.ironbow.stickmanarcher.entities.Pickup
import com.ironbow.stickmanarcher.entities.Zombie
import kotlin.math.hypot

class PhysicsSystem {

    private val tmpHead = Vector2()
    private val tmpBody = Rectangle()
    private val tmpLegs = Rectangle()
    private val tmpPrev = Vector2()
    private val tmpCurr = Vector2()
    private val tmpNearest = Vector2()

    fun checkArrowZombieHit(arrow: Arrow, zombie: Zombie): HitZone? {
        if (!arrow.active || arrow.stuck || !zombie.active || arrow.hasHitZombie(zombie.id)) return null
        tmpPrev.set(arrow.prevX, arrow.prevY)
        tmpCurr.set(arrow.x, arrow.y)

        zombie.getHeadCenter(tmpHead)
        if (Intersector.distanceSegmentPoint(tmpPrev, tmpCurr, tmpHead) <= zombie.getHeadRadius() + 4f) return HitZone.HEAD
        zombie.getBodyBounds(tmpBody)
        if (segmentIntersectsRect(tmpPrev.x, tmpPrev.y, tmpCurr.x, tmpCurr.y, tmpBody)) return HitZone.BODY
        zombie.getLegBounds(tmpLegs)
        if (segmentIntersectsRect(tmpPrev.x, tmpPrev.y, tmpCurr.x, tmpCurr.y, tmpLegs)) return HitZone.LEGS
        return null
    }

    fun checkArrowPickupHit(arrow: Arrow, pickup: Pickup): Boolean {
        if (!arrow.active || arrow.stuck || !pickup.active) return false
        tmpPrev.set(arrow.prevX, arrow.prevY)
        tmpCurr.set(arrow.x, arrow.y)
        tmpNearest.set(pickup.x, pickup.y)
        return Intersector.distanceSegmentPoint(tmpPrev, tmpCurr, tmpNearest) <= pickup.radius + 6f
    }

    fun isNearFarTarget(arrow: Arrow, zombie: Zombie): Boolean {
        if (!arrow.active || arrow.stuck || !zombie.active) return false
        if (arrow.x - arrow.spawnX < GameConfig.BULLET_TIME_MIN_DISTANCE || arrow.vx <= 50f) return false
        zombie.getHeadCenter(tmpHead)
        val dist = hypot((tmpHead.x - arrow.x).toDouble(), (tmpHead.y - arrow.y).toDouble()).toFloat()
        return dist <= GameConfig.BULLET_TIME_TRIGGER_RADIUS && arrow.x < tmpHead.x + 25f
    }

    fun computeTrajectoryPoint(startX: Float, startY: Float, velX: Float, velY: Float, windX: Float, stepIndex: Int, out: Vector2): Vector2 {
        val t = stepIndex * GameConfig.TRAJECTORY_TIME_STEP
        return out.set(startX + velX * t + 0.5f * windX * t * t, startY + velY * t + 0.5f * GameConfig.GRAVITY * t * t)
    }

    private fun segmentIntersectsRect(x1: Float, y1: Float, x2: Float, y2: Float, r: Rectangle): Boolean {
        val steps = 4
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            if (r.contains(x1 + (x2 - x1) * t, y1 + (y2 - y1) * t)) return true
        }
        return false
    }
}
