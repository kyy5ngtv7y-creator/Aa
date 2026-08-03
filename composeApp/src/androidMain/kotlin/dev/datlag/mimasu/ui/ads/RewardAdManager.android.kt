package dev.datlag.mimasu.ui.ads

import android.app.Activity
import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

// Ads have been removed from this build. Rewarded content is granted directly.
@Serializable
actual class RewardAdManager(
    @Transient private val activity: Activity? = null
) {
    actual fun showRewardAd(onRewarded: () -> Unit) {
        onRewarded()
    }
}

@Composable
actual fun rememberAdManager(): RewardAdManager {
    return RewardAdManager()
}
