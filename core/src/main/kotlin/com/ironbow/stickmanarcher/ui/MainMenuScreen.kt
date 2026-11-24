package com.ironbow.stickmanarcher.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Dialog
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.ironbow.stickmanarcher.StickmanArcherGame
import com.ironbow.stickmanarcher.assets.SfxType
import com.ironbow.stickmanarcher.config.GameConfig
import com.ironbow.stickmanarcher.data.GameMode

class MainMenuScreen(private val game: StickmanArcherGame) : ScreenAdapter() {

    private val viewport = ExtendViewport(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT)
    private val stage = Stage(viewport, game.batch)
    private val skin = game.assetManager.uiSkin
    private val save = game.saveSystem

    init { buildUI() }

    private fun buildUI() {
        stage.clear()
        val root = Table().apply { setFillParent(true); pad(24f) }
        val topBar = Table()
        topBar.add(Label("COINS: ${save.data.coins}", skin, "gold")).left().padRight(24f)
        topBar.add(Label("GEMS: ${save.data.gems}", skin, "default")).left().expandX()
        val dailyBtn = TextButton(if (save.data.dailyRewardClaimedToday) "DAILY REWARDS" else "CLAIM DAILY!", skin, if (save.data.dailyRewardClaimedToday) "small" else "accent")
        dailyBtn.addListener(click { showDailyDialog() })
        topBar.add(dailyBtn).width(210f).height(46f).padRight(10f)
        val lbBtn = TextButton("LEADERBOARD", skin, "small")
        lbBtn.addListener(click { showLeaderboardDialog() })
        topBar.add(lbBtn).width(190f).height(46f)
        root.add(topBar).growX().row()

        root.add(Label("STICKMAN ARCHER: ZOMBIE SIEGE", skin, "title")).padTop(34f).row()
        root.add(Label("Bow: ${save.getSelectedBow().displayName}   |   Skin: ${save.getSelectedSkin().displayName}", skin, "small")).padBottom(28f).row()

        val btnTable = Table()
        val playCampaignBtn = TextButton("PLAY CAMPAIGN (LVL ${save.data.highestUnlockedLevel})", skin, "accent")
        playCampaignBtn.addListener(click { game.uiManager.startGameplay(GameMode.CAMPAIGN, save.data.highestUnlockedLevel) })
        val modesBtn = TextButton("LEVELS & MODES", skin).apply { addListener(click { game.uiManager.showLevelSelect() }) }
        val shopBtn = TextButton("ARMORY & SHOP", skin).apply { addListener(click { game.uiManager.showShop() }) }
        val upgradeBtn = TextButton("UPGRADE TREE", skin).apply { addListener(click { game.uiManager.showUpgrades() }) }
        val settingsBtn = TextButton("SETTINGS", skin).apply { addListener(click { game.uiManager.showSettings() }) }

        btnTable.add(playCampaignBtn).width(420f).height(64f).colspan(2).padBottom(14f).row()
        btnTable.add(modesBtn).width(204f).height(56f).pad(6f)
        btnTable.add(shopBtn).width(204f).height(56f).pad(6f).row()
        btnTable.add(upgradeBtn).width(204f).height(56f).pad(6f)
        btnTable.add(settingsBtn).width(204f).height(56f).pad(6f).row()
        root.add(btnTable).expand().center()
        stage.addActor(root)
    }

    private fun showDailyDialog() {
        val dlg = Dialog("DAILY LOGIN & MISSIONS", skin)
        val content = dlg.contentTable; content.pad(16f)
        content.add(Label("Login Streak: Day ${save.data.loginStreak} / 7", skin, "gold")).left().row()
        if (!save.data.dailyRewardClaimedToday) {
            val claimLogin = TextButton("CLAIM TODAY'S REWARD", skin, "accent")
            claimLogin.addListener(click {
                save.claimDailyLoginReward()?.let { res ->
                    game.platformServices.showToast("+${res.first} Coins, +${res.second} Gems!")
                    dlg.hide(); buildUI()
                }
            })
            content.add(claimLogin).width(280f).height(46f).pad(10f).row()
        } else content.add(Label("Daily login reward claimed today.", skin, "small")).padBottom(10f).row()

        content.add(Label("--- Daily Missions ---", skin)).padTop(8f).row()
        for (m in save.data.dailyMissions) {
            val row = Table()
            row.add(Label("${m.title} (${m.progress}/${m.target})", skin, "small")).left().expandX().padRight(12f)
            val statusBtn = when {
                m.claimed -> TextButton("CLAIMED", skin, "small").apply { isDisabled = true }
                m.progress >= m.target -> TextButton("CLAIM", skin, "accent").apply { addListener(click { if (save.claimMission(m.id)) { dlg.hide(); buildUI() } }) }
                else -> TextButton("+${m.rewardCoins}c", skin, "small").apply { isDisabled = true }
            }
            row.add(statusBtn).width(120f).height(38f)
            content.add(row).growX().pad(4f).row()
        }
        dlg.button("CLOSE"); dlg.show(stage)
    }

    private fun showLeaderboardDialog() {
        game.platformServices.showLeaderboard(GameConfig.LEADERBOARD_ENDLESS_ID) {
            val dlg = Dialog("ENDLESS LEADERBOARD (LOCAL)", skin)
            val c = dlg.contentTable; c.pad(16f)
            if (save.data.localLeaderboard.isEmpty()) c.add(Label("No Endless Survival runs recorded yet.", skin, "small")).row()
            else save.data.localLeaderboard.forEachIndexed { idx, rec ->
                c.add(Label("#${idx + 1}  Score: ${rec.score}  |  Wave ${rec.wave}  |  ${rec.dateLabel}", skin, "small")).left().pad(4f).row()
            }
            dlg.button("CLOSE"); dlg.show(stage)
        }
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
