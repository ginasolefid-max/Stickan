package com.ironbow.stickmanarcher.ui

import com.ironbow.stickmanarcher.StickmanArcherGame
import com.ironbow.stickmanarcher.data.GameMode

class UIManager(private val game: StickmanArcherGame) {
    fun showMainMenu() { game.setScreen(MainMenuScreen(game)) }
    fun showLevelSelect() { game.setScreen(LevelSelectScreen(game)) }
    fun showShop() { game.setScreen(ShopScreen(game)) }
    fun showUpgrades() { game.setScreen(UpgradeScreen(game)) }
    fun showSettings() { game.setScreen(SettingsScreen(game)) }
    fun startGameplay(mode: GameMode, levelNumber: Int = 1) { game.setScreen(GameScreen(game, mode, levelNumber)) }
}
