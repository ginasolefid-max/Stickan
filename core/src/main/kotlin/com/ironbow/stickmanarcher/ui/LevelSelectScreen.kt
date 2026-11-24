package com.ironbow.stickmanarcher.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.ironbow.stickmanarcher.StickmanArcherGame
import com.ironbow.stickmanarcher.assets.SfxType
import com.ironbow.stickmanarcher.config.GameConfig
import com.ironbow.stickmanarcher.data.BiomeType
import com.ironbow.stickmanarcher.data.GameMode
import com.ironbow.stickmanarcher.data.LevelData

class LevelSelectScreen(private val game: StickmanArcherGame) : ScreenAdapter() {

    private val viewport = ExtendViewport(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT)
    private val stage = Stage(viewport, game.batch)
    private val skin = game.assetManager.uiSkin
    private val save = game.saveSystem
    private var selectedBiome = BiomeType.GRAVEYARD

    init { buildUI() }

    private fun buildUI() {
        stage.clear()
        val root = Table().apply { setFillParent(true); pad(20f) }
        val header = Table()
        header.add(TextButton("< BACK", skin, "small").apply { addListener(click { game.uiManager.showMainMenu() }) }).width(120f).height(44f).left()
        header.add(Label("GAME MODES & CAMPAIGN", skin, "large")).expandX().center()
        root.add(header).growX().padBottom(14f).row()

        val modeRow = Table()
        modeRow.add(TextButton("ENDLESS\nBest: ${save.data.endlessBestScore}", skin, "accent").apply { addListener(click { game.uiManager.startGameplay(GameMode.ENDLESS) }) }).width(240f).height(68f).pad(6f)
        modeRow.add(TextButton("BOSS RUSH\nBest W${save.data.bossRushBestWave}", skin).apply { addListener(click { game.uiManager.startGameplay(GameMode.BOSS_RUSH) }) }).width(240f).height(68f).pad(6f)
        modeRow.add(TextButton("TIME ATTACK\nBest: ${save.data.timeAttackBestScore}", skin).apply { addListener(click { game.uiManager.startGameplay(GameMode.TIME_ATTACK) }) }).width(240f).height(68f).pad(6f)
        val dailyRules = LevelData.getDailyChallengeRules(save.currentEpochDay())
        modeRow.add(TextButton("DAILY CHALLENGE\n${dailyRules.forcedArrowType.displayName}", skin).apply { addListener(click { game.uiManager.startGameplay(GameMode.DAILY_CHALLENGE) }) }).width(260f).height(68f).pad(6f)
        root.add(modeRow).padBottom(16f).row()

        val biomeRow = Table()
        for (biome in BiomeType.entries) {
            biomeRow.add(TextButton(biome.displayName, skin, if (biome == selectedBiome) "accent" else "small").apply {
                addListener(click { selectedBiome = biome; buildUI() })
            }).width(210f).height(44f).pad(4f)
        }
        root.add(biomeRow).padBottom(12f).row()

        val grid = Table()
        LevelData.getAllCampaignLevels().filter { it.biome == selectedBiome }.forEachIndexed { idx, cfg ->
            val unlocked = cfg.levelNumber <= save.data.highestUnlockedLevel
            val stars = save.getLevelStars(cfg.levelNumber)
            val label = if (unlocked) "LVL ${cfg.levelNumber}${if (cfg.hasBoss) " [BOSS]" else ""}\n${"★".repeat(stars)}" else "LVL ${cfg.levelNumber}\nLOCKED"
            val btn = TextButton(label, skin, if (unlocked) "default" else "small").apply {
                isDisabled = !unlocked
                if (unlocked) addListener(click { game.uiManager.startGameplay(GameMode.CAMPAIGN, cfg.levelNumber) })
            }
            grid.add(btn).width(195f).height(76f).pad(8f)
            if ((idx + 1) % 5 == 0) grid.row()
        }
        root.add(ScrollPane(grid, skin)).grow()
        stage.addActor(root)
    }

    private fun click(action: () -> Unit) = object : ClickListener() {
        override fun clicked(event: InputEvent?, x: Float, y: Float) { game.audioManager.play(SfxType.UI_CLICK); action() }
    }

    override fun show() { Gdx.input.inputProcessor = stage }
    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.07f, 0.09f, 0.13f, 1f); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        stage.act(delta); stage.draw()
    }
    override fun resize(width: Int, height: Int) { viewport.update(width, height, true) }
    override fun dispose() { stage.dispose() }
}
