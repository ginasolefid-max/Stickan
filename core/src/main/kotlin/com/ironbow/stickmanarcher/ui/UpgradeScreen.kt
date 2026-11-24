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
import com.ironbow.stickmanarcher.data.UpgradeType

class UpgradeScreen(private val game: StickmanArcherGame) : ScreenAdapter() {

    private val viewport = ExtendViewport(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT)
    private val stage = Stage(viewport, game.batch)
    private val skin = game.assetManager.uiSkin
    private val save = game.saveSystem
    private val upgrades = game.upgradeManager

    init { buildUI() }

    private fun buildUI() {
        stage.clear()
        val root = Table().apply { setFillParent(true); pad(20f) }
        val header = Table()
        header.add(TextButton("< BACK", skin, "small").apply { addListener(click { game.uiManager.showMainMenu() }) }).width(120f).height(44f).left()
        header.add(Label("UPGRADE TREE & BARRICADE", skin, "large")).expandX().center()
        header.add(Label("COINS: ${save.data.coins}", skin, "gold")).right()
        root.add(header).growX().padBottom(14f).row()

        val list = Table().pad(10f)
        for (type in UpgradeType.entries) {
            val lvl = upgrades.getUpgradeLevel(type); val maxed = upgrades.isMaxed(type); val cost = upgrades.getNextUpgradeCost(type)
            val row = Table(); val info = Table()
            info.add(Label("${type.displayName}  [Rank $lvl / ${type.maxLevel}]", skin)).left().row()
            info.add(Label(type.description, skin, "small")).left()
            row.add(info).left().expandX().padRight(16f)
            val btn = TextButton(if (maxed) "MAXED" else "UPGRADE ($cost c)", skin, if (maxed) "small" else "accent").apply {
                isDisabled = maxed || save.data.coins < cost
                if (!maxed) addListener(click { if (upgrades.purchaseUpgrade(type)) buildUI() })
            }
            row.add(btn).width(220f).height(54f)
            list.add(row).growX().pad(8f).row()
        }
        root.add(ScrollPane(list, skin)).grow()
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
