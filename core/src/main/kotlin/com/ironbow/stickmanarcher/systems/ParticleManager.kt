package com.ironbow.stickmanarcher.systems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Array as GdxArray
import com.badlogic.gdx.utils.Pool
import com.ironbow.stickmanarcher.assets.AssetManager
import com.ironbow.stickmanarcher.config.GameConfig
import com.ironbow.stickmanarcher.data.ArrowTrail
import com.ironbow.stickmanarcher.data.ArrowType
import com.ironbow.stickmanarcher.data.ZombieType
import com.ironbow.stickmanarcher.entities.FloatingText
import com.ironbow.stickmanarcher.entities.Particle
import com.ironbow.stickmanarcher.entities.RagdollPiece
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class ParticleManager {

    private val particlePool = object : Pool<Particle>(64, GameConfig.POOL_PARTICLES) { override fun newObject() = Particle() }
    private val textPool = object : Pool<FloatingText>(16, GameConfig.POOL_FLOATING_TEXT) { override fun newObject() = FloatingText() }
    private val ragdollPool = object : Pool<RagdollPiece>(24, GameConfig.POOL_RAGDOLL) { override fun newObject() = RagdollPiece() }

    private val activeParticles = GdxArray<Particle>(false, GameConfig.POOL_PARTICLES)
    private val activeTexts = GdxArray<FloatingText>(false, GameConfig.POOL_FLOATING_TEXT)
    private val activeRagdolls = GdxArray<RagdollPiece>(false, GameConfig.POOL_RAGDOLL)

    fun clear() {
        for (i in 0 until activeParticles.size) particlePool.free(activeParticles[i])
        activeParticles.clear()
        for (i in 0 until activeTexts.size) textPool.free(activeTexts[i])
        activeTexts.clear()
        for (i in 0 until activeRagdolls.size) ragdollPool.free(activeRagdolls[i])
        activeRagdolls.clear()
    }

    fun spawnParticle(x: Float, y: Float, vx: Float, vy: Float, size: Float, life: Float, color: Color, gravScale: Float = 0.5f, glow: Boolean = false) {
        if (activeParticles.size >= GameConfig.POOL_PARTICLES) return
        val p = particlePool.obtain()
        p.init(x, y, vx, vy, size, life, color, gravScale, glow)
        activeParticles.add(p)
    }

    fun spawnHitBurst(x: Float, y: Float, isHeadshot: Boolean, bloodEnabled: Boolean, arrowType: ArrowType) {
        val count = if (isHeadshot) 16 else 9
        val col = when {
            arrowType == ArrowType.FIRE -> Color(1f, 0.50f, 0.15f, 1f)
            arrowType == ArrowType.ICE -> Color(0.55f, 0.92f, 1f, 1f)
            arrowType == ArrowType.POISON -> Color(0.45f, 0.95f, 0.25f, 1f)
            bloodEnabled -> Color(0.84f, 0.08f, 0.12f, 1f)
            else -> Color(0.95f, 0.85f, 0.45f, 1f)
        }
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 6.283f
            val speed = 60f + Random.nextFloat() * (if (isHeadshot) 220f else 140f)
            spawnParticle(x, y, cos(angle) * speed, sin(angle) * speed + 70f, 4f + Random.nextFloat() * 5f,
                0.35f + Random.nextFloat() * 0.35f, col, if (bloodEnabled) 0.85f else 0.35f, !bloodEnabled || arrowType != ArrowType.NORMAL)
        }
    }

    fun spawnExplosion(x: Float, y: Float) {
        for (i in 0 until 28) {
            val angle = Random.nextFloat() * 6.283f
            val speed = 70f + Random.nextFloat() * 290f
            val col = if (i % 2 == 0) Color(1f, 0.45f, 0.10f, 1f) else Color(1f, 0.85f, 0.25f, 1f)
            spawnParticle(x, y, cos(angle) * speed, sin(angle) * speed + 60f, 8f + Random.nextFloat() * 14f, 0.40f + Random.nextFloat() * 0.35f, col, 0.2f, true)
        }
    }

    fun spawnArrowTrail(x: Float, y: Float, type: ArrowType, trail: ArrowTrail) {
        val col = when {
            type == ArrowType.FIRE -> Color(1f, 0.45f, 0.12f, 0.9f)
            type == ArrowType.ICE -> Color(0.55f, 0.92f, 1f, 0.9f)
            type == ArrowType.POISON -> Color(0.45f, 0.96f, 0.28f, 0.9f)
            trail == ArrowTrail.EMBER -> Color(1f, 0.55f, 0.18f, 0.85f)
            trail == ArrowTrail.FROST -> Color(0.65f, 0.90f, 1f, 0.85f)
            trail == ArrowTrail.NEON -> Color(0.35f, 0.95f, 1f, 0.9f)
            trail == ArrowTrail.SHADOW_GOLD -> Color(1f, 0.84f, 0.25f, 0.9f)
            else -> return
        }
        spawnParticle(x, y, (Random.nextFloat() - 0.5f) * 28f, (Random.nextFloat() - 0.5f) * 28f, 6f + Random.nextFloat() * 5f, 0.26f, col, -0.05f, true)
    }

    fun spawnZombieRagdoll(x: Float, y: Float, type: ZombieType, headshotKill: Boolean, scale: Float) {
        val baseCol = when (type) {
            ZombieType.WALKER -> Color(0.32f, 0.56f, 0.34f, 1f)
            ZombieType.RUNNER -> Color(0.70f, 0.24f, 0.24f, 1f)
            ZombieType.TANK -> Color(0.38f, 0.44f, 0.50f, 1f)
            ZombieType.JUMPER -> Color(0.56f, 0.34f, 0.68f, 1f)
            ZombieType.SPITTER -> Color(0.42f, 0.78f, 0.22f, 1f)
            ZombieType.EXPLODER -> Color(0.88f, 0.42f, 0.16f, 1f)
            ZombieType.BOSS -> Color(0.46f, 0.12f, 0.22f, 1f)
        }
        val impulseBoost = if (headshotKill) 1.45f else 1.0f
        for (i in 0 until 4) {
            if (activeRagdolls.size >= GameConfig.POOL_RAGDOLL) break
            val p = ragdollPool.obtain()
            p.init(x, y + (24f + i * 12f) * scale, (60f + Random.nextFloat() * 140f) * impulseBoost, 80f + Random.nextFloat() * 180f,
                (Random.nextFloat() - 0.5f) * 14f, 26f * scale, 6f * scale, false, baseCol)
            activeRagdolls.add(p)
        }
        if (headshotKill && activeRagdolls.size < GameConfig.POOL_RAGDOLL) {
            val head = ragdollPool.obtain()
            head.init(x, y + 84f * scale, 140f + Random.nextFloat() * 160f, 240f + Random.nextFloat() * 150f, 12f, 24f * scale, 6f, true, baseCol)
            activeRagdolls.add(head)
        }
    }

    fun spawnText(msg: String, x: Float, y: Float, color: Color, large: Boolean = false) {
        if (activeTexts.size >= GameConfig.POOL_FLOATING_TEXT) return
        val ft = textPool.obtain()
        ft.init(msg, x, y, color, large)
        activeTexts.add(ft)
    }

    fun update(dt: Float) {
        var i = activeParticles.size - 1
        while (i >= 0) {
            val p = activeParticles[i]; p.update(dt)
            if (!p.active) { activeParticles.removeIndex(i); particlePool.free(p) }
            i--
        }
        i = activeRagdolls.size - 1
        while (i >= 0) {
            val r = activeRagdolls[i]; r.update(dt)
            if (!r.active) { activeRagdolls.removeIndex(i); ragdollPool.free(r) }
            i--
        }
        i = activeTexts.size - 1
        while (i >= 0) {
            val t = activeTexts[i]; t.update(dt)
            if (!t.active) { activeTexts.removeIndex(i); textPool.free(t) }
            i--
        }
    }

    fun render(batch: SpriteBatch, assets: AssetManager) {
        for (i in 0 until activeRagdolls.size) activeRagdolls[i].render(batch, assets)
        for (i in 0 until activeParticles.size) activeParticles[i].render(batch, assets)
    }

    fun renderTexts(batch: SpriteBatch, assets: AssetManager) {
        for (i in 0 until activeTexts.size) activeTexts[i].render(batch, assets)
    }
}
