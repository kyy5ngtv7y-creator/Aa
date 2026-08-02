package dev.datlag.mimasu

import android.content.Context
import android.os.Build
import androidx.multidex.MultiDexApplication
import co.touchlab.kermit.ExperimentalKermitApi
import co.touchlab.kermit.Logger
import co.touchlab.kermit.crashlytics.CrashlyticsLogWriter
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import com.appmattus.certificatetransparency.BasicAndroidCTLogger
import com.appmattus.certificatetransparency.cache.AndroidDiskCache
import com.appmattus.certificatetransparency.installCertificateTransparencyProvider
import com.google.android.gms.net.CronetProviderInstaller
import dev.datlag.mimasu.core.Virtual
import dev.datlag.mimasu.firebase.config.FirebaseRemoteConfigService
import dev.datlag.mimasu.module.NetworkModule
import dev.datlag.mimasu.ui.AppInitializer
import dev.datlag.mimasu.ui.other.Network
import dev.datlag.tooling.Platform
import dev.datlag.tooling.async.VirtualIO
import dev.datlag.tooling.async.ioDispatcher
import dev.datlag.tooling.scopeCatching
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import org.kodein.di.instanceOrNull

class App : MultiDexApplication(), DIAware {

    private val applicationScope = CoroutineScope(ioDispatcher() + SupervisorJob())
    private val appContext: Context
        get() = scopeCatching {
            applicationContext
        }.getOrNull() ?: scopeCatching {
            baseContext
        }.getOrNull() ?: this

    override val di: DI = DI {
        bindSingleton<Context> {
            appContext
        }
        bindSingleton<FirebaseRemoteConfigService> {
            FirebaseRemoteConfigService(
                isDebug = BuildConfig.DEBUG
            )
        }

        import(NetworkModule.di)
    }

    @OptIn(DelicateCoilApi::class, ExperimentalKermitApi::class)
    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA && !Platform.isTelevision(this)) {
            installCertificateTransparencyProvider {
                logger = BasicAndroidCTLogger(BuildConfig.DEBUG)
                diskCache = AndroidDiskCache(appContext)
            }
        }

        val imageLoader by di.instanceOrNull<ImageLoader>()
        imageLoader?.let(SingletonImageLoader::setUnsafe)

        CronetProviderInstaller.installProvider(this)

        // A TMDB key baked into the build lets the app show content without a
        // Firebase project. When present, use it directly and treat Firebase
        // (auth, firestore, remote config) as an optional best-effort extra.
        val usedStaticKey = Network.applyStaticKey(BuildKonfig.tmdbApiKey)

        val firebaseReady = if (AppInitializer.isSekretLoaded(appContext)) {
            val appId = Sekret.firebaseAppId(BuildKonfig.packageName)
            val apiKey = Sekret.firebaseApiKey(BuildKonfig.packageName)

            if (appId.isNullOrBlank() || apiKey.isNullOrBlank()) {
                false
            } else {
                scopeCatching {
                    Firebase.initialize(
                        context = this,
                        options = FirebaseOptions(
                            projectId = Sekret.projectId(BuildKonfig.packageName),
                            applicationId = appId,
                            apiKey = apiKey
                        )
                    )
                }.isSuccess
            }
        } else {
            false
        }

        if (firebaseReady && !BuildConfig.DEBUG) {
            Logger.setLogWriters(CrashlyticsLogWriter())
        }

        when {
            // With a baked-in key the config is already Success and must stay
            // that way. Fetching remote config would move it back through
            // Fetching/Failure, and Network.tmdbApiKey throws in those states
            // (NetworkModule reads it when building the TMDB client), which
            // crashes the app. So skip the fetch entirely.
            usedStaticKey -> Unit
            firebaseReady -> {
                val config by di.instance<FirebaseRemoteConfigService>()
                applicationScope.launch(Dispatchers.VirtualIO) {
                    Network.fetchConfig(config)
                }
            }
            else -> {
                Network.initializeFailure()
                return
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()

        applicationScope.cancel()
    }

}