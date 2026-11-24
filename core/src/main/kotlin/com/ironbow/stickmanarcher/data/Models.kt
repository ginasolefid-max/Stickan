package com.ironbow.stickmanarcher.data

enum class GameMode {
    CAMPAIGN,
    ENDLESS,
    BOSS_RUSH,
    TIME_ATTACK,
    DAILY_CHALLENGE
}

enum class BiomeType(val displayName: String) {
    GRAVEYARD("Graveyard"),
    FOREST("Dark Forest"),
    CITY_RUINS("City Ruins"),
    SNOW("Frozen Wastes")
}

enum class ArrowType(
    val displayName: String,
    val damageMult: Float,
    val shopPriceCoins: Int,
    val packSize: Int,
    val colorHex: Long
) {
    NORMAL("Normal", 1.0f, 0, 999, 0xE6C280FF),
    FIRE("Fire", 1.25f, 80, 12, 0xFF6B2BFF),
    ICE("Ice", 1.10f, 85, 12, 0x4FD8FFFF),
    EXPLOSIVE("Explosive", 1.60f, 130, 8, 0xFF334BFF),
    PIERCING("Piercing", 1.35f, 110, 10, 0xD4D8FFFF),
    POISON("Poison", 1.15f, 95, 10, 0x62E84AFF),
    TRIPLE_SHOT("Triple", 0.78f, 120, 8, 0xFFD94AFF)
}

enum class BowType(
    val displayName: String,
    val damageMultiplier: Float,
    val drawSpeedMultiplier: Float,
    val accuracy: Float,
    val velocityMultiplier: Float,
    val costCoins: Int,
    val costGems: Int
) {
    WOODEN("Wooden Bow", 1.0f, 1.0f, 0.90f, 1.0f, 0, 0),
    HUNTER("Hunter Bow", 1.28f, 1.15f, 0.94f, 1.08f, 450, 0),
    COMPOSITE("Composite Bow", 1.55f, 1.28f, 0.97f, 1.16f, 1200, 0),
    CROSSBOW("Siege Crossbow", 1.85f, 1.38f, 0.99f, 1.28f, 2400, 15),
    LEGENDARY("Dragonbone Bow", 2.35f, 1.55f, 1.00f, 1.38f, 0, 65)
}

enum class ZombieType(
    val displayName: String,
    val baseHp: Float,
    val baseSpeed: Float,
    val attackDamage: Float,
    val coinReward: Int,
    val scoreReward: Int,
    val scale: Float
) {
    WALKER("Walker", 46f, 52f, 10f, 8, 25, 1.0f),
    RUNNER("Runner", 30f, 128f, 8f, 10, 35, 0.94f),
    TANK("Armored Tank", 165f, 34f, 18f, 22, 75, 1.24f),
    JUMPER("Leaper", 52f, 86f, 12f, 14, 50, 0.98f),
    SPITTER("Acid Spitter", 44f, 46f, 12f, 15, 55, 1.0f),
    EXPLODER("Volatile", 58f, 74f, 32f, 18, 65, 1.08f),
    BOSS("Siege Overlord", 780f, 28f, 30f, 120, 450, 1.78f)
}

enum class StickmanSkin(val displayName: String, val costCoins: Int, val costGems: Int) {
    DEFAULT("Ranger", 0, 0),
    NINJA("Shadow Ninja", 600, 0),
    KNIGHT("Iron Knight", 1100, 0),
    ROBOT("Cyber Unit", 0, 25),
    PIRATE("Corsair", 900, 10)
}

enum class BowSkin(val displayName: String, val costCoins: Int, val costGems: Int) {
    CLASSIC("Oak Finish", 0, 0),
    CRIMSON("Crimson Ember", 400, 0),
    SHADOW("Obsidian", 750, 0),
    EMERALD("Venom Jade", 0, 18),
    GOLD_RUNE("Royal Gold", 0, 35)
}

enum class ArrowTrail(val displayName: String, val costCoins: Int, val costGems: Int) {
    NONE("Classic", 0, 0),
    EMBER("Ember Sparks", 350, 0),
    FROST("Frost Mist", 500, 0),
    NEON("Arcane Neon", 0, 15),
    SHADOW_GOLD("Golden Comet", 0, 30)
}

enum class AllyType(val displayName: String, val description: String, val costCoins: Int, val costGems: Int) {
    GUNNER("Marksman Ally", "Fires rifle rounds from behind the barricade.", 1400, 0),
    TRAP_LAYER("Trapper", "Deploys spike traps that damage & slow zombies.", 1100, 0),
    DOG("War Hound", "Rushes forward to bite approaching undead.", 0, 25)
}

enum class UpgradeType(
    val displayName: String,
    val description: String,
    val maxLevel: Int,
    val baseCost: Int
) {
    DAMAGE("Arrow Power", "+12% arrow damage per rank", 15, 90),
    DRAW_SPEED("Quick Draw", "+8% faster bow draw per rank", 12, 85),
    ARROW_COUNT("Deep Quiver", "+3 special arrows per wave", 10, 100),
    CRIT_CHANCE("Deadly Precision", "+4% critical hit chance", 12, 110),
    HEALTH("Fortitude", "+25 Hero & +40 Barricade HP", 15, 80),
    EAGLE_EYE("Eagle Eye", "Extends dotted trajectory preview", 10, 95),
    BARRICADE_ARMOR("Iron Wall", "+15% barricade damage reduction", 10, 120)
}

enum class OrientationMode { LANDSCAPE, PORTRAIT }
enum class HitZone { HEAD, BODY, LEGS }
enum class MissionType { KILL_ZOMBIES, SCORE_HEADSHOTS, CLEAR_WAVES }

data class DailyMission(
    var id: String = "",
    var type: MissionType = MissionType.KILL_ZOMBIES,
    var title: String = "",
    var target: Int = 10,
    var progress: Int = 0,
    var rewardCoins: Int = 100,
    var rewardGems: Int = 2,
    var claimed: Boolean = false
)

data class EndlessRecord(
    var score: Long = 0L,
    var wave: Int = 0,
    var kills: Int = 0,
    var dateLabel: String = ""
)

data class LevelConfig(
    val levelNumber: Int,
    val biome: BiomeType,
    val waveCount: Int,
    val baseZombiesPerWave: Int,
    val windRange: Float,
    val hasBoss: Boolean,
    val rewardCoins: Int,
    val rewardGems: Int
)

data class SaveData(
    var coins: Int = 180,
    var gems: Int = 5,
    var highestUnlockedLevel: Int = 1,
    var levelStars: HashMap<String, Int> = hashMapOf(),
    var selectedBow: String = BowType.WOODEN.name,
    var unlockedBows: ArrayList<String> = arrayListOf(BowType.WOODEN.name),
    var selectedSkin: String = StickmanSkin.DEFAULT.name,
    var unlockedSkins: ArrayList<String> = arrayListOf(StickmanSkin.DEFAULT.name),
    var selectedBowSkin: String = BowSkin.CLASSIC.name,
    var unlockedBowSkins: ArrayList<String> = arrayListOf(BowSkin.CLASSIC.name),
    var selectedTrail: String = ArrowTrail.NONE.name,
    var unlockedTrails: ArrayList<String> = arrayListOf(ArrowTrail.NONE.name),
    var unlockedAllies: ArrayList<String> = arrayListOf(),
    var activeAllies: ArrayList<String> = arrayListOf(),
    var arrowInventory: HashMap<String, Int> = hashMapOf(
        ArrowType.FIRE.name to 6,
        ArrowType.ICE.name to 6,
        ArrowType.EXPLOSIVE.name to 3,
        ArrowType.PIERCING.name to 4,
        ArrowType.POISON.name to 4,
        ArrowType.TRIPLE_SHOT.name to 3
    ),
    var upgradeLevels: HashMap<String, Int> = hashMapOf(),
    var lastLoginEpochDay: Long = -1L,
    var loginStreak: Int = 0,
    var dailyRewardClaimedToday: Boolean = false,
    var dailyChallengeCompletedDay: Long = -1L,
    var lastMissionDay: Long = -1L,
    var dailyMissions: ArrayList<DailyMission> = arrayListOf(),
    var endlessBestScore: Long = 0L,
    var endlessBestWave: Int = 0,
    var timeAttackBestScore: Long = 0L,
    var bossRushBestWave: Int = 0,
    var localLeaderboard: ArrayList<EndlessRecord> = arrayListOf(),
    var tutorialCompleted: Boolean = false,
    var bloodEnabled: Boolean = false,
    var trajectoryPreviewEnabled: Boolean = true,
    var leftHandedMode: Boolean = false,
    var aimSensitivity: Float = 1.0f,
    var sfxVolume: Float = 0.85f,
    var musicVolume: Float = 0.55f,
    var vibrationEnabled: Boolean = true,
    var orientationMode: String = OrientationMode.LANDSCAPE.name
)
