package com.ironbow.stickmanarcher.config

object GameConfig {
    const val WORLD_WIDTH = 1280f
    const val WORLD_HEIGHT = 720f
    const val GROUND_Y = 115f

    const val PLAYER_X = 118f
    const val PLAYER_HEIGHT = 104f
    const val BARRICADE_X = 225f
    const val BARRICADE_WIDTH = 42f
    const val ZOMBIE_SPAWN_X = 1360f

    const val GRAVITY = -980f
    const val MAX_DRAG_DISTANCE = 190f
    const val MIN_DRAG_DISTANCE = 24f
    const val BASE_ARROW_SPEED = 1140f
    const val TRAJECTORY_BASE_STEPS = 16
    const val TRAJECTORY_TIME_STEP = 0.045f

    const val HEADSHOT_MULTIPLIER = 3.0f
    const val BODYSHOT_MULTIPLIER = 1.0f
    const val LEGSHOT_MULTIPLIER = 0.5f
    const val LEG_SLOW_DURATION = 2.6f
    const val LEG_SLOW_FACTOR = 0.45f

    const val BULLET_TIME_SCALE = 0.24f
    const val BULLET_TIME_MIN_DISTANCE = 540f
    const val BULLET_TIME_TRIGGER_RADIUS = 135f
    const val BULLET_TIME_MAX_DURATION = 0.75f
    const val BULLET_TIME_COOLDOWN = 2.2f

    const val FIRE_BURN_DURATION = 4.0f
    const val FIRE_DPS = 16f
    const val ICE_FREEZE_DURATION = 3.2f
    const val ICE_SLOW_FACTOR = 0.30f
    const val POISON_DURATION = 5.0f
    const val POISON_DPS = 13f
    const val EXPLOSIVE_RADIUS = 135f
    const val EXPLOSIVE_DAMAGE_RATIO = 0.85f

    const val BASE_PLAYER_HP = 100f
    const val BASE_BARRICADE_HP = 220f
    const val BARRICADE_REPAIR_COST_COINS = 40
    const val BARRICADE_REPAIR_PERCENT = 0.45f
    const val BOSS_WAVE_INTERVAL = 10
    const val TOTAL_CAMPAIGN_LEVELS = 60
    const val TIME_ATTACK_START_SECONDS = 90f

    const val POOL_ARROWS = 48
    const val POOL_ZOMBIES = 64
    const val POOL_PARTICLES = 320
    const val POOL_FLOATING_TEXT = 48
    const val POOL_PICKUPS = 24
    const val POOL_ACID = 24
    const val POOL_RAGDOLL = 96

    const val LEADERBOARD_ENDLESS_ID = "CgkI_ENDLESS_SURVIVAL_ID"
    const val SAVE_FILE_NAME = "stickman_archer_save.json"
    const val PREFS_NAME = "stickman_archer_prefs"
}
