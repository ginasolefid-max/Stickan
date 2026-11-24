package com.ironbow.stickmanarcher.platform

import com.ironbow.stickmanarcher.data.OrientationMode

interface PlatformServices {
    fun setOrientation(mode: OrientationMode)
    fun vibrate(milliseconds: Long)
    fun signInPlayGames(onResult: (Boolean) -> Unit)
    fun submitLeaderboardScore(leaderboardId: String, score: Long)
    fun showLeaderboard(leaderboardId: String, onUnavailable: () -> Unit)
    fun showRewardedAd(placement: String, onRewardEarned: () -> Unit, onAdUnavailable: (String) -> Unit)
    fun purchaseProduct(productId: String, onSuccess: (String) -> Unit, onFailure: (String) -> Unit)
    fun showToast(message: String)
}

class DesktopPlatformServices : PlatformServices {
    override fun setOrientation(mode: OrientationMode) {}
    override fun vibrate(milliseconds: Long) {}
    override fun signInPlayGames(onResult: (Boolean) -> Unit) = onResult(false)
    override fun submitLeaderboardScore(leaderboardId: String, score: Long) {}
    override fun showLeaderboard(leaderboardId: String, onUnavailable: () -> Unit) = onUnavailable()
    override fun showRewardedAd(placement: String, onRewardEarned: () -> Unit, onAdUnavailable: (String) -> Unit) =
        onAdUnavailable("Rewarded ads require Android device.")
    override fun purchaseProduct(productId: String, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) =
        onFailure("In-app purchases require Google Play.")
    override fun showToast(message: String) {}
}
