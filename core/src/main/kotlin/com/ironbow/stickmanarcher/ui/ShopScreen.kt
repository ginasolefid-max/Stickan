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
import com.ironbow.stickmanarcher.data.AllyType
import com.ironbow.stickmanarcher.data.ArrowTrail
import com.ironbow.stickmanarcher.data.ArrowType
import com.ironbow.stickmanarcher.data.BowSkin
import com.ironbow.stickmanarcher.data.BowType
import com.ironbow.stickmanarcher.data.StickmanSkin

class ShopScreen(private val game: StickmanArcherGame) : ScreenAdapter() {

    private enum class Tab { BOWS, ARROWS, SKINS, TRAILS, ALLIES, GEMS }

    private val viewport = ExtendViewport(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT)
    private val stage = Stage(viewport, game.batch)
    private val skin = game.assetManager.uiSkin
    private val save = game.saveSystem
    private val upgrades = game.upgradeManager
    private var currentTab = Tab.BOWS

    init { buildUI() }

    private fun buildUI() {
        stage.clear()
        val root = Table().apply { setFillParent(true); pad(20f) }
        val header = Table()
        header.add(TextButton("< BACK", skin, "small").apply { addListener(click { game.uiManager.showMainMenu() }) }).width(120f).height(44f).left()
        header.add(Label("ARMORY & SHOP", skin, "large")).expandX().center()
        header.add(Label("COINS: ${save.data.coins}   GEMS: ${save.data.gems}", skin, "gold")).right()
        root.add(header).growX().padBottom(12f).row()

        val tabs = Table()
        for (t in Tab.entries) {
            tabs.add(TextButton(t.name, skin, if (t == currentTab) "accent" else "small").apply {
                addListener(click { currentTab = t; buildUI() })
            }).width(145f).height(44f).pad(4f)
        }
        root.add(tabs).padBottom(12f).row()

        val list = Table().pad(10f)
        when (currentTab) {
            Tab.BOWS -> for (bow in BowType.entries) {
                val unlocked = save.data.unlockedBows.contains(bow.name); val eq = save.getSelectedBow() == bow
                val btnTxt = if (eq) "EQUIPPED" else if (unlocked) "EQUIP" else formatCost(bow.costCoins, bow.costGems)
                addRow(list, bow.displayName, "DMG x${bow.damageMultiplier}  SPD x${bow.drawSpeedMultiplier}  ACC ${(bow.accuracy * 100).toInt()}%", btnTxt, eq) {
                    if (upgrades.buyOrEquipBow(bow)) buildUI() else game.platformServices.showToast("Not enough currency!")
                }
            }
            Tab.ARROWS -> for (arrow in ArrowType.entries) {
                if (arrow == ArrowType.NORMAL) continue
                addRow(list, "${arrow.displayName} Arrows (Owned: ${save.getArrowCount(arrow)})", "Pack of ${arrow.packSize}  |  Damage x${arrow.damageMult}", "BUY ${arrow.shopPriceCoins}c", false) {
                    if (upgrades.buyArrowPack(arrow)) buildUI() else game.platformServices.showToast("Need ${arrow.shopPriceCoins} Coins!")
                }
            }
            Tab.SKINS -> {
                list.add(Label("--- Stickman Skins ---", skin, "gold")).left().pad(6f).row()
                for (s in StickmanSkin.entries) {
                    val unlocked = save.data.unlockedSkins.contains(s.name); val eq = save.getSelectedSkin() == s
                    addRow(list, s.displayName, "Character Appearance", if (eq) "EQUIPPED" else if (unlocked) "EQUIP" else formatCost(s.costCoins, s.costGems), eq) {
                        if (upgrades.buyOrEquipSkin(s)) buildUI() else game.platformServices.showToast("Not enough currency!")
                    }
                }
                list.add(Label("--- Bow Finishes ---", skin, "gold")).left().padTop(14f).row()
                for (bs in BowSkin.entries) {
                    val unlocked = save.data.unlockedBowSkins.contains(bs.name); val eq = save.getSelectedBowSkin() == bs
                    addRow(list, bs.displayName, "Bow Visual Style", if (eq) "EQUIPPED" else if (unlocked) "EQUIP" else formatCost(bs.costCoins, bs.costGems), eq) {
                        if (upgrades.buyOrEquipBowSkin(bs)) buildUI() else game.platformServices.showToast("Not enough currency!")
                    }
                }
            }
            Tab.TRAILS -> for (tr in ArrowTrail.entries) {
                val unlocked = save.data.unlockedTrails.contains(tr.name); val eq = save.getSelectedTrail() == tr
                addRow(list, tr.displayName, "Arrow Flight Trail", if (eq) "EQUIPPED" else if (unlocked) "EQUIP" else formatCost(tr.costCoins, tr.costGems), eq) {
                    if (upgrades.buyOrEquipTrail(tr)) buildUI() else game.platformServices.showToast("Not enough currency!")
                }
            }
            Tab.ALLIES -> for (ally in AllyType.entries) {
                val unlocked = upgrades.isAllyUnlocked(ally); val active = upgrades.isAllyActive(ally)
                addRow(list, ally.displayName, ally.description, if (active) "ACTIVE (TAP TO BENCH)" else if (unlocked) "DEPLOY" else formatCost(ally.costCoins, ally.costGems), active) {
                    if (upgrades.buyOrToggleAlly(ally)) buildUI() else game.platformServices.showToast("Not enough currency!")
                }
            }
            Tab.GEMS -> {
                addRow(list, "Free Rewarded Gems", "Watch a short video ad for +5 Gems", "WATCH AD", false) {
                    game.platformServices.showRewardedAd("shop_gems", { save.data.gems += 5; save.save(); buildUI() }, { msg -> game.platformServices.showToast(msg) })
                }
                addRow(list, "Pouch of 50 Gems", "Google Play In-App Purchase", "$0.99", false) {
                    game.platformServices.purchaseProduct("gems_pack_50", { save.data.gems += 50; save.save(); buildUI() }, { msg -> game.platformServices.showToast(msg) })
                }
                addRow(list, "Chest of 250 Gems", "Google Play In-App Purchase", "$3.99", false) {
                    game.platformServices.purchaseProduct("gems_pack_250", { save.data.gems += 250; save.save(); buildUI() }, { msg -> game.platformServices.showToast(msg) })
                }
            }
        }
        root.add(ScrollPane(list, skin)).grow()
        stage.addActor(root)
    }

    private fun addRow(list: Table, title: String, subtitle: String, btnText: String, highlight: Boolean, onClick: () -> Unit) {
        val row = Table(); val info = Table()
        info.add(Label(title, skin)).left().row()
        info.add(Label(subtitle, skin, "small")).left()
        row.add(info).left().expandX().padRight(16f)
        row.add(TextButton(btnText, skin, if (highlight) "accent" else "default").apply { addListener(click(onClick)) }).width(220f).height(52f)
        list.add(row).growX().pad(6f).row()
    }

    private fun formatCost(coins: Int, gems: Int) = when {
        coins > 0 && gems > 0 -> "${coins}c + ${gems}g"; gems > 0 -> "$gems GEMS"; else -> "$coins COINS"
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
