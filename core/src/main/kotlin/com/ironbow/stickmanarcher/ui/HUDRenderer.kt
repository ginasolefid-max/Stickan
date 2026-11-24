package com.ironbow.stickmanarcher.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.ironbow.stickmanarcher.assets.AssetManager
import com.ironbow.stickmanarcher.config.GameConfig
import com.ironbow.stickmanarcher.data.ArrowType
import com.ironbow.stickmanarcher.data.GameMode
import com.ironbow.stickmanarcher.data.SaveSystem
import com.ironbow.stickmanarcher.systems.GameWorld
import kotlin.math.abs

class HUDRenderer {

    private val barBg = Color(0.08f, 0.10f, 0.14f, 0.85f)
    private val hpFill = Color(0.22f, 0.82f, 0.36f, 1f)
    private val wallFill = Color(0.35f, 0.72f, 0.96f, 1f)
    private val bossFill = Color(0.90f, 0.16f, 0.24f, 1f)

    fun render(batch: SpriteBatch, assets: AssetManager, world: GameWorld, saveSystem: SaveSystem) {
        val topY = GameConfig.WORLD_HEIGHT - 36f
        assets.drawRect(batch, 24f, topY - 12f, 220f, 20f, barBg)
        assets.drawRect(batch, 26f, topY - 10f, 216f * (world.player.hp / world.player.maxHp).coerceIn(0f, 1f), 16f, hpFill)
        assets.fontSmall.draw(batch, "HERO ${world.player.hp.toInt()}/${world.player.maxHp.toInt()}", 32f, topY + 4f)

        assets.drawRect(batch, 24f, topY - 38f, 220f, 18f, barBg)
        assets.drawRect(batch, 26f, topY - 36f, 216f * (world.barricade.hp / world.barricade.maxHp).coerceIn(0f, 1f), 14f, wallFill)
        assets.fontSmall.draw(batch, "WALL ${world.barricade.hp.toInt()}/${world.barricade.maxHp.toInt()}", 32f, topY - 22f)

        val waveText = when (world.gameMode) {
            GameMode.TIME_ATTACK -> "TIME: ${world.timeAttackRemaining.toInt()}s   SCORE: ${world.sessionScore}"
            GameMode.CAMPAIGN -> "LVL ${world.levelNumber}  WAVE ${world.waveManager.currentWave}/${world.waveManager.totalWaves}"
            else -> "WAVE ${world.waveManager.currentWave}   SCORE: ${world.sessionScore}"
        }
        assets.fontMedium.draw(batch, waveText, GameConfig.WORLD_WIDTH * 0.5f - 135f, topY + 12f)

        val wind = world.waveManager.currentWindX
        val windDir = when { wind > 10f -> "WIND >>> ${wind.toInt()}"; wind < -10f -> "WIND <<< ${abs(wind).toInt()}"; else -> "WIND: CALM" }
        assets.fontSmall.color = Color(0.75f, 0.90f, 1f, 1f)
        assets.fontSmall.draw(batch, windDir, GameConfig.WORLD_WIDTH * 0.5f - 62f, topY - 18f)
        assets.fontSmall.color = Color.WHITE

        assets.fontMedium.color = Color.GOLD
        assets.fontMedium.draw(batch, "COINS: ${saveSystem.data.coins + world.sessionCoins}", GameConfig.WORLD_WIDTH - 340f, topY + 12f)
        assets.fontMedium.color = Color(0.45f, 0.92f, 1f, 1f)
        assets.fontMedium.draw(batch, "GEMS: ${saveSystem.data.gems + world.sessionGems}", GameConfig.WORLD_WIDTH - 340f, topY - 18f)
        assets.fontMedium.color = Color.WHITE

        if (world.comboCount >= 2) {
            assets.fontLarge.color = Color(1f, 0.80f, 0.20f, 1f)
            assets.fontLarge.draw(batch, "COMBO x${world.comboCount}", GameConfig.WORLD_WIDTH - 280f, topY - 58f)
            assets.fontLarge.color = Color.WHITE
        }

        world.getActiveBoss()?.let { boss ->
            val bw = 460f; val bx = GameConfig.WORLD_WIDTH * 0.5f - bw * 0.5f; val by = topY - 68f
            assets.drawRect(batch, bx - 2f, by - 2f, bw + 4f, 22f, barBg)
            assets.drawRect(batch, bx, by, bw * (boss.hp / boss.maxHp).coerceIn(0f, 1f), 18f, bossFill)
            assets.fontSmall.draw(batch, "BOSS: ${boss.type.displayName.uppercase()}", bx + 12f, by + 15f)
        }

        if (world.waveBannerTimer > 0f) {
            assets.fontTitle.setColor(1f, 0.86f, 0.30f, (world.waveBannerTimer / 0.6f).coerceIn(0f, 1f))
            assets.fontTitle.draw(batch, world.waveBannerText, GameConfig.WORLD_WIDTH * 0.5f - 170f, GameConfig.WORLD_HEIGHT * 0.62f)
            assets.fontTitle.color = Color.WHITE
        }

        if (world.tutorialStep == 0) {
            val pulseCol = Color(1f, 0.92f, 0.38f, 0.95f)
            assets.drawLimb(batch, 390f, 360f, 250f, 290f, 4f, pulseCol)
            assets.drawCircle(batch, 250f, 290f, 12f, pulseCol)
            assets.fontMedium.color = pulseCol
            assets.fontMedium.draw(batch, "TUTORIAL: DRAG BACK TO AIM, RELEASE TO SHOOT!", 240f, 420f)
            assets.fontMedium.color = Color.WHITE
        }

        val ammoCount = saveSystem.getArrowCount(world.selectedArrowType)
        val ammoStr = if (world.selectedArrowType == ArrowType.NORMAL) "INF" else ammoCount.toString()
        assets.fontSmall.draw(batch, "ACTIVE: ${world.selectedArrowType.displayName} ($ammoStr)", 24f, 92f)
    }
}
