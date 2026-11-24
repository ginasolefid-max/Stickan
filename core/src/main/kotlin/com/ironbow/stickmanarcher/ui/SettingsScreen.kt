package com.ironbow.stickmanarcher.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.ironbow.stickmanarcher.StickmanArcherGame
import com.ironbow.stickmanarcher.assets.SfxType
import com.ironbow.stickmanarcher.config.GameConfig
import com.ironbow.stickmanarcher.data.OrientationMode

class SettingsScreen(private val game: StickmanArcherGame) : ScreenAdapter() {

    private val viewport = ExtendViewport(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT)
    private val stage = Stage(viewport, game.batch)
    private val skin = game.assetManager.uiSkin
    private val save = game.saveSystem

    init { buildUI() }

    private fun buildUI() {
        stage.clear()
        val root = Table().apply { setFillParent(true); pad(24f) }
        val header = Table()
        header.add(TextButton("< BACK", skin, "small").apply { addListener(click { save.save(); game.uiManager.showMainMenu() }) }).width(120f).height(44f).left()
        header.add(Label("SETTINGS", skin, "large")).expandX().center()
        root.add(header).growX().padBottom(18f).row()

        val form = Table()
        val sfxSlider = Slider(0f, 1f, 0.05f, false, skin).apply {
            value = save.data.sfxVolume
            addListener(object : ChangeListener() { override fun changed(event: ChangeEvent?, actor: Actor?) { save.data.sfxVolume = value } })
        }
        val musicSlider = Slider(0f, 1f, 0.05f, false, skin).apply {
            value = save.data.musicVolume
            addListener(object : ChangeListener() { override fun changed(event: ChangeEvent?, actor: Actor?) { save.data.musicVolume = value } })
        }
        val sensSlider = Slider(0.5f, 1.8f, 0.05f, false, skin).apply {
            value = save.data.aimSensitivity
            addListener(object : ChangeListener() { override fun changed(event: ChangeEvent?, actor: Actor?) { save.data.aimSensitivity = value } })
        }
        form.add(Label("SFX Volume", skin)).left().padRight(24f).padBottom(10f); form.add(sfxSlider).width(320f).padBottom(10f).row()
        form.add(Label("Music Volume", skin)).left().padRight(24f).padBottom(10f); form.add(musicSlider).width(320f).padBottom(10f).row()
        form.add(Label("Aim Sensitivity", skin)).left().padRight(24f).padBottom(10f); form.add(sensSlider).width(320f).padBottom(10f).row()

        val trajBox = CheckBox(" Show Dotted Trajectory Preview", skin).apply {
            isChecked = save.data.trajectoryPreviewEnabled
            addListener(object : ChangeListener() { override fun changed(event: ChangeEvent?, actor: Actor?) { save.data.trajectoryPreviewEnabled = isChecked } })
        }
        val leftBox = CheckBox(" Left-Handed Aiming Mode", skin).apply {
            isChecked = save.data.leftHandedMode
            addListener(object : ChangeListener() { override fun changed(event: ChangeEvent?, actor: Actor?) { save.data.leftHandedMode = isChecked } })
        }
        val bloodBox = CheckBox(" Blood Effects (Family-Friendly = Off)", skin).apply {
            isChecked = save.data.bloodEnabled
            addListener(object : ChangeListener() { override fun changed(event: ChangeEvent?, actor: Actor?) { save.data.bloodEnabled = isChecked } })
        }
        val vibBox = CheckBox(" Haptic Vibration", skin).apply {
            isChecked = save.data.vibrationEnabled
            addListener(object : ChangeListener() { override fun changed(event: ChangeEvent?, actor: Actor?) { save.data.vibrationEnabled = isChecked } })
        }
        form.add(trajBox).left().colspan(2).pad(8f).row()
        form.add(leftBox).left().colspan(2).pad(8f).row()
        form.add(bloodBox).left().colspan(2).pad(8f).row()
        form.add(vibBox).left().colspan(2).pad(8f).row()

        val currentMode = save.getOrientationMode()
        form.add(TextButton("ORIENTATION: ${currentMode.name}", skin).apply {
            addListener(click {
                val next = if (save.getOrientationMode() == OrientationMode.LANDSCAPE) OrientationMode.PORTRAIT else OrientationMode.LANDSCAPE
                save.data.orientationMode = next.name; save.save(); game.platformServices.setOrientation(next); buildUI()
            })
        }).width(320f).height(48f).padTop(12f).padRight(12f)

        form.add(TextButton("REPLAY AIM TUTORIAL", skin, "small").apply {
            addListener(click { save.data.tutorialCompleted = false; save.save(); game.platformServices.showToast("Aim tutorial will appear on your next battle.") })
        }).width(240f).height(48f).padTop(12f).row()

        root.add(form).expand(); stage.addActor(root)
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
