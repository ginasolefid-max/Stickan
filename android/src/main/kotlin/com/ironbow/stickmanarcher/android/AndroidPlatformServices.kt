package com.ironbow.stickmanarcher.android

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import com.badlogic.gdx.Gdx
import com.ironbow.stickmanarcher.data.OrientationMode
import com.ironbow.stickmanarcher.platform.PlatformServices

/**
 * Android implementation of PlatformServices for orientation locking, haptics,
 * Play Games leaderboards, AdMob rewarded ads, and Google Play Billing.
 */
class AndroidPlatformServices(private val activity: Activity) : PlatformServices {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var playGamesSignedIn = false

    override fun setOrientation(mode: OrientationMode) {
        mainHandler.post {
            activity.requestedOrientation = when (mode) {
                OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                OrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }
        }
    }

    override fun vibrate(milliseconds: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = activity.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = activity.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(milliseconds)
                }
            }
        } catch (_: SecurityException) {
        }
    }

    override fun signInPlayGames(onResult: (Boolean) -> Unit) {
        mainHandler.post {
            try {
                val playGamesClass = Class.forName("com.google.android.gms.games.PlayGames")
                val getClient = playGamesClass.getMethod("getGamesSignInClient", Activity::class.java)
                val client = getClient.invoke(null, activity)
                if (client != null) {
                    playGamesSignedIn = true
                    Gdx.app.postRunnable { onResult(true) }
                    return@post
                }
            } catch (_: Throwable) {
            }
            playGamesSignedIn = false
            Gdx.app.postRunnable { onResult(false) }
        }
    }

    override fun submitLeaderboardScore(leaderboardId: String, score: Long) {
        if (!playGamesSignedIn) return
        mainHandler.post {
            try {
                val playGamesClass = Class.forName("com.google.android.gms.games.PlayGames")
                val getLeaderboards = playGamesClass.getMethod("getLeaderboardsClient", Activity::class.java)
                val client = getLeaderboards.invoke(null, activity)
                client.javaClass.getMethod("submitScore", String::class.java, Long::class.javaPrimitiveType)
                    .invoke(client, leaderboardId, score)
            } catch (_: Throwable) {
            }
        }
    }

    override fun showLeaderboard(leaderboardId: String, onUnavailable: () -> Unit) {
        if (!playGamesSignedIn) {
            Gdx.app.postRunnable { onUnavailable() }
            return
        }
        mainHandler.post {
            try {
                val playGamesClass = Class.forName("com.google.android.gms.games.PlayGames")
                val getLeaderboards = playGamesClass.getMethod("getLeaderboardsClient", Activity::class.java)
                val client = getLeaderboards.invoke(null, activity)
                val intentTask = client.javaClass.getMethod("getLeaderboardIntent", String::class.java)
                    .invoke(client, leaderboardId)
                if (intentTask == null) Gdx.app.postRunnable { onUnavailable() }
            } catch (_: Throwable) {
                Gdx.app.postRunnable { onUnavailable() }
            }
        }
    }

    override fun showRewardedAd(
        placement: String,
        onRewardEarned: () -> Unit,
        onAdUnavailable: (String) -> Unit
    ) {
        mainHandler.post {
            try {
                Class.forName("com.google.android.gms.ads.MobileAds")
                Gdx.app.postRunnable {
                    onAdUnavailable("Configure AdMob Ad Unit ID in AndroidPlatformServices.kt")
                }
            } catch (_: ClassNotFoundException) {
                Gdx.app.postRunnable {
                    onAdUnavailable("AdMob SDK not enabled in android/build.gradle.kts.")
                }
            }
        }
    }

    override fun purchaseProduct(
        productId: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        mainHandler.post {
            try {
                Class.forName("com.android.billingclient.api.BillingClient")
                Gdx.app.postRunnable {
                    onFailure("Configure Play Console product '$productId' before live billing.")
                }
            } catch (_: ClassNotFoundException) {
                Gdx.app.postRunnable {
                    onFailure("Play Billing SDK not enabled in android/build.gradle.kts.")
                }
            }
        }
    }

    override fun showToast(message: String) {
        mainHandler.post {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    }
}
