package com.ironbow.stickmanarcher.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.ironbow.stickmanarcher.StickmanArcherGame
import com.ironbow.stickmanarcher.assets.SfxType
import com.ironbow.stickmanarcher.config.GameConfig
import com.ironbow.stickmanarcher.data.ArrowType
import com.ironbow.stickmanarcher.data.GameMode
import com.ironbow.stickmanarcher.systems.GameWorld
import kotlin.random.Random

class GameScreen(
    private val game: StickmanArcherGame,
    private val mode: GameMode,
    private val levelNumber: Int
) : ScreenAdapter() {

    private val worldCamera = OrthographicCamera()
    private val worldViewport = ExtendViewport(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT, worldCamera)
    private val uiViewport = ExtendViewport(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT)
    private val uiStage = Stage(uiViewport, game.batch)
    private val skin = game.assetManager.uiSkin
    private val world = GameWorld(game.saveSystem, game.upgradeManager, game.audioManager, game.platformServices)
    private val hudRenderer = HUDRenderer()
    private val tmpTouch = Vector2()

    private var overlayShowing: GameWorld.MatchState? = null
    private lateinit var overlayTable: Table
    private val arrowButtons = HashMap<ArrowType, TextButton>()

    init {
        world.startMatch(mode, levelNumber)
        buildHudControls()
    }

    private fun buildHudControls() {
        uiStage.clear()
        val root = Table().apply { setFillParent(true); pad(14f) }
        val topBar = Table()
        topBar.add().expandX()
        topBar.add(TextButton("REPAIR (${GameConfig.BARRICADE_REPAIR_COST_COINS}c)", skin, "small").apply {
            addListener(click { world.repairBarricade() })
        }).width(165f).height(42f).padRight(8f)
        topBar.add(TextButton("PAUSE", skin, "small").apply {
            addListener(click {
                if (world.matchState == GameWorld.MatchState.PLAYING) {
                    world.matchState = GameWorld.MatchState.PAUSED
                    showOverlay(GameWorld.MatchState.PAUSED)
                }
            })
        }).width(105f).height(42f)
        root.add(topBar).growX().top().row()
        root.add().expand().row()

        val arrowBar = Table()
        arrowButtons.clear()
        if (mode != GameMode.DAILY_CHALLENGE) {
            for (type in ArrowType.entries) {
                val count = game.saveSystem.getArrowCount(type)
                val label = if (type == ArrowType.NORMAL) "NORM\nINF" else "${type.displayName.take(4).uppercase()}\n$count"
                val btn = TextButton(label, skin, if (world.selectedArrowType == type) "accent" else "small").apply {
                    addListener(click {
                        if (type == ArrowType.NORMAL || game.saveSystem.getArrowCount(type) > 0) {
                            world.selectedArrowType = type; refreshArrowButtons()
                        } else game.platformServices.showToast("Out of ${type.displayName} arrows!")
                    })
                }
                arrowButtons[type] = btn
                arrowBar.add(btn).width(92f).height(52f).pad(3f)
            }
        }
        root.add(arrowBar).bottom().left()
        overlayTable = Table().apply { setFillParent(true); isVisible = false }
        uiStage.addActor(root); uiStage.addActor(overlayTable)
    }

    private fun refreshArrowButtons() {
        for ((type, btn) in arrowButtons) {
            val count = game.saveSystem.getArrowCount(type)
            btn.setText(if (type == ArrowType.NORMAL) "NORM\nINF" else "${type.displayName.take(4).uppercase()}\n$count")
            btn.style = skin.get(if (world.selectedArrowType == type) "accent" else "small", TextButton.TextButtonStyle::class.java)
        }
    }

    private fun showOverlay(state: GameWorld.MatchState) {
        overlayShowing = state; overlayTable.clear(); overlayTable.isVisible = true
        val box = Table(skin).apply { background = skin.getDrawable("panel"); pad(26f) }
        when (state) {
            GameWorld.MatchState.PAUSED -> {
                box.add(Label("GAME PAUSED", skin, "large")).padBottom(18f).row()
                box.add(TextButton("RESUME", skin, "accent").apply { addListener(click { world.matchState = GameWorld.MatchState.PLAYING; hideOverlay() }) }).width(260f).height(52f).pad(6f).row()
                box.add(TextButton("RESTART", skin).apply { addListener(click { hideOverlay(); world.startMatch(mode, levelNumber); refreshArrowButtons() }) }).width(260f).height(52f).pad(6f).row()
                box.add(TextButton("MAIN MENU", skin).apply { addListener(click { game.uiManager.showMainMenu() }) }).width(260f).height(52f).pad(6f).row()
            }
            GameWorld.MatchState.VICTORY -> {
                val stars = world.computeStars()
                box.add(Label("VICTORY!", skin, "title")).padBottom(8f).row()
                box.add(Label("★".repeat(stars) + "☆".repeat(3 - stars), skin, "gold")).padBottom(10f).row()
                box.add(Label("Kills: ${world.sessionKills}   |   Score: ${world.sessionScore}", skin)).padBottom(6f).row()
                box.add(Label("Earned: +${world.sessionCoins} Coins, +${world.sessionGems} Gems", skin, "gold")).padBottom(16f).row()
                if (!world.coinsDoubled && world.sessionCoins > 0) {
                    box.add(TextButton("WATCH AD: 2X COINS", skin, "accent").apply {
                        addListener(click {
                            game.platformServices.showRewardedAd("double_coins", {
                                world.coinsDoubled = true; game.saveSystem.data.coins += world.sessionCoins; game.saveSystem.save()
                                world.sessionCoins *= 2; showOverlay(GameWorld.MatchState.VICTORY)
                            }, { msg -> game.platformServices.showToast(msg) })
                        })
                    }).width(280f).height(50f).pad(6f).row()
                }
                if (mode == GameMode.CAMPAIGN && levelNumber < GameConfig.TOTAL_CAMPAIGN_LEVELS) {
                    box.add(TextButton("NEXT LEVEL", skin, "accent").apply { addListener(click { game.uiManager.startGameplay(GameMode.CAMPAIGN, levelNumber + 1) }) }).width(280f).height(52f).pad(6f).row()
                }
                box.add(TextButton("MAIN MENU", skin).apply { addListener(click { game.uiManager.showMainMenu() }) }).width(280f).height(50f).pad(6f).row()
            }
            GameWorld.MatchState.GAME_OVER -> {
                box.add(Label("SIEGE FALLEN", skin, "title")).padBottom(10f).row()
                box.add(Label("Wave Reached: ${world.waveManager.currentWave}   |   Kills: ${world.sessionKills}", skin)).padBottom(6f).row()
                box.add(Label("Earned: +${world.sessionCoins} Coins", skin, "gold")).padBottom(16f).row()
                if (!world.usedRevive) {
                    box.add(TextButton("WATCH AD: REVIVE", skin, "accent").apply {
                        addListener(click {
                            game.platformServices.showRewardedAd("revive", { hideOverlay(); world.reviveFromAd() }, { msg -> game.platformServices.showToast(msg) })
                        })
                    }).width(280f).height(52f).pad(6f).row()
                }
                box.add(TextButton("RETRY", skin).apply { addListener(click { hideOverlay(); world.startMatch(mode, levelNumber); refreshArrowButtons() }) }).width(280f).height(50f).pad(6f).row()
                box.add(TextButton("MAIN MENU", skin).apply { addListener(click { game.uiManager.showMainMenu() }) }).width(280f).height(50f).pad(6f).row()
            }
            else -> {}
        }
        overlayTable.add(box)
    }

    private fun hideOverlay() { overlayShowing = null; overlayTable.isVisible = false }

    private val aimInput = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (pointer != 0 || world.matchState != GameWorld.MatchState.PLAYING) return false
            tmpTouch.set(screenX.toFloat(), screenY.toFloat()); worldViewport.unproject(tmpTouch)
            world.onTouchDown(tmpTouch.x, tmpTouch.y); return true
        }
        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            if (pointer != 0) return false
            tmpTouch.set(screenX.toFloat(), screenY.toFloat()); worldViewport.unproject(tmpTouch)
            world.onTouchDragged(tmpTouch.x, tmpTouch.y); return true
        }
        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (pointer != 0) return false
            world.onTouchUp(); refreshArrowButtons(); return true
        }
    }

    private fun click(action: () -> Unit) = object : ClickListener() {
        override fun clicked(event: InputEvent?, x: Float, y: Float) { game.audioManager.play(SfxType.UI_CLICK); action() }
    }

    override fun show() { Gdx.input.inputProcessor = InputMultiplexer(uiStage, aimInput) }

    override fun render(delta: Float) {
        world.update(delta)
        if (world.matchState != GameWorld.MatchState.PLAYING && overlayShowing != world.matchState) showOverlay(world.matchState)
        val shakeX = if (world.screenShakeTimer > 0f) (Random.nextFloat() * 2f - 1f) * world.screenShakeIntensity else 0f
        val shakeY = if (world.screenShakeTimer > 0f) (Random.nextFloat() * 2f - 1f) * world.screenShakeIntensity else 0f
        worldCamera.zoom = if (world.bulletTimeTimer > 0f) 0.90f else 1.0f
        worldCamera.position.set(GameConfig.WORLD_WIDTH * 0.5f + world.cameraFocusX + shakeX, GameConfig.WORLD_HEIGHT * 0.5f + shakeY, 0f)
        worldCamera.update()

        Gdx.gl.glClearColor(0.05f, 0.06f, 0.10f, 1f); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        worldViewport.apply(); game.batch.projectionMatrix = worldCamera.combined
        game.batch.begin(); world.render(game.batch, game.assetManager); game.batch.end()

        uiViewport.apply(); game.batch.projectionMatrix = uiViewport.camera.combined
        game.batch.begin(); hudRenderer.render(game.batch, game.assetManager, world, game.saveSystem); game.batch.end()
        uiStage.act(delta); uiStage.draw()
    }

    override fun resize(width: Int, height: Int) {
        worldViewport.update(width, height, true); uiViewport.update(width, height, true)
    }

    override fun dispose() { uiStage.dispose() }
}
