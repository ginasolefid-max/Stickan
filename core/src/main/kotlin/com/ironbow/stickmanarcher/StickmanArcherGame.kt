package com.ironbow.stickmanarcher

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.ironbow.stickmanarcher.assets.AssetManager
import com.ironbow.stickmanarcher.assets.AudioManager
import com.ironbow.stickmanarcher.data.SaveSystem
import com.ironbow.stickmanarcher.data.UpgradeManager
import com.ironbow.stickmanarcher.platform.PlatformServices
import com.ironbow.stickmanarcher.ui.UIManager

class StickmanArcherGame(val platformServices: PlatformServices) : Game() {

    lateinit var batch: SpriteBatch private set
    lateinit var saveSystem: SaveSystem private set
    lateinit var upgradeManager: UpgradeManager private set
    lateinit var assetManager: AssetManager private set
    lateinit var audioManager: AudioManager private set
    lateinit var uiManager: UIManager private set

    override fun create() {
        batch = SpriteBatch()
        saveSystem = SaveSystem().apply { load() }
        upgradeManager = UpgradeManager(saveSystem)
        assetManager = AssetManager().apply { load() }
        audioManager = AudioManager(saveSystem).apply { init() }
        platformServices.setOrientation(saveSystem.getOrientationMode())
        uiManager = UIManager(this)
        uiManager.showMainMenu()
    }

    override fun pause() { super.pause(); audioManager.pause(); saveSystem.save() }
    override fun resume() { super.resume(); audioManager.resume() }

    override fun dispose() {
        super.dispose(); saveSystem.save(); audioManager.dispose(); assetManager.dispose(); batch.dispose()
    }
}
