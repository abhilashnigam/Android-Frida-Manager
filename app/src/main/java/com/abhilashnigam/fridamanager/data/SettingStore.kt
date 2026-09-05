package com.abhilashnigam.fridamanager.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

    private fun isValidIpv4(address: String): Boolean {
        val ipv4Regex = Regex(
            """^(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}$"""
        )

        return ipv4Regex.matches(address)
    }
}