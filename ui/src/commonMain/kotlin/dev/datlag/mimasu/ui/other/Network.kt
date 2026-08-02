package dev.datlag.mimasu.ui.other

import dev.datlag.mimasu.core.now
import dev.datlag.mimasu.core.toEpochMilliseconds
import dev.datlag.mimasu.firebase.config.FirebaseRemoteConfigService
import dev.datlag.sekret.Secret
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

data object Network {

    private val _config = MutableStateFlow<Config>(Config.Fetching)
    val config: StateFlow<Config> = _config.asStateFlow()

    private var startedFetching = 0L
    private var staticKey: String? = null

    val showSplashscreen: Boolean
        get() {
            return if (config.value is Config.Fetching) {
                startedFetching == 0L || LocalDateTime.now().toEpochMilliseconds() - startedFetching < 3000L
            } else {
                false
            }
        }

    val tmdbApiKey: String
        get() = config.value.getOrThrow().tmdb

    suspend fun fetchConfig(remoteService: FirebaseRemoteConfigService) {
        if ((config.firstOrNull() ?: config.value) is Config.Failure.Initialize) {
            return
        }
        // A baked-in key already provides a usable config; never downgrade it.
        if (staticKey != null) {
            return
        }
        startedFetching = LocalDateTime.now().toEpochMilliseconds()
        _config.update { Config.Fetching }

        val tmdb = remoteService.getStringResult(Config.TMDB_KEY)
        startedFetching = 0L

        _config.update {
            if (tmdb.isFailure) {
                Config.Failure.Fetching(tmdb.exceptionOrNull())
            } else {

                Config.Success(
                    tmdb = tmdb.getOrNull() ?: return@update Config.Failure.Fetching()
                )
            }
        }
    }

    fun initializeFailure() {
        staticKey?.let {
            _config.update { _ -> Config.Success(tmdb = it) }
            return
        }
        _config.update { Config.Failure.Initialize }
    }

    /**
     * Use a TMDB key that was baked into the build instead of fetching it from
     * Firebase Remote Config. Returns true when a usable key was applied.
     *
     * Once set, the config can no longer fall back to Fetching/Failure:
     * [tmdbApiKey] throws outside of Success, so a downgrade would crash any
     * caller building the TMDB client.
     */
    fun applyStaticKey(key: String?): Boolean {
        val trimmed = key?.trim()?.ifBlank { null } ?: return false
        staticKey = trimmed
        _config.update { Config.Success(tmdb = trimmed) }
        return true
    }

    @Serializable
    sealed interface Config {

        fun getOrThrow(): Success {
            return this as? Success ?: throw AccessException(this)
        }

        @Serializable
        data object Fetching : Config

        @Serializable
        sealed interface Failure : Config {

            @Serializable
            data object Initialize : Failure

            @Serializable
            data class Fetching(
                @Transient val throwable: Throwable? = null
            ) : Failure
        }

        @Serializable
        data class Success(
            @Secret val tmdb: String,
        ) : Config

        class AccessException(state: Config) : Exception("Tried to access config data while in $state state.")

        companion object {
            internal const val TMDB_KEY = "tmdb_api_key"
        }
    }
}