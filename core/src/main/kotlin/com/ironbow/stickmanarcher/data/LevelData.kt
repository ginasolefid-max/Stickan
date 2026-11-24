package com.ironbow.stickmanarcher.data

import com.ironbow.stickmanarcher.config.GameConfig
import kotlin.math.min

object LevelData {

    private val campaignLevels: List<LevelConfig> = (1..GameConfig.TOTAL_CAMPAIGN_LEVELS).map { lvl ->
        val biome = when {
            lvl <= 15 -> BiomeType.GRAVEYARD
            lvl <= 30 -> BiomeType.FOREST
            lvl <= 45 -> BiomeType.CITY_RUINS
            else -> BiomeType.SNOW
        }
        val localIdx = (lvl - 1) % 15 + 1
        val waveCount = when {
            lvl % 10 == 0 -> 5
            localIdx >= 10 -> 4
            localIdx >= 4 -> 3
            else -> 2
        }
        val baseZombies = 5 + (lvl / 3) + (localIdx / 2)
        val windRange = min(140f, 18f + lvl * 2.1f)
        val hasBoss = (lvl % 10 == 0) || (lvl == GameConfig.TOTAL_CAMPAIGN_LEVELS)

        LevelConfig(
            levelNumber = lvl,
            biome = biome,
            waveCount = waveCount,
            baseZombiesPerWave = baseZombies,
            windRange = windRange,
            hasBoss = hasBoss,
            rewardCoins = 75 + lvl * 18 + if (hasBoss) 150 else 0,
            rewardGems = if (hasBoss) 5 else if (lvl % 5 == 0) 2 else 1
        )
    }

    fun getCampaignLevel(levelNumber: Int): LevelConfig =
        campaignLevels[levelNumber.coerceIn(1, campaignLevels.size) - 1]

    fun getAllCampaignLevels(): List<LevelConfig> = campaignLevels

    data class DailyChallengeRules(
        val biome: BiomeType,
        val fixedWind: Float,
        val forcedArrowType: ArrowType,
        val description: String
    )

    fun getDailyChallengeRules(epochDay: Long): DailyChallengeRules {
        val biomes = BiomeType.entries
        val biome = biomes[((epochDay % biomes.size).toInt() + biomes.size) % biomes.size]
        val arrowPool = listOf(ArrowType.FIRE, ArrowType.ICE, ArrowType.PIERCING, ArrowType.EXPLOSIVE, ArrowType.POISON)
        val arrow = arrowPool[((epochDay / 2).toInt() % arrowPool.size + arrowPool.size) % arrowPool.size]
        val windDir = if (epochDay % 2L == 0L) 1f else -1f
        val wind = windDir * (45f + ((epochDay % 5L) * 18f))
        val dirLabel = if (wind >= 0f) "Tailwind" else "Headwind"

        return DailyChallengeRules(
            biome = biome,
            fixedWind = wind,
            forcedArrowType = arrow,
            description = "${biome.displayName} • $dirLabel ${kotlin.math.abs(wind).toInt()} • ${arrow.displayName} Arrows"
        )
    }
}
