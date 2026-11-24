package com.ironbow.stickmanarcher.systems

import com.ironbow.stickmanarcher.config.GameConfig
import com.ironbow.stickmanarcher.data.GameMode
import com.ironbow.stickmanarcher.data.LevelData
import com.ironbow.stickmanarcher.data.ZombieType
import kotlin.random.Random

class WaveManager {

    var currentWave = 1; private set
    var totalWaves = 3; private set
    var currentWindX = 0f; private set
    var zombiesRemainingToSpawn = 0; private set
    var bossPending = false; private set

    private var spawnTimer = 0f; private var spawnInterval = 1.6f
    private var gameMode = GameMode.CAMPAIGN; private var campaignLevel = 1; private var fixedDailyWind = 0f

    fun configure(mode: GameMode, levelNumber: Int, dailyWind: Float = 0f) {
        gameMode = mode; campaignLevel = levelNumber; fixedDailyWind = dailyWind; currentWave = 0
        totalWaves = when (mode) {
            GameMode.CAMPAIGN -> LevelData.getCampaignLevel(levelNumber).waveCount
            GameMode.DAILY_CHALLENGE -> 4
            else -> Int.MAX_VALUE
        }
    }

    fun startNextWave(): Boolean {
        if (currentWave >= totalWaves) return false
        currentWave++

        currentWindX = when (gameMode) {
            GameMode.DAILY_CHALLENGE -> fixedDailyWind
            GameMode.CAMPAIGN -> (Random.nextFloat() * 2f - 1f) * LevelData.getCampaignLevel(campaignLevel).windRange
            else -> (Random.nextFloat() * 2f - 1f) * (35f + currentWave * 6f).coerceAtMost(145f)
        }

        when (gameMode) {
            GameMode.BOSS_RUSH -> { zombiesRemainingToSpawn = 3 + currentWave; bossPending = true; spawnInterval = 1.4f }
            GameMode.CAMPAIGN -> {
                val cfg = LevelData.getCampaignLevel(campaignLevel)
                zombiesRemainingToSpawn = cfg.baseZombiesPerWave + (currentWave - 1) * 2
                bossPending = cfg.hasBoss && currentWave == totalWaves
                spawnInterval = (1.75f - campaignLevel * 0.015f).coerceAtLeast(0.70f)
            }
            GameMode.TIME_ATTACK -> {
                zombiesRemainingToSpawn = 8 + currentWave * 2; bossPending = false
                spawnInterval = (1.15f - currentWave * 0.05f).coerceAtLeast(0.45f)
            }
            GameMode.DAILY_CHALLENGE -> {
                zombiesRemainingToSpawn = 7 + currentWave * 2; bossPending = currentWave == totalWaves; spawnInterval = 1.25f
            }
            GameMode.ENDLESS -> {
                zombiesRemainingToSpawn = 6 + currentWave * 2
                bossPending = (currentWave % GameConfig.BOSS_WAVE_INTERVAL == 0)
                spawnInterval = (1.65f - currentWave * 0.04f).coerceAtLeast(0.55f)
            }
        }
        spawnTimer = 0.6f
        return true
    }

    fun pollNextSpawn(dt: Float): ZombieType? {
        if (zombiesRemainingToSpawn <= 0 && !bossPending) return null
        spawnTimer -= dt
        if (spawnTimer > 0f) return null
        spawnTimer = spawnInterval * (0.75f + Random.nextFloat() * 0.5f)
        if (zombiesRemainingToSpawn > 0) { zombiesRemainingToSpawn--; return pickZombieType() }
        if (bossPending) { bossPending = false; return ZombieType.BOSS }
        return null
    }

    fun getWaveHpScale(): Float = when (gameMode) {
        GameMode.CAMPAIGN -> 1f + (campaignLevel - 1) * 0.07f + (currentWave - 1) * 0.10f
        GameMode.BOSS_RUSH -> 1f + (currentWave - 1) * 0.25f
        else -> 1f + (currentWave - 1) * 0.16f
    }

    fun isFinalWaveComplete(activeZombieCount: Int): Boolean =
        currentWave >= totalWaves && zombiesRemainingToSpawn == 0 && !bossPending && activeZombieCount == 0

    fun isWaveCleared(activeZombieCount: Int): Boolean =
        zombiesRemainingToSpawn == 0 && !bossPending && activeZombieCount == 0

    private fun pickZombieType(): ZombieType {
        val progression = if (gameMode == GameMode.CAMPAIGN) campaignLevel + currentWave else currentWave * 3
        val roll = Random.nextFloat()
        return when {
            progression <= 3 -> if (roll < 0.78f) ZombieType.WALKER else ZombieType.RUNNER
            progression <= 7 -> when {
                roll < 0.45f -> ZombieType.WALKER; roll < 0.72f -> ZombieType.RUNNER
                roll < 0.88f -> ZombieType.JUMPER; else -> ZombieType.TANK
            }
            else -> when {
                roll < 0.28f -> ZombieType.WALKER; roll < 0.48f -> ZombieType.RUNNER
                roll < 0.63f -> ZombieType.TANK; roll < 0.76f -> ZombieType.JUMPER
                roll < 0.89f -> ZombieType.SPITTER; else -> ZombieType.EXPLODER
            }
        }
    }
}
