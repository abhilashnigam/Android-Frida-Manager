package com.abhilashnigam.fridamanager.ui.theme

import com.abhilashnigam.fridamanager.repository.AppLoadState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abhilashnigam.fridamanager.data.SettingsStore
import com.abhilashnigam.fridamanager.repository.FridaRepository
import com.abhilashnigam.fridamanager.widget.FridaWidgetProvider
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    repository: FridaRepository,
    onManageVersions: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val state by repository.state.collectAsState()
    val scope = rememberCoroutineScope()

    var busy by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Frida Manager",
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "By oathk33p3r",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = onOpenSettings
                    ) {
                        Text("Settings")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            /*
             * ---------------------------------------------------------
             * INITIALIZATION
             * ---------------------------------------------------------
             */
            if (state.loadState == AppLoadState.LOADING) {
                LoadingFridaState()
                return@Column
            }

            /*
             * ---------------------------------------------------------
             * FRIDA NOT INSTALLED
             * ---------------------------------------------------------
             */
            if (!state.fridaInstalled) {

                EmptyFridaState(
                    onManageVersions = onManageVersions
                )

                return@Column
            }

            /*
             * ---------------------------------------------------------
             * SERVER STATUS
             * ---------------------------------------------------------
             */
            val context = LocalContext.current
            val settings = remember {
                SettingsStore(context.applicationContext)
            }

            val bindAddress by settings.bindAddress.collectAsState(
                initial = "127.0.0.1"
            )

            val port by settings.fridaPort.collectAsState(
                initial = 27042
            )
            ServerStatusCard(
                running = state.fridaRunning,
                version = state.installedVersion,
                bindAddress = bindAddress,
                port = port,
                busy = busy,
                onToggle = {
                    busy = true

                    scope.launch {
                        repository.toggleServer()
                        FridaWidgetProvider.pushStatusUpdate(
                            context = context.applicationContext,
                            running = repository.state.value.fridaRunning
                        )
                        busy = false
                    }
                }
            )

            /*
             * ---------------------------------------------------------
             * UPDATE
             * ---------------------------------------------------------
             */
            if (
                state.checkForUpdatesEnabled &&
                state.updateAvailable
            ) {
                UpdateCard(
                    installedVersion = state.installedVersion,
                    latestVersion = state.latestKnownVersion,
                    onManageVersions = onManageVersions
                )
            }

            /*
             * ---------------------------------------------------------
             * VERSION MANAGEMENT
             * ---------------------------------------------------------
             */
            VersionManagementCard(
                onManageVersions = onManageVersions
            )
        }
    }
}


/**
 * Main Frida server status card.
 */
@Composable
private fun ServerStatusCard(
    running: Boolean,
    version: String,
    bindAddress: String,
    port: Int,
    busy: Boolean,
    onToggle: () -> Unit
) {
    val statusText = if (running) {
        "SERVER RUNNING"
    } else {
        "SERVER STOPPED"
    }

    val endpointText = if (running) {
        "Listening on $bindAddress:$port"
    } else {
        "Configured for $bindAddress:$port"
    }

    val statusColor = if (running) {
        Color(0xFF2E7D32)
    } else {
        MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            /*
             * Status indicator
             */
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .size(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(statusColor)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    Text(
                        text = endpointText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor

                    )
                }
            }

            /*
             * Frida version
             */
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Frida Server",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = version.ifBlank { "Unknown version" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

            /*
             * Start / Stop button
             */
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !busy,
                onClick = onToggle,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = when {
                        busy -> "Please wait..."
                        running -> "STOP SERVER"
                        else -> "START SERVER"
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Displays when Frida is loading.
 */
@Composable
private fun LoadingFridaState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )

            Text(
                text = "Initializing",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Checking Frida server...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


/**
 * Displays when Frida is not installed.
 */
@Composable
private fun EmptyFridaState(
    onManageVersions: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = "Frida Server",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "No Frida server is installed on this device.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Choose a Frida version to download and install.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onManageVersions,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("CHOOSE VERSION")
            }
        }
    }
}


/**
 * Displays when a newer Frida version is available.
 */
@Composable
private fun UpdateCard(
    installedVersion: String,
    latestVersion: String?,
    onManageVersions: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "UPDATE AVAILABLE",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Current",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    Text(
                        text = installedVersion.ifBlank { "Unknown" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "→",
                    style = MaterialTheme.typography.titleLarge
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Latest",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    Text(
                        text = latestVersion ?: "Unknown",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            TextButton(
                onClick = onManageVersions,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("VIEW AVAILABLE VERSIONS")
            }
        }
    }
}


/**
 * Version management entry point.
 */
@Composable
private fun VersionManagementCard(
    onManageVersions: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp,
                    vertical = 18.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Manage Versions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Install or switch between Frida releases",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(
                onClick = onManageVersions
            ) {
                Text("OPEN")
            }
        }
    }
}