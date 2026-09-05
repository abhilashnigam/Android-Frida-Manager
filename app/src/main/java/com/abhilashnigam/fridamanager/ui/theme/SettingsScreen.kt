package com.abhilashnigam.fridamanager.ui.theme

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abhilashnigam.fridamanager.data.SettingsStore
import com.abhilashnigam.fridamanager.data.ThemeMode
import com.abhilashnigam.fridamanager.repository.FridaRepository
import kotlinx.coroutines.launch
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.abhilashnigam.fridamanager.network.BindAddressOption
import com.abhilashnigam.fridamanager.network.NetworkInterfaceManager

@Composable
fun SettingsScreen(
    repository: FridaRepository,
    onDone: () -> Unit
) {
    val state by repository.state.collectAsState()

    val settings = SettingsStore(
        context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    )

    val themeMode by settings.themeMode.collectAsState(
        initial = ThemeMode.SYSTEM
    )

    val bindAddress by settings.bindAddress.collectAsState(
        initial = "127.0.0.1"
    )

    val fridaPort by settings.fridaPort.collectAsState(
        initial = 27042
    )

    val networkAddresses = remember {
        NetworkInterfaceManager.getIpv4Addresses()
    }

    val bindAddressOptions = remember(networkAddresses) {
        buildList {
            add(
                BindAddressOption(
                    address = "127.0.0.1",
                    label = "Local device only"
                )
            )

            networkAddresses.forEach { networkAddress ->
                add(
                    BindAddressOption(
                        address = networkAddress.address,
                        label = networkAddress.interfaceName
                    )
                )
            }

            add(
                BindAddressOption(
                    address = "0.0.0.0",
                    label = "All interfaces"
                )
            )
        }.distinctBy { it.address }
    }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Settings",
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Frida Manager configuration",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    TextButton(
                        onClick = onDone
                    ) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            /*
             * ---------------------------------------------------------
             * UPDATES
             * ---------------------------------------------------------
             */

            SettingsSectionTitle(
                title = "UPDATES"
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "Check for updates",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = "Check GitHub for newer Frida server releases.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = state.checkForUpdatesEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    repository.setCheckForUpdates(enabled)
                                }
                            }
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Text(
                        text = if (state.checkForUpdatesEnabled) {
                            "A newer release will be shown on the main screen when one is available."
                        } else {
                            "Update notifications are disabled. The installed version will not be flagged as outdated."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            /*
             * ---------------------------------------------------------
             * APPEARANCE
             * ---------------------------------------------------------
             */

            SettingsSectionTitle(
                title = "APPEARANCE"
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(
                            horizontal = 20.dp,
                            vertical = 12.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Theme",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Choose how Frida Manager looks.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    ThemeOption(
                        title = "System default",
                        description = "Follow the Android system theme.",
                        selected = themeMode == ThemeMode.SYSTEM,
                        onClick = {
                            scope.launch {
                                settings.setThemeMode(ThemeMode.SYSTEM)
                            }
                        }
                    )

                    ThemeOption(
                        title = "Light",
                        description = "Always use the light theme.",
                        selected = themeMode == ThemeMode.LIGHT,
                        onClick = {
                            scope.launch {
                                settings.setThemeMode(ThemeMode.LIGHT)
                            }
                        }
                    )

                    ThemeOption(
                        title = "Dark",
                        description = "Always use the dark theme.",
                        selected = themeMode == ThemeMode.DARK,
                        onClick = {
                            scope.launch {
                                settings.setThemeMode(ThemeMode.DARK)
                            }
                        }
                    )
                }
            }

            /*
 * ---------------------------------------------------------
 * FRIDA SERVER
 * ---------------------------------------------------------
 */

            SettingsSectionTitle(
                title = "FRIDA SERVER"
            )

            FridaServerSettings(
                bindAddress = bindAddress,
                port = fridaPort,
                options = bindAddressOptions,
                onBindAddressChanged = { address ->
                    scope.launch {
                        settings.setBindAddress(address)
                    }
                },
                onPortChanged = { port ->
                    scope.launch {
                        settings.setFridaPort(port)
                    }
                }
            )

            /*
             * ---------------------------------------------------------
             * VERSION MANAGEMENT
             * ---------------------------------------------------------
             */

            SettingsSectionTitle(
                title = "VERSION MANAGEMENT"
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Text(
                        text = "Pinned versions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "You can intentionally use an older Frida server version. Disabling update checks prevents the application from reporting that version as outdated.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            /*
             * ---------------------------------------------------------
             * ABOUT
             * ---------------------------------------------------------
             */

            SettingsSectionTitle(
                title = "ABOUT"
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {

                    Text(
                        text = "Frida Manager",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Manage frida-server installations on rooted Android devices.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


/**
 * A single theme selection row.
 */
@Composable
private fun ThemeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                ),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {

            RadioButton(
                selected = selected,
                onClick = onClick
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = 8.dp,
                        end = 8.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {

                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


/**
 * Small section heading used throughout Settings.
 */
@Composable
private fun SettingsSectionTitle(
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun FridaServerSettings(
    bindAddress: String,
    port: Int,
    options: List<BindAddressOption>,
    onBindAddressChanged: (String) -> Unit,
    onPortChanged: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var portText by remember(port) {
        mutableStateOf(port.toString())
    }

    val selectedOption = options.firstOrNull {
        it.address == bindAddress
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Bind address",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Choose the network address where frida-server will listen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = {
                    expanded = !expanded
                }
            ) {
                OutlinedTextField(
                    value = selectedOption?.address ?: bindAddress,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    label = {
                        Text("IP address")
                    },
                    supportingText = {
                        Text(
                            selectedOption?.label
                                ?: "Custom address"
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = expanded
                        )
                    }
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                    }
                ) {
                    options.forEach { option ->

                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = option.address,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onBindAddressChanged(option.address)
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = portText,
                onValueChange = { value ->
                    if (value.all { it.isDigit() } && value.length <= 5) {
                        portText = value

                        val portValue = value.toIntOrNull()

                        if (portValue != null &&
                            portValue in 1000..65535
                        ) {
                            onPortChanged(portValue)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Port")
                },
                singleLine = true,
                supportingText = {
                    Text("Allowed range: 1000–65535")
                },
                isError = portText.toIntOrNull()?.let {
                    it !in 1000..65535
                } ?: true
            )

            if (bindAddress == "0.0.0.0") {
                Text(
                    text = "Warning: Frida will listen on all IPv4 interfaces and may be reachable from other devices on the network.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}