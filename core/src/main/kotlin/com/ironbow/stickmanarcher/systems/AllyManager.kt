package com.ironbow.stickmanarcher.systems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Array as GdxArray
import com.ironbow.stickmanarcher.assets.AssetManager
import com.ironbow.stickmanarcher.config.GameConfig
import com.ironbow.stickmanarcher.data.AllyType
import com.ironbow.stickmanarcher.entities.Ally
import com.ironbow.stickmanarcher.entities.AllyBullet
import com.ironbow.stickmanarcher.entities.SpikeTrap
import com.ironbow.stickmanarcher.entities.Zombie
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

class AllyManager {

    private val allies = GdxArray<Ally>(false, 3)
    private val traps = GdxArray<SpikeTrap>(false, 6)
    private val bullets = GdxArray<AllyBullet>(false, 12)

    fun configure(activeTypes: List<AllyType>) {
        allies.clear(); traps.clear(); bullets.clear()
        for (type in activeTypes) allies.add(Ally(type))
    }

    fun update(dt: Float, zombies: GdxArray<Zombie>, onZombieDamaged: (Zombie, Float, Boolean) -> Unit) {
        for (i in 0 until allies.size) {
            val ally = allies[i]
            ally.animTime += dt; ally.actionTimer -= dt
            when (ally.type) {
                AllyType.GUNNER -> if (ally.actionTimer <= 0f) {
                    val target = findNearestZombie(ally.x, zombies)
                    if (target != null && target.x < GameConfig.WORLD_WIDTH - 30f) {
                        val dx = target.x - ally.x
                        val dy = (target.y + 52f * target.scale) - (ally.y + 56f)
                        val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
                        bullets.add(AllyBullet(ally.x + 32f, ally.y + 56f, (dx / dist) * 1050f, (dy / dist) * 1050f, 19f))
                        ally.actionTimer = 1.65f
                    } else ally.actionTimer = 0.4f
                }
                AllyType.TRAP_LAYER -> if (ally.actionTimer <= 0f && traps.size < 4) {
                    traps.add(SpikeTrap(GameConfig.BARRICADE_X + 120f + Random.nextFloat() * 340f, 3, 0f))
                    ally.actionTimer = 6.5f
                }
                AllyType.DOG -> {
                    val nearest = findNearestZombie(GameConfig.BARRICADE_X, zombies)
                    if (!ally.dogReturning && nearest != null && nearest.x < 720f && ally.actionTimer <= 0f) {
                        ally.x += (if (nearest.x > ally.x) 1f else -1f) * 270f * dt
                        if (abs(ally.x - nearest.x) < 28f) {
                            onZombieDamaged(nearest, 26f, false); ally.dogReturning = true; ally.actionTimer = 2.6f
                        }
                    } else {
                        val homeX = GameConfig.BARRICADE_X + 36f
                        if (abs(ally.x - homeX) > 6f) ally.x += (if (homeX > ally.x) 1f else -1f) * 210f * dt
                        else ally.dogReturning = false
                    }
                }
            }
        }

        var bIdx = bullets.size - 1
        while (bIdx >= 0) {
            val b = bullets[bIdx]; b.x += b.vx * dt; b.y += b.vy * dt
            var hit = false
            for (zIdx in 0 until zombies.size) {
                val z = zombies[zIdx]
                if (z.active && abs(b.x - z.x) < 22f * z.scale && b.y >= z.y && b.y <= z.y + 92f * z.scale) {
                    onZombieDamaged(z, b.damage, false); hit = true; break
                }
            }
            if (hit || b.x > GameConfig.WORLD_WIDTH + 60f || b.y < GameConfig.GROUND_Y) bullets.removeIndex(bIdx)
            bIdx--
        }

        var tIdx = traps.size - 1
        while (tIdx >= 0) {
            val trap = traps[tIdx]
            if (trap.cooldown > 0f) trap.cooldown -= dt
            if (trap.cooldown <= 0f) {
                for (zIdx in 0 until zombies.size) {
                    val z = zombies[zIdx]
                    if (z.active && !z.isAirborne && abs(z.x - trap.x) < 24f) {
                        z.legSlowTimer = 2.5f; onZombieDamaged(z, 22f, false)
                        trap.charges--; trap.cooldown = 0.9f; break
                    }
                }
            }
            if (trap.charges <= 0) traps.removeIndex(tIdx)
            tIdx--
        }
    }

    fun render(batch: SpriteBatch, assets: AssetManager) {
        for (i in 0 until allies.size) allies[i].render(batch, assets)
        val spikeCol = Color(0.75f, 0.80f, 0.86f, 1f)
        for (i in 0 until traps.size) {
            val t = traps[i]; val y = GameConfig.GROUND_Y
            assets.drawRect(batch, t.x - 18f, y, 36f, 5f, Color(0.32f, 0.24f, 0.16f, 1f))
            assets.drawLimb(batch, t.x - 12f, y + 3f, t.x - 8f, y + 17f, 3.5f, spikeCol)
            assets.drawLimb(batch, t.x, y + 3f, t.x, y + 19f, 3.5f, spikeCol)
            assets.drawLimb(batch, t.x + 12f, y + 3f, t.x + 8f, y + 17f, 3.5f, spikeCol)
        }
        val bulletCol = Color(1f, 0.90f, 0.35f, 1f)
        for (i in 0 until bullets.size) {
            val b = bullets[i]
            assets.drawLimb(batch, b.x - 10f, b.y, b.x + 4f, b.y, 3.5f, bulletCol)
        }
    }

    private fun findNearestZombie(fromX: Float, zombies: GdxArray<Zombie>): Zombie? {
        var best: Zombie? = null; var bestDist = Float.MAX_VALUE
        for (i in 0 until zombies.size) {
            val z = zombies[i]; if (!z.active) continue
            val d = abs(z.x - fromX)
            if (d < bestDist) { bestDist = d; best = z }
        }
        return best
    }
}
