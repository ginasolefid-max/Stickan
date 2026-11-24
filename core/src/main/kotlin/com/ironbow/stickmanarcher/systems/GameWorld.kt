package com.ironbow.stickmanarcher.systems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array as GdxArray
import com.badlogic.gdx.utils.Pool
import com.ironbow.stickmanarcher.assets.AssetManager
import com.ironbow.stickmanarcher.assets.AudioManager
import com.ironbow.stickmanarcher.assets.SfxType
import com.ironbow.stickmanarcher.config.GameConfig
import com.ironbow.stickmanarcher.data.AllyType
import com.ironbow.stickmanarcher.data.ArrowTrail
import com.ironbow.stickmanarcher.data.ArrowType
import com.ironbow.stickmanarcher.data.BiomeType
import com.ironbow.stickmanarcher.data.GameMode
import com.ironbow.stickmanarcher.data.HitZone
import com.ironbow.stickmanarcher.data.LevelData
import com.ironbow.stickmanarcher.data.MissionType
import com.ironbow.stickmanarcher.data.SaveSystem
import com.ironbow.stickmanarcher.data.UpgradeManager
import com.ironbow.stickmanarcher.data.UpgradeType
import com.ironbow.stickmanarcher.data.ZombieType
import com.ironbow.stickmanarcher.entities.AcidProjectile
import com.ironbow.stickmanarcher.entities.Arrow
import com.ironbow.stickmanarcher.entities.Barricade
import com.ironbow.stickmanarcher.entities.Pickup
import com.ironbow.stickmanarcher.entities.Player
import com.ironbow.stickmanarcher.entities.Zombie
import com.ironbow.stickmanarcher.platform.PlatformServices
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class GameWorld(
    private val saveSystem: SaveSystem,
    private val upgradeManager: UpgradeManager,
    private val audioManager: AudioManager,
    private val platformServices: PlatformServices
) {
    enum class MatchState { PLAYING, PAUSED, VICTORY, GAME_OVER }

    val player = Player()
    val barricade = Barricade()
    val waveManager = WaveManager()
    val particleManager = ParticleManager()
    val physicsSystem = PhysicsSystem()
    val allyManager = AllyManager()
    val biomeRenderer = BiomeRenderer()

    private val arrowPool = object : Pool<Arrow>(24, GameConfig.POOL_ARROWS) { override fun newObject() = Arrow() }
    private val zombiePool = object : Pool<Zombie>(32, GameConfig.POOL_ZOMBIES) { override fun newObject() = Zombie() }
    private val pickupPool = object : Pool<Pickup>(8, GameConfig.POOL_PICKUPS) { override fun newObject() = Pickup() }
    private val acidPool = object : Pool<AcidProjectile>(12, GameConfig.POOL_ACID) { override fun newObject() = AcidProjectile() }

    val activeArrows = GdxArray<Arrow>(false, GameConfig.POOL_ARROWS)
    val activeZombies = GdxArray<Zombie>(false, GameConfig.POOL_ZOMBIES)
    val activePickups = GdxArray<Pickup>(false, GameConfig.POOL_PICKUPS)
    val activeAcid = GdxArray<AcidProjectile>(false, GameConfig.POOL_ACID)

    var gameMode = GameMode.CAMPAIGN; private set
    var levelNumber = 1; private set
    var biome = BiomeType.GRAVEYARD; private set
    var matchState = MatchState.PLAYING

    var selectedArrowType = ArrowType.NORMAL
    var equippedTrail = ArrowTrail.NONE
    var sessionCoins = 0; var sessionGems = 0; var sessionScore = 0L; var sessionKills = 0
    var comboCount = 0; var comboTimer = 0f
    var timeAttackRemaining = GameConfig.TIME_ATTACK_START_SECONDS
    var waveBannerTimer = 0f; var waveBannerText = ""
    var tutorialStep = 0; var usedRevive = false; var coinsDoubled = false

    var bulletTimeTimer = 0f; var bulletTimeCooldown = 0f
    var screenShakeTimer = 0f; var screenShakeIntensity = 0f; var cameraFocusX = 0f
    var isDragging = false
    private var dragStartX = 0f; private var dragStartY = 0f; private var dragCurrX = 0f; private var dragCurrY = 0f
    private var drawCooldown = 0f; private var nextZombieId = 1; private var wavePauseTimer = 0f; private var groanTimer = 3f
    private val tmpVec = Vector2()

    fun startMatch(mode: GameMode, level: Int = 1) {
        clearPools(); gameMode = mode; levelNumber = level; matchState = MatchState.PLAYING
        usedRevive = false; coinsDoubled = false

        val dailyRules = LevelData.getDailyChallengeRules(saveSystem.currentEpochDay())
        biome = when (mode) {
            GameMode.CAMPAIGN -> LevelData.getCampaignLevel(level).biome
            GameMode.DAILY_CHALLENGE -> dailyRules.biome
            GameMode.BOSS_RUSH -> BiomeType.CITY_RUINS
            GameMode.TIME_ATTACK -> BiomeType.FOREST
            GameMode.ENDLESS -> BiomeType.GRAVEYARD
        }
        biomeRenderer.reset(biome)
        player.resetForMatch(upgradeManager.getMaxPlayerHp(), saveSystem.getSelectedSkin(), saveSystem.getSelectedBow(), saveSystem.getSelectedBowSkin())
        barricade.reset(upgradeManager.getMaxBarricadeHp(), upgradeManager.getBarricadeDamageReduction(), upgradeManager.getUpgradeLevel(UpgradeType.BARRICADE_ARMOR))
        equippedTrail = saveSystem.getSelectedTrail()
        selectedArrowType = if (mode == GameMode.DAILY_CHALLENGE) dailyRules.forcedArrowType else ArrowType.NORMAL

        val bonusQuiver = upgradeManager.getBonusWaveArrows()
        if (bonusQuiver > 0 && mode != GameMode.DAILY_CHALLENGE) {
            saveSystem.addArrowAmmo(ArrowType.FIRE, (bonusQuiver / 2).coerceAtLeast(1))
        }
        allyManager.configure(AllyType.entries.filter { upgradeManager.isAllyActive(it) })
        waveManager.configure(mode, level, dailyRules.fixedWind)
        waveManager.startNextWave()

        sessionCoins = 0; sessionGems = 0; sessionScore = 0L; sessionKills = 0
        comboCount = 0; comboTimer = 0f; timeAttackRemaining = GameConfig.TIME_ATTACK_START_SECONDS
        bulletTimeTimer = 0f; bulletTimeCooldown = 0f; screenShakeTimer = 0f; cameraFocusX = 0f
        isDragging = false; drawCooldown = 0f; wavePauseTimer = 0f
        tutorialStep = if (saveSystem.data.tutorialCompleted) 1 else 0
        announceWave("WAVE ${waveManager.currentWave}")
    }

    private fun clearPools() {
        for (i in 0 until activeArrows.size) arrowPool.free(activeArrows[i]); activeArrows.clear()
        for (i in 0 until activeZombies.size) zombiePool.free(activeZombies[i]); activeZombies.clear()
        for (i in 0 until activePickups.size) pickupPool.free(activePickups[i]); activePickups.clear()
        for (i in 0 until activeAcid.size) acidPool.free(activeAcid[i]); activeAcid.clear()
        particleManager.clear()
    }

    private fun announceWave(text: String) { waveBannerText = text; waveBannerTimer = 2.2f }

    fun onTouchDown(worldX: Float, worldY: Float) {
        if (matchState != MatchState.PLAYING || drawCooldown > 0f) return
        isDragging = true; dragStartX = worldX; dragStartY = worldY; dragCurrX = worldX; dragCurrY = worldY
        player.state = Player.AnimState.DRAW
        audioManager.play(SfxType.BOW_DRAW, 0.7f)
    }

    fun onTouchDragged(worldX: Float, worldY: Float) {
        if (!isDragging || matchState != MatchState.PLAYING) return
        dragCurrX = worldX; dragCurrY = worldY
        val sens = saveSystem.data.aimSensitivity
        var dx = (dragStartX - dragCurrX) * sens
        val dy = (dragStartY - dragCurrY) * sens
        if (saveSystem.data.leftHandedMode) dx = -dx
        val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (dist > 4f) {
            player.aimAngleRad = atan2(dy.toDouble(), dx.toDouble()).toFloat().coerceIn(-0.45f, 1.38f)
            player.drawPower = (dist / GameConfig.MAX_DRAG_DISTANCE).coerceIn(0f, 1f)
        }
    }

    fun onTouchUp() {
        if (!isDragging) return
        isDragging = false
        if (matchState != MatchState.PLAYING) { player.state = Player.AnimState.IDLE; player.drawPower = 0f; return }
        val power = player.drawPower
        if (power < GameConfig.MIN_DRAG_DISTANCE / GameConfig.MAX_DRAG_DISTANCE) {
            player.state = Player.AnimState.IDLE; player.drawPower = 0f; return
        }

        var fireType = selectedArrowType
        if (gameMode != GameMode.DAILY_CHALLENGE && fireType != ArrowType.NORMAL && !saveSystem.consumeArrowAmmo(fireType)) {
            fireType = ArrowType.NORMAL; selectedArrowType = ArrowType.NORMAL
        }

        val launchAngle = player.getEffectiveAimAngle()
        val speed = GameConfig.BASE_ARROW_SPEED * player.bowType.velocityMultiplier * (0.35f + 0.65f * power)
        val baseDmg = 28f * upgradeManager.getDamageMultiplier() * fireType.damageMult * (0.55f + 0.45f * power)
        val isCrit = Random.nextFloat() < upgradeManager.getCritChance()
        val spawnX = player.x + 28f; val spawnY = player.y + 72f

        if (fireType == ArrowType.TRIPLE_SHOT) {
            for (off in floatArrayOf(-0.08f, 0f, 0.08f)) spawnArrow(spawnX, spawnY, launchAngle + off, speed, fireType, baseDmg, isCrit)
        } else spawnArrow(spawnX, spawnY, launchAngle, speed, fireType, baseDmg, isCrit)

        player.triggerRelease()
        drawCooldown = (0.36f / upgradeManager.getDrawSpeedMultiplier()).coerceAtLeast(0.14f)
        audioManager.play(SfxType.ARROW_WHOOSH)
        if (tutorialStep == 0) { tutorialStep = 1; saveSystem.data.tutorialCompleted = true; saveSystem.save() }
    }

    private fun spawnArrow(sx: Float, sy: Float, angle: Float, speed: Float, type: ArrowType, damage: Float, crit: Boolean) {
        if (activeArrows.size >= GameConfig.POOL_ARROWS) return
        val arrow = arrowPool.obtain()
        arrow.init(sx, sy, cos(angle.toDouble()).toFloat() * speed, sin(angle.toDouble()).toFloat() * speed, type, equippedTrail, damage, crit)
        activeArrows.add(arrow)
    }

    fun repairBarricade(): Boolean {
        if (barricade.hp >= barricade.maxHp || saveSystem.data.coins < GameConfig.BARRICADE_REPAIR_COST_COINS) return false
        saveSystem.data.coins -= GameConfig.BARRICADE_REPAIR_COST_COINS
        barricade.repair(GameConfig.BARRICADE_REPAIR_PERCENT); saveSystem.save()
        particleManager.spawnText("REPAIRED!", barricade.x, barricade.y + 115f, Color.CYAN, true)
        audioManager.play(SfxType.PICKUP); return true
    }

    fun reviveFromAd() {
        usedRevive = true; player.hp = player.maxHp * 0.65f; player.state = Player.AnimState.IDLE
        barricade.hp = barricade.maxHp * 0.50f; matchState = MatchState.PLAYING
        for (i in 0 until activeZombies.size) if (activeZombies[i].x < GameConfig.BARRICADE_X + 260f) activeZombies[i].x += 220f
        particleManager.spawnText("SECOND WIND!", player.x + 20f, player.y + 130f, Color.GOLD, true)
    }

    fun update(rawDt: Float) {
        val dt = rawDt.coerceAtMost(0.05f)
        if (matchState != MatchState.PLAYING) return

        if (drawCooldown > 0f) drawCooldown -= dt
        if (waveBannerTimer > 0f) waveBannerTimer -= dt
        if (bulletTimeCooldown > 0f) bulletTimeCooldown -= dt
        if (screenShakeTimer > 0f) screenShakeTimer -= dt
        if (comboCount > 0) { comboTimer -= dt; if (comboTimer <= 0f) comboCount = 0 }

        if (gameMode == GameMode.TIME_ATTACK) {
            timeAttackRemaining -= dt
            if (timeAttackRemaining <= 0f) { timeAttackRemaining = 0f; finishVictory(); return }
        }

        val simScale = if (bulletTimeTimer > 0f) { bulletTimeTimer -= dt; GameConfig.BULLET_TIME_SCALE } else 1f
        val simDt = dt * simScale

        biomeRenderer.update(simDt); player.update(dt); barricade.update(simDt); particleManager.update(simDt)
        groanTimer -= dt
        if (groanTimer <= 0f) {
            groanTimer = 4.2f + Random.nextFloat() * 4f
            if (activeZombies.size > 0) audioManager.play(SfxType.ZOMBIE_GROAN, 0.55f)
        }

        waveManager.pollNextSpawn(simDt)?.let { spawnZombie(it) }

        if (waveManager.isFinalWaveComplete(activeZombies.size)) { finishVictory(); return }
        else if (waveManager.isWaveCleared(activeZombies.size)) {
            wavePauseTimer += dt
            if (wavePauseTimer >= 1.5f) {
                wavePauseTimer = 0f; saveSystem.recordMissionProgress(MissionType.CLEAR_WAVES, 1)
                if (waveManager.startNextWave()) {
                    announceWave("WAVE ${waveManager.currentWave}")
                    spawnRandomPickup(480f + Random.nextFloat() * 480f, GameConfig.WORLD_HEIGHT - 60f)
                }
            }
        }

        updateArrows(simDt); updateZombies(simDt); updatePickups(simDt); updateAcid(simDt)
        allyManager.update(simDt, activeZombies) { zombie, dmg, isHead -> applyDamageToZombie(zombie, dmg, isHead, ArrowType.NORMAL, false) }
        cameraFocusX += ((if (bulletTimeTimer > 0f) 110f else 0f) - cameraFocusX) * (dt * 6f).coerceAtMost(1f)
    }

    private fun spawnZombie(type: ZombieType) {
        if (activeZombies.size >= GameConfig.POOL_ZOMBIES) return
        val z = zombiePool.obtain(); z.init(nextZombieId++, type, GameConfig.ZOMBIE_SPAWN_X, waveManager.getWaveHpScale()); activeZombies.add(z)
        if (type == ZombieType.BOSS) { announceWave("BOSS APPROACHING!"); triggerScreenShake(0.35f, 8f) }
    }

    private fun spawnRandomPickup(x: Float, y: Float) {
        if (activePickups.size >= GameConfig.POOL_PICKUPS) return
        val types = arrayOf(ArrowType.FIRE, ArrowType.ICE, ArrowType.EXPLOSIVE, ArrowType.PIERCING, ArrowType.POISON)
        val p = pickupPool.obtain(); p.init(x, y, types[Random.nextInt(types.size)], 3 + Random.nextInt(3), 15); activePickups.add(p)
    }

    private fun updateArrows(dt: Float) {
        var i = activeArrows.size - 1
        while (i >= 0) {
            val arrow = activeArrows[i]; arrow.update(dt, waveManager.currentWindX)
            if (arrow.stuck) {
                if (!arrow.stuckInGround) {
                    val host = findZombieById(arrow.stuckZombieId)
                    if (host != null && host.active) { arrow.x = host.x + arrow.stuckOffsetX; arrow.y = host.y + arrow.stuckOffsetY }
                    else arrow.active = false
                }
            } else if (arrow.active) {
                arrow.trailTimer += dt
                if (arrow.trailTimer >= 0.025f) { arrow.trailTimer = 0f; particleManager.spawnArrowTrail(arrow.x, arrow.y, arrow.type, equippedTrail) }
                for (pIdx in 0 until activePickups.size) {
                    val pickup = activePickups[pIdx]
                    if (physicsSystem.checkArrowPickupHit(arrow, pickup)) {
                        saveSystem.addArrowAmmo(pickup.arrowType, pickup.amount); sessionCoins += pickup.bonusCoins
                        particleManager.spawnText("+${pickup.amount} ${pickup.arrowType.displayName}", pickup.x, pickup.y + 24f, Color.CYAN)
                        audioManager.play(SfxType.PICKUP); pickup.active = false
                    }
                }
                if (bulletTimeTimer <= 0f && bulletTimeCooldown <= 0f) {
                    for (zIdx in 0 until activeZombies.size) {
                        if (physicsSystem.isNearFarTarget(arrow, activeZombies[zIdx])) {
                            bulletTimeTimer = GameConfig.BULLET_TIME_MAX_DURATION; bulletTimeCooldown = GameConfig.BULLET_TIME_COOLDOWN; break
                        }
                    }
                }
                for (zIdx in 0 until activeZombies.size) {
                    val zombie = activeZombies[zIdx]
                    val zone = physicsSystem.checkArrowZombieHit(arrow, zombie)
                    if (zone != null) { handleArrowZombieImpact(arrow, zombie, zone); if (!arrow.active || arrow.stuck) break }
                }
                if (arrow.active && !arrow.stuck && arrow.y <= GameConfig.GROUND_Y) {
                    if (arrow.type == ArrowType.EXPLOSIVE) { triggerExplosion(arrow.x, GameConfig.GROUND_Y + 8f, arrow.damage); arrow.active = false }
                    else arrow.stickToGround(GameConfig.GROUND_Y)
                }
            }
            if (!arrow.active) { activeArrows.removeIndex(i); arrowPool.free(arrow) }
            i--
        }
    }

    private fun handleArrowZombieImpact(arrow: Arrow, zombie: Zombie, zone: HitZone) {
        arrow.markZombieHit(zombie.id)
        val zoneMult = when (zone) { HitZone.HEAD -> GameConfig.HEADSHOT_MULTIPLIER; HitZone.BODY -> GameConfig.BODYSHOT_MULTIPLIER; HitZone.LEGS -> GameConfig.LEGSHOT_MULTIPLIER }
        val totalDmg = arrow.damage * zoneMult * (if (arrow.isCrit) 1.5f else 1.0f)
        if (zone == HitZone.LEGS) zombie.legSlowTimer = GameConfig.LEG_SLOW_DURATION
        when (arrow.type) {
            ArrowType.FIRE -> zombie.burnTimer = GameConfig.FIRE_BURN_DURATION
            ArrowType.ICE -> { zombie.freezeTimer = GameConfig.ICE_FREEZE_DURATION; audioManager.play(SfxType.ICE_SHATTER, 0.8f) }
            ArrowType.POISON -> zombie.poisonTimer = GameConfig.POISON_DURATION
            ArrowType.EXPLOSIVE -> triggerExplosion(arrow.x, arrow.y, totalDmg)
            else -> {}
        }
        val isHeadshot = zone == HitZone.HEAD
        if (isHeadshot) {
            comboCount++; comboTimer = 4.5f; saveSystem.recordMissionProgress(MissionType.SCORE_HEADSHOTS, 1)
            audioManager.play(SfxType.HIT_HEADSHOT); triggerScreenShake(0.12f, 4f)
            particleManager.spawnText(if (comboCount > 1) "HEADSHOT! x$comboCount" else "HEADSHOT!", zombie.x - 36f, zombie.y + 115f * zombie.scale, Color(1f, 0.85f, 0.18f, 1f), true)
            if (gameMode == GameMode.TIME_ATTACK) timeAttackRemaining += 1.5f
        } else audioManager.play(SfxType.HIT_BODY)

        particleManager.spawnHitBurst(arrow.x, arrow.y, isHeadshot, saveSystem.data.bloodEnabled, arrow.type)
        applyDamageToZombie(zombie, totalDmg, isHeadshot, arrow.type, arrow.type == ArrowType.PIERCING)
        if (arrow.type == ArrowType.EXPLOSIVE) arrow.active = false
        else if (arrow.pierceRemaining > 0) arrow.pierceRemaining--
        else if (zombie.active) arrow.stickToZombie(zombie, arrow.x, arrow.y)
        else arrow.active = false
    }

    private fun triggerExplosion(cx: Float, cy: Float, baseDamage: Float) {
        particleManager.spawnExplosion(cx, cy); audioManager.play(SfxType.EXPLOSION); triggerScreenShake(0.24f, 9f)
        for (i in 0 until activeZombies.size) {
            val z = activeZombies[i]; if (!z.active) continue
            val dist = hypot((z.x - cx).toDouble(), ((z.y + 45f) - cy).toDouble()).toFloat()
            if (dist <= GameConfig.EXPLOSIVE_RADIUS) {
                applyDamageToZombie(z, baseDamage * GameConfig.EXPLOSIVE_DAMAGE_RATIO * (1f - (dist / GameConfig.EXPLOSIVE_RADIUS) * 0.5f), false, ArrowType.EXPLOSIVE, true)
            }
        }
    }

    private fun applyDamageToZombie(zombie: Zombie, rawDamage: Float, headshot: Boolean, arrowType: ArrowType, piercing: Boolean) {
        if (!zombie.active) return
        val dealt = zombie.applyDamage(rawDamage, piercing)
        particleManager.spawnText("-${dealt.toInt()}", zombie.x - 10f, zombie.y + 78f * zombie.scale, if (headshot) Color.GOLD else Color.WHITE, false)
        if (zombie.hp <= 0f) handleZombieDeath(zombie, headshot)
    }

    private fun handleZombieDeath(zombie: Zombie, headshotKill: Boolean) {
        zombie.active = false; sessionKills++; saveSystem.recordMissionProgress(MissionType.KILL_ZOMBIES, 1)
        val comboBonus = 1f + (comboCount.coerceAtMost(10) * 0.08f)
        sessionCoins += (zombie.type.coinReward * comboBonus).toInt()
        sessionScore += (zombie.type.scoreReward * comboBonus).toLong()
        if (zombie.type == ZombieType.BOSS) { sessionGems += 3; triggerScreenShake(0.35f, 10f) }
        else if (Random.nextFloat() < 0.16f) spawnRandomPickup(zombie.x, zombie.y + 38f)
        if (zombie.type == ZombieType.EXPLODER) triggerExplosion(zombie.x, zombie.y + 42f, 45f)
        particleManager.spawnZombieRagdoll(zombie.x, zombie.y, zombie.type, headshotKill, zombie.scale)
    }

    private fun updateZombies(dt: Float) {
        var i = activeZombies.size - 1
        while (i >= 0) {
            val z = activeZombies[i]; z.updateTimers(dt)
            z.statusTickAccum += dt
            if (z.statusTickAccum >= 0.5f) {
                z.statusTickAccum -= 0.5f
                if (z.burnTimer > 0f) applyDamageToZombie(z, GameConfig.FIRE_DPS * 0.5f, false, ArrowType.FIRE, true)
                if (z.active && z.poisonTimer > 0f) applyDamageToZombie(z, GameConfig.POISON_DPS * 0.5f, false, ArrowType.POISON, true)
            }
            if (z.active) {
                val targetStopX = if (barricade.isIntact()) barricade.x + barricade.width + 18f else player.x + 34f
                if (z.type == ZombieType.SPITTER && z.x < 980f && z.x > targetStopX + 120f && z.spitCooldown <= 0f) {
                    z.spitCooldown = 2.8f; spawnAcid(z.x - 16f, z.y + 72f, targetStopX, GameConfig.GROUND_Y + 45f, z.attackDamage)
                }
                if (z.type == ZombieType.BOSS && z.bossPatternTimer <= 0f) {
                    z.bossPatternTimer = 4.2f; z.bossPhase = (z.bossPhase + 1) % 3
                    when (z.bossPhase) {
                        0 -> { spawnAcid(z.x - 20f, z.y + 110f, targetStopX, GameConfig.GROUND_Y + 50f, z.attackDamage * 0.7f); spawnAcid(z.x - 20f, z.y + 110f, player.x, GameConfig.GROUND_Y + 55f, z.attackDamage * 0.7f) }
                        1 -> { spawnZombie(ZombieType.RUNNER); particleManager.spawnText("SUMMON!", z.x - 30f, z.y + 165f, Color.RED, true) }
                        2 -> { triggerScreenShake(0.28f, 7f); if (barricade.isIntact() && z.x < barricade.x + 280f) barricade.applyDamage(z.attackDamage * 0.6f) }
                    }
                }
                if (z.x > targetStopX) {
                    val spd = z.currentSpeed(); z.x -= spd * dt; z.walkCycle += (spd * 0.09f) * dt
                    if (z.type == ZombieType.JUMPER && !z.isAirborne && z.jumpCooldown <= 0f && z.x < 850f) { z.isAirborne = true; z.vy = 390f; z.jumpCooldown = 3.2f }
                } else if (z.type == ZombieType.EXPLODER) {
                    if (barricade.isIntact()) barricade.applyDamage(z.attackDamage) else player.applyDamage(z.attackDamage)
                    triggerExplosion(z.x, z.y + 40f, 35f); z.active = false
                } else if (z.attackCooldown <= 0f) {
                    z.attackCooldown = 1.15f
                    if (barricade.isIntact()) barricade.applyDamage(z.attackDamage)
                    else { player.applyDamage(z.attackDamage); if (saveSystem.data.vibrationEnabled) platformServices.vibrate(45L); triggerScreenShake(0.15f, 5f) }
                }
            }
            if (!z.active) { activeZombies.removeIndex(i); zombiePool.free(z) }
            i--
        }
        if (player.hp <= 0f) finishDefeat()
    }

    private fun spawnAcid(sx: Float, sy: Float, tx: Float, ty: Float, dmg: Float) {
        if (activeAcid.size >= GameConfig.POOL_ACID) return
        val flightT = 1.05f
        val vx = (tx - sx) / flightT
        val vy = ((ty - sy) - 0.5f * (GameConfig.GRAVITY * 0.55f) * flightT * flightT) / flightT
        val acid = acidPool.obtain(); acid.init(sx, sy, vx, vy, dmg); activeAcid.add(acid)
    }

    private fun updateAcid(dt: Float) {
        var i = activeAcid.size - 1
        while (i >= 0) {
            val acid = activeAcid[i]; acid.update(dt)
            if (acid.active) {
                if (barricade.isIntact() && acid.x <= barricade.x + barricade.width && acid.x >= barricade.x - 10f && acid.y <= barricade.y + barricade.height) {
                    barricade.applyDamage(acid.damage); acid.active = false
                } else if (acid.x <= player.x + 18f && acid.x >= player.x - 18f && acid.y <= player.y + GameConfig.PLAYER_HEIGHT) {
                    player.applyDamage(acid.damage); acid.active = false
                }
            }
            if (!acid.active) { activeAcid.removeIndex(i); acidPool.free(acid) }
            i--
        }
    }

    private fun updatePickups(dt: Float) {
        var i = activePickups.size - 1
        while (i >= 0) {
            val p = activePickups[i]; p.update(dt)
            if (!p.active) { activePickups.removeIndex(i); pickupPool.free(p) }
            i--
        }
    }

    private fun finishVictory() {
        matchState = MatchState.VICTORY; audioManager.play(SfxType.LEVEL_CLEAR)
        if (gameMode == GameMode.CAMPAIGN) {
            val cfg = LevelData.getCampaignLevel(levelNumber)
            sessionCoins += cfg.rewardCoins; sessionGems += cfg.rewardGems
            saveSystem.recordCampaignVictory(levelNumber, computeStars())
        } else if (gameMode == GameMode.DAILY_CHALLENGE) {
            val today = saveSystem.currentEpochDay()
            if (saveSystem.data.dailyChallengeCompletedDay != today) { saveSystem.data.dailyChallengeCompletedDay = today; sessionCoins += 280; sessionGems += 8 }
        } else if (gameMode == GameMode.TIME_ATTACK && sessionScore > saveSystem.data.timeAttackBestScore) {
            saveSystem.data.timeAttackBestScore = sessionScore
        }
        saveSystem.data.coins += sessionCoins; saveSystem.data.gems += sessionGems; saveSystem.save()
    }

    private fun finishDefeat() {
        matchState = MatchState.GAME_OVER; audioManager.play(SfxType.GAME_OVER)
        saveSystem.data.coins += sessionCoins; saveSystem.data.gems += sessionGems
        if (gameMode == GameMode.ENDLESS) {
            saveSystem.recordEndlessRun(sessionScore, waveManager.currentWave, sessionKills)
            platformServices.submitLeaderboardScore(GameConfig.LEADERBOARD_ENDLESS_ID, sessionScore)
        } else if (gameMode == GameMode.BOSS_RUSH && waveManager.currentWave > saveSystem.data.bossRushBestWave) {
            saveSystem.data.bossRushBestWave = waveManager.currentWave
        }
        saveSystem.save()
    }

    fun computeStars(): Int = when {
        player.hp / player.maxHp >= 0.80f && barricade.hp > 0f -> 3
        player.hp / player.maxHp >= 0.45f -> 2
        else -> 1
    }

    fun triggerScreenShake(duration: Float, intensity: Float) { screenShakeTimer = duration; screenShakeIntensity = intensity }

    private fun findZombieById(id: Int): Zombie? {
        for (i in 0 until activeZombies.size) if (activeZombies[i].id == id) return activeZombies[i]
        return null
    }

    fun render(batch: SpriteBatch, assets: AssetManager) {
        biomeRenderer.render(batch, assets, biome, cameraFocusX)
        barricade.render(batch, assets); allyManager.render(batch, assets); player.render(batch, assets)
        for (i in 0 until activePickups.size) activePickups[i].render(batch, assets)
        for (i in 0 until activeZombies.size) activeZombies[i].render(batch, assets)
        for (i in 0 until activeArrows.size) activeArrows[i].render(batch, assets)
        for (i in 0 until activeAcid.size) activeAcid[i].render(batch, assets)
        particleManager.render(batch, assets)

        if (isDragging && saveSystem.data.trajectoryPreviewEnabled && player.drawPower > 0.08f) {
            val angle = player.getEffectiveAimAngle()
            val speed = GameConfig.BASE_ARROW_SPEED * player.bowType.velocityMultiplier * (0.35f + 0.65f * player.drawPower)
            val vx = cos(angle.toDouble()).toFloat() * speed; val vy = sin(angle.toDouble()).toFloat() * speed
            val steps = upgradeManager.getTrajectoryStepCount()
            val dotCol = Color(1f, 0.95f, 0.72f, 0.85f)
            for (step in 1..steps) {
                physicsSystem.computeTrajectoryPoint(player.x + 28f, player.y + 72f, vx, vy, waveManager.currentWindX, step, tmpVec)
                if (tmpVec.y < GameConfig.GROUND_Y) break
                dotCol.a = (1f - step.toFloat() / (steps + 2)).coerceIn(0.15f, 0.9f)
                assets.drawCircle(batch, tmpVec.x, tmpVec.y, 4.2f, dotCol)
            }
        }
        particleManager.renderTexts(batch, assets)
    }

    fun getActiveBoss(): Zombie? {
        for (i in 0 until activeZombies.size) if (activeZombies[i].type == ZombieType.BOSS) return activeZombies[i]
        return null
    }
}
