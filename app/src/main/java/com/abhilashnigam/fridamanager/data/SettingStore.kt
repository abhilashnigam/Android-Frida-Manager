package com.abhilashnigam.fridamanager.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.SecureRandom

private val Context.dataStore by preferencesDataStore(
    name = "frida_manager_settings"
)

private val KEY_CHECK_FOR_UPDATES =
    booleanPreferencesKey("check_for_updates")

private val KEY_THEME_MODE =
    stringPreferencesKey("theme_mode")

private val KEY_BIND_ADDRESS =
    stringPreferencesKey("bind_address")

private val KEY_FRIDA_PORT =
    intPreferencesKey("frida_port")

private val KEY_ANONYMIZER_ENABLED =
    booleanPreferencesKey("anonymizer_enabled")

private val KEY_ANONYMIZER_DIRECTORY =
    stringPreferencesKey("anonymizer_directory")

private val KEY_ANONYMIZER_BINARY =
    stringPreferencesKey("anonymizer_binary")

private const val FRIDA_TMP_DIR = "/data/local/tmp"
private const val DEFAULT_FRIDA_BINARY = "$FRIDA_TMP_DIR/frida-server"
private const val RANDOM_NAME_LENGTH = 16
private const val ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

data class FridaBinaryLocation(
    val anonymized: Boolean,
    val directoryName: String?,
    val binaryName: String?
) {
    val binaryPath: String
        get() = if (anonymized && directoryName != null && binaryName != null) {
            "$FRIDA_TMP_DIR/$directoryName/$binaryName"
        } else {
            DEFAULT_FRIDA_BINARY
        }
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

class SettingsStore(
    private val context: Context
) {

    /**
     * Defaults to true for a fresh install.
     * User can disable this via the Settings screen.
     */
    val checkForUpdates: Flow<Boolean> =
        context.dataStore.data
            .map { prefs ->
                prefs[KEY_CHECK_FOR_UPDATES] ?: true
            }

    /**
     * Controls the application's appearance.
     *
     * Defaults to SYSTEM so the application follows
     * the Android device's current theme.
     */
    val themeMode: Flow<ThemeMode> =
        context.dataStore.data
            .map { prefs ->
                when (prefs[KEY_THEME_MODE]) {
                    ThemeMode.LIGHT.name -> ThemeMode.LIGHT
                    ThemeMode.DARK.name -> ThemeMode.DARK
                    else -> ThemeMode.SYSTEM
                }
            }

    /**
     * Frida server bind address.
     *
     * Defaults to localhost so Frida is not exposed
     * to other devices on the network.
     */
    val bindAddress: Flow<String> =
        context.dataStore.data
            .map { prefs ->
                prefs[KEY_BIND_ADDRESS] ?: "127.0.0.1"
            }

    /**
     * Frida server listening port.
     *
     * Defaults to Frida's standard port.
     */
    val fridaPort: Flow<Int> =
        context.dataStore.data
            .map { prefs ->
                prefs[KEY_FRIDA_PORT] ?: 27042
            }

    /** The persisted path of the managed Frida binary. */
    val fridaBinaryLocation: Flow<FridaBinaryLocation> =
        context.dataStore.data.map { prefs ->
            val enabled = prefs[KEY_ANONYMIZER_ENABLED] ?: false
            val directory = prefs[KEY_ANONYMIZER_DIRECTORY]
            val binary = prefs[KEY_ANONYMIZER_BINARY]
            FridaBinaryLocation(
                anonymized = enabled && isValidRandomName(directory) && isValidRandomName(binary),
                directoryName = directory,
                binaryName = binary
            )
        }

    /**
     * Enables or disables checking for newer Frida releases.
     */
    suspend fun setCheckForUpdates(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CHECK_FOR_UPDATES] = enabled
        }
    }

    /**
     * Saves the user's selected application theme.
     */
    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    /**
     * Saves the Frida server bind address.
     *
     * Only valid IPv4 addresses are accepted.
     */
    suspend fun setBindAddress(address: String) {
        require(isValidIpv4(address)) {
            "Invalid IPv4 address"
        }

        context.dataStore.edit { prefs ->
            prefs[KEY_BIND_ADDRESS] = address
        }
    }

    /**
     * Saves the Frida server port.
     *
     * Valid ports are 1000 through 65535.
     */
    suspend fun setFridaPort(port: Int) {
        require(port in 1000..65535) {
            "Frida port must be between 1000 and 65535"
        }

        context.dataStore.edit { prefs ->
            prefs[KEY_FRIDA_PORT] = port
        }
    }

    /**
     * Enables or disables use of an opaque, app-managed binary path. Names are
     * created once with SecureRandom and retained so the install location survives
     * process death and device restarts.
     */
    suspend fun setAnonymizerEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            if (enabled) {
                if (!isValidRandomName(prefs[KEY_ANONYMIZER_DIRECTORY])) {
                    prefs[KEY_ANONYMIZER_DIRECTORY] = generateRandomName()
                }
                if (!isValidRandomName(prefs[KEY_ANONYMIZER_BINARY])) {
                    prefs[KEY_ANONYMIZER_BINARY] = generateRandomName()
                }
            }
            prefs[KEY_ANONYMIZER_ENABLED] = enabled
        }
    }

    /** Ensures persisted anonymizer names exist without switching the active path. */
    suspend fun ensureAnonymizerLocation(): FridaBinaryLocation {
        context.dataStore.edit { prefs ->
            if (!isValidRandomName(prefs[KEY_ANONYMIZER_DIRECTORY])) {
                prefs[KEY_ANONYMIZER_DIRECTORY] = generateRandomName()
            }
            if (!isValidRandomName(prefs[KEY_ANONYMIZER_BINARY])) {
                prefs[KEY_ANONYMIZER_BINARY] = generateRandomName()
            }
        }
        return fridaBinaryLocation.first()
    }

    private fun isValidIpv4(address: String): Boolean {
        val ipv4Regex = Regex(
            """^(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}$"""
        )

        return ipv4Regex.matches(address)
    }

    private fun generateRandomName(): String = buildString(RANDOM_NAME_LENGTH) {
        repeat(RANDOM_NAME_LENGTH) {
            append(ALPHANUMERIC[SecureRandomHolder.instance.nextInt(ALPHANUMERIC.length)])
        }
    }

    private fun isValidRandomName(value: String?): Boolean =
        value?.length == RANDOM_NAME_LENGTH && value.all { it in ALPHANUMERIC }

    private object SecureRandomHolder {
        val instance = SecureRandom()
    }
}
