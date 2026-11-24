package com.ironbow.stickmanarcher.data

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter
import com.ironbow.stickmanarcher.config.GameConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SaveSystem {

    private val json = Json().apply {
        setOutputType(JsonWriter.OutputType.json)
        setIgnoreUnknownFields(true)
    }

    var data: SaveData = SaveData()
        private set

    fun load() {
        val file = Gdx.files.local(GameConfig.SAVE_FILE_NAME)
        val prefs = Gdx.app.getPreferences(GameConfig.PREFS_NAME)

        val rawJson = when {
            file.exists() -> runCatching { file.readString("UTF-8") }.getOrNull()
            prefs.contains("save_json") -> prefs.getString("save_json", "")
            else -> null
        }

        if (!rawJson.isNullOrBlank()) {
            val loaded = runCatching { json.fromJson(SaveData::class.java, rawJson) }.getOrNull()
            if (loaded != null) data = loaded
        }

        ensureDefaults()
        checkDailyReset()
        save()
    }

    fun save() {
        val serialized = json.toJson(data)
        runCatching { Gdx.files.local(GameConfig.SAVE_FILE_NAME).writeString(serialized, false, "UTF-8") }
        runCatching {
            val prefs = Gdx.app.getPreferences(GameConfig.PREFS_NAME)
            prefs.putString("save_json", serialized)
            prefs.flush()
        }
    }

    private fun ensureDefaults() {
        if (!data.unlockedBows.contains(BowType.WOODEN.name)) data.unlockedBows.add(BowType.WOODEN.name)
        if (!data.unlockedSkins.contains(StickmanSkin.DEFAULT.name)) data.unlockedSkins.add(StickmanSkin.DEFAULT.name)
        if (!data.unlockedBowSkins.contains(BowSkin.CLASSIC.name)) data.unlockedBowSkins.add(BowSkin.CLASSIC.name)
        if (!data.unlockedTrails.contains(ArrowTrail.NONE.name)) data.unlockedTrails.add(ArrowTrail.NONE.name)
        if (data.highestUnlockedLevel < 1) data.highestUnlockedLevel = 1
    }

    fun checkDailyReset() {
        val today = currentEpochDay()
        if (data.lastLoginEpochDay != today) {
            data.loginStreak = if (data.lastLoginEpochDay == today - 1L) (data.loginStreak % 7) + 1 else 1
            data.lastLoginEpochDay = today
            data.dailyRewardClaimedToday = false
        }

        if (data.lastMissionDay != today || data.dailyMissions.isEmpty()) {
            data.lastMissionDay = today
            data.dailyMissions.clear()
            data.dailyMissions.add(DailyMission("m_kills_$today", MissionType.KILL_ZOMBIES, "Slay 35 Zombies", 35, 0, 160, 3, false))
            data.dailyMissions.add(DailyMission("m_headshots_$today", MissionType.SCORE_HEADSHOTS, "Land 12 Headshots", 12, 0, 200, 4, false))
            data.dailyMissions.add(DailyMission("m_waves_$today", MissionType.CLEAR_WAVES, "Survive 6 Waves", 6, 0, 220, 5, false))
        }
    }

    fun claimDailyLoginReward(): Pair<Int, Int>? {
        if (data.dailyRewardClaimedToday) return null
        val day = data.loginStreak.coerceIn(1, 7)
        val coins = 80 + day * 45
        val gems = if (day == 7) 15 else day
        data.coins += coins
        data.gems += gems
        data.dailyRewardClaimedToday = true
        save()
        return coins to gems
    }

    fun recordMissionProgress(type: MissionType, amount: Int) {
        if (amount <= 0) return
        var changed = false
        for (mission in data.dailyMissions) {
            if (!mission.claimed && mission.type == type && mission.progress < mission.target) {
                mission.progress = (mission.progress + amount).coerceAtMost(mission.target)
                changed = true
            }
        }
        if (changed) save()
    }

    fun claimMission(missionId: String): Boolean {
        val mission = data.dailyMissions.firstOrNull { it.id == missionId } ?: return false
        if (mission.claimed || mission.progress < mission.target) return false
        mission.claimed = true
        data.coins += mission.rewardCoins
        data.gems += mission.rewardGems
        save()
        return true
    }

    fun recordCampaignVictory(levelNumber: Int, stars: Int) {
        val key = levelNumber.toString()
        val prev = data.levelStars[key] ?: 0
        if (stars > prev) data.levelStars[key] = stars.coerceIn(1, 3)
        if (levelNumber >= data.highestUnlockedLevel && levelNumber < GameConfig.TOTAL_CAMPAIGN_LEVELS) {
            data.highestUnlockedLevel = levelNumber + 1
        }
        save()
    }

    fun recordEndlessRun(score: Long, wave: Int, kills: Int) {
        if (score > data.endlessBestScore) data.endlessBestScore = score
        if (wave > data.endlessBestWave) data.endlessBestWave = wave
        val dateStr = SimpleDateFormat("MMM dd", Locale.US).format(Date())
        data.localLeaderboard.add(EndlessRecord(score, wave, kills, dateStr))
        data.localLeaderboard.sortByDescending { it.score }
        while (data.localLeaderboard.size > 10) data.localLeaderboard.removeAt(data.localLeaderboard.lastIndex)
        save()
    }

    fun getLevelStars(levelNumber: Int): Int = data.levelStars[levelNumber.toString()] ?: 0
    fun currentEpochDay(): Long = System.currentTimeMillis() / 86_400_000L
    fun getSelectedBow(): BowType = runCatching { BowType.valueOf(data.selectedBow) }.getOrDefault(BowType.WOODEN)
    fun getSelectedSkin(): StickmanSkin = runCatching { StickmanSkin.valueOf(data.selectedSkin) }.getOrDefault(StickmanSkin.DEFAULT)
    fun getSelectedBowSkin(): BowSkin = runCatching { BowSkin.valueOf(data.selectedBowSkin) }.getOrDefault(BowSkin.CLASSIC)
    fun getSelectedTrail(): ArrowTrail = runCatching { ArrowTrail.valueOf(data.selectedTrail) }.getOrDefault(ArrowTrail.NONE)
    fun getOrientationMode(): OrientationMode = runCatching { OrientationMode.valueOf(data.orientationMode) }.getOrDefault(OrientationMode.LANDSCAPE)

    fun getArrowCount(type: ArrowType): Int = if (type == ArrowType.NORMAL) 999 else data.arrowInventory[type.name] ?: 0

    fun addArrowAmmo(type: ArrowType, amount: Int) {
        if (type == ArrowType.NORMAL) return
        val current = data.arrowInventory[type.name] ?: 0
        data.arrowInventory[type.name] = (current + amount).coerceAtLeast(0)
        save()
    }

    fun consumeArrowAmmo(type: ArrowType): Boolean {
        if (type == ArrowType.NORMAL) return true
        val current = data.arrowInventory[type.name] ?: 0
        if (current <= 0) return false
        data.arrowInventory[type.name] = current - 1
        return true
    }
}
