package com.abhilashnigam.fridamanager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.abhilashnigam.fridamanager.data.SettingsStore
import com.abhilashnigam.fridamanager.data.ThemeMode
import com.abhilashnigam.fridamanager.repository.FridaRepository
import com.abhilashnigam.fridamanager.root.RootManager
import com.abhilashnigam.fridamanager.root.RootManager.RootState
import com.abhilashnigam.fridamanager.service.FridaMonitorService
import com.abhilashnigam.fridamanager.ui.theme.AppScreen
import com.abhilashnigam.fridamanager.ui.theme.FridaManagerTheme
import com.abhilashnigam.fridamanager.ui.theme.MainScreen
import com.abhilashnigam.fridamanager.ui.theme.SettingsScreen
import com.abhilashnigam.fridamanager.ui.theme.VersionScreen
import com.abhilashnigam.fridamanager.widget.FridaWidgetProvider
import androidx.activity.compose.BackHandler

class MainActivity : ComponentActivity() {

    private val repository by lazy {
        FridaRepository(applicationContext)
    }

    private val settings by lazy {
        SettingsStore(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            /*
             * ---------------------------------------------------------
             * THEME
             * ---------------------------------------------------------
             *
             * Observe the user's saved theme preference.
             *
             * Because this is a Flow from DataStore, changing the
             * setting will automatically cause Compose to recompose.
             */
            val themeMode by settings.themeMode.collectAsState(
                initial = ThemeMode.SYSTEM
            )

            /*
             * Convert ThemeMode into the Boolean expected by
             * FridaManagerTheme.
             */
            val systemDarkTheme = isSystemInDarkTheme()

            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> systemDarkTheme
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            FridaManagerTheme(
                darkTheme = darkTheme
            ) {

                /*
                 * -----------------------------------------------------
                 * ROOT STATE
                 * -----------------------------------------------------
                 */
                var rootState by remember {
                    mutableStateOf<RootState?>(null)
                }

                /*
                 * Request root when the application starts.
                 */
                LaunchedEffect(Unit) {
                    rootState = RootManager.requestRoot()

                    if (rootState == RootState.Granted) {
                        startForegroundService(
                            Intent(
                                this@MainActivity,
                                FridaMonitorService::class.java
                            )
                        )

                        repository.refresh()
                        FridaWidgetProvider.pushStatusUpdate(
                            context = applicationContext,
                            running = repository.state.value.fridaRunning
                        )
                    }
                }

                /*
                 * -----------------------------------------------------
                 * APPLICATION NAVIGATION
                 * -----------------------------------------------------
                 */
                when (rootState) {

                    /*
                     * Root check is still running.
                     */
                    null -> {
                        LoadingScreen()
                    }

                    /*
                     * Root was denied.
                     */
                    RootState.Denied -> {
                        RootDeniedScreen(
                            onExit = { finish() }
                        )
                    }

                    /*
                     * Root granted.
                     */
                    RootState.Granted -> {

                        var screen by remember {
                            mutableStateOf(AppScreen.MAIN)
                        }
                        BackHandler(
                            enabled = screen != AppScreen.MAIN
                        ) {
                            screen = AppScreen.MAIN
                        }

                        when (screen) {

                            AppScreen.MAIN -> {
                                MainScreen(
                                    repository = repository,
                                    onManageVersions = {
                                        screen = AppScreen.VERSIONS
                                    },
                                    onOpenSettings = {
                                        screen = AppScreen.SETTINGS
                                    }
                                )
                            }

                            AppScreen.VERSIONS -> {
                                VersionScreen(
                                    repository = repository,
                                    onDone = {
                                        screen = AppScreen.MAIN
                                    }
                                )
                            }

                            AppScreen.SETTINGS -> {
                                SettingsScreen(
                                    repository = repository,
                                    onDone = {
                                        screen = AppScreen.MAIN
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


/**
 * Initial loading screen while root access is being checked.
 */
@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}


/**
 * Displayed when root access is unavailable.
 */
@Composable
private fun RootDeniedScreen(
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onExit,

        confirmButton = {
            TextButton(
                onClick = onExit
            ) {
                Text("Exit")
            }
        },

        title = {
            Text("Root access required")
        },

        text = {
            Text(
                "Frida Manager needs root access to manage " +
                        "frida-server. Grant root access and reopen the app."
            )
        }
    )
}