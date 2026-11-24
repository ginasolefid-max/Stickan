package com.ironbow.stickmanarcher.android

import android.os.Bundle
import android.view.WindowManager
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.ironbow.stickmanarcher.StickmanArcherGame

/**
 * Primary Android Activity entry point for Stickman Archer: Zombie Siege.
 */
class AndroidLauncher : AndroidApplication() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useGyroscope = false
            useImmersiveMode = true
            numSamples = 2
            r = 8
            g = 8
            b = 8
            a = 8
        }

        initialize(StickmanArcherGame(AndroidPlatformServices(this)), config)
    }
}
