package dev.datlag.mimasu

import android.app.PictureInPictureUiState
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import co.touchlab.kermit.Logger
import dev.datlag.kast.Kast
import dev.datlag.kast.UnselectReason
import dev.datlag.mimasu.common.isInPiPMode
import dev.datlag.mimasu.common.toExpressiveTypography
import dev.datlag.mimasu.firebase.auth.FirebaseAuthService
import dev.datlag.mimasu.other.PiPHelper
import dev.datlag.mimasu.ui.other.Network
import dev.datlag.mimasu.ui.theme.Font
import dev.datlag.mimasu.ui.viewmodel.LoginViewModel
import dev.datlag.tooling.Platform
import dev.datlag.tooling.async.launchIO
import dev.datlag.tooling.safeCast
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instanceOrNull
import kotlin.reflect.safeCast

class MainActivity : MimasuActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        fun exit(reason: String?) {
            reason?.let { Logger.e(messageString = it) }
            finishAffinity()
        }

        super.onCreate(savedInstanceState)

        // If the previous run died, show the trace instead of starting the app
        // again - otherwise the same crash would repeat before it can be read.
        CrashReporter.consume(this)?.let { report ->
            showCrashReport(report)
            return
        }

        if (Platform.isTelevision(this)) {
            val intent = Intent(this, TVActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            installSplashScreen().apply {
                setKeepOnScreenCondition {
                    Network.showSplashscreen
                }
            }
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        val di = this.di() ?: return exit("Could not find dependency injection.")
        bindExtension { !Platform.isTelevision(this) }
        PiPHelper.setActive(this.isInPiPMode())
        Kast.setup(this)

        setContent {
            App(
                di = di,
                typography = Font.manrope.toExpressiveTypography(),
            )
        }

        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()

        registerExtension { !Platform.isTelevision(this) }
        bindExtension { !Platform.isTelevision(this) }
        PiPHelper.setActive(this.isInPiPMode())
    }

    override fun onResume() {
        super.onResume()

        bindExtension { !Platform.isTelevision(this) }
        PiPHelper.setActive(this.isInPiPMode())
    }

    override fun onPause() {
        super.onPause()

        bindExtension { !Platform.isTelevision(this) }
        PiPHelper.setActive(this.isInPiPMode())
    }

    override fun onRestart() {
        super.onRestart()

        bindExtension { !Platform.isTelevision(this) }
        PiPHelper.setActive(this.isInPiPMode())
    }

    override fun onDestroy() {
        super.onDestroy()

        PiPHelper.setActive(this.isInPiPMode())

        Kast.castContext?.sessionManager?.endCurrentSession(true)
        Kast.unselect(UnselectReason.disconnected)
        Kast.dispose()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.action?.ifBlank { null }

        if (Intent.ACTION_VIEW == action) {
            val data = intent.data

            if (data != null) {
                val oobCode = data.getQueryParameter("oobCode")?.ifBlank { null }
                val mode = data.getQueryParameter("mode")?.ifBlank { null }

                if (!oobCode.isNullOrBlank()) {
                    when {
                        mode.equals("resetPassword", ignoreCase = true) -> {
                            LoginViewModel.setResetCode(oobCode)
                        }
                        mode.equals("verify", ignoreCase = true) || mode.equals("verifyEmail", ignoreCase = true) -> {
                            val authService = di()?.let {
                                val instance by it.instanceOrNull<FirebaseAuthService>()
                                instance
                            }

                            lifecycleScope.launchIO {
                                authService?.verifyEmail(oobCode)
                                authService?.currentUser?.reload()
                            }
                        }
                    }
                }
            }
        }
        setIntent(Intent())
    }

    override fun onPictureInPictureUiStateChanged(pipState: PictureInPictureUiState) {
        super.onPictureInPictureUiStateChanged(pipState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            PiPHelper.setActive(pipState.isTransitioningToPip || this.isInPiPMode())
        }
    }

    /**
     * Renders a stored crash trace with plain views - no Compose, no theming,
     * no dependency injection - so displaying it cannot fail for the same
     * reason the app just did.
     */
    private fun showCrashReport(report: String) {
        val padding = (16 * resources.displayMetrics.density).toInt()

        val title = TextView(this).apply {
            text = "Letzter Absturz"
            textSize = 20f
            setPadding(padding, padding, padding, padding / 2)
        }
        val copyButton = Button(this).apply {
            text = "Fehlertext kopieren"
            setOnClickListener {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("Mimasu crash", report))
                Toast.makeText(this@MainActivity, "Kopiert", Toast.LENGTH_SHORT).show()
            }
        }
        val trace = TextView(this).apply {
            text = report
            textSize = 11f
            setTextIsSelectable(true)
            setPadding(padding, padding / 2, padding, padding)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(title)
            addView(copyButton)
            addView(
                ScrollView(this@MainActivity).apply { addView(trace) },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0
                ).apply { weight = 1f }
            )
        }

        setContentView(root)
    }
}