package com.abhilashnigam.fridamanager.ui.theme

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abhilashnigam.fridamanager.model.FridaRelease
import com.abhilashnigam.fridamanager.repository.FridaRepository
import com.abhilashnigam.fridamanager.repository.InstallResult
import com.abhilashnigam.fridamanager.network.ReleaseFetchResult
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog

private sealed class LoadState {
    data object Loading : LoadState()
    data class Loaded(val releases: List<FridaRelease>) : LoadState()
    data object Empty : LoadState()
    data class Error(val message: String) : LoadState()
}

@Composable
fun VersionScreen(
    repository: FridaRepository,
    onDone: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val mainState by repository.state.collectAsState()

    var loadState by remember {
        mutableStateOf<LoadState>(LoadState.Loading)
    }

    var installingTag by remember {
        mutableStateOf<String?>(null)
    }

    var uninstalling by remember {
        mutableStateOf(false)
    }

    var showUninstallDialog by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    fun load() {
        scope.launch {
            loadState = LoadState.Loading
            errorMessage = null

            loadState = when (val result = repository.availableReleases()) {
                is ReleaseFetchResult.Success -> {
                    if (result.releases.isEmpty()) LoadState.Empty
                    else LoadState.Loaded(result.releases)
                }
                is ReleaseFetchResult.HttpError -> LoadState.Error(
                    "GitHub returned HTTP ${result.statusCode}. Please try again later."
                )
                ReleaseFetchResult.NetworkError -> LoadState.Error(
                    "Could not connect to GitHub. Check your network connection and try again."
                )
                ReleaseFetchResult.ParseError -> LoadState.Error(
                    "GitHub returned an unexpected release response. Please try again later."
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Frida Versions",
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Choose a server release",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    TextButton(
                        onClick = onDone,
                        enabled = installingTag == null
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
        ) {

            /*
             * ---------------------------------------------------------
             * CURRENT VERSION
             * ---------------------------------------------------------
             */

            if (mainState.fridaInstalled) {
                CurrentVersionCard(
                    version = mainState.installedVersion
                )
            }

            /*
             * ---------------------------------------------------------
             * ERROR
             * ---------------------------------------------------------
             */

            errorMessage?.let { message ->
                ErrorCard(
                    message = message,
                    onDismiss = {
                        errorMessage = null
                    }
                )
            }

            /*
             * ---------------------------------------------------------
             * CONTENT
             * ---------------------------------------------------------
             */

            when (val state = loadState) {

                is LoadState.Loading -> {
                    LoadingVersions()
                }

                is LoadState.Empty -> {
                    EmptyReleasesState()
                }

                is LoadState.Error -> {
                    ReleaseErrorState(
                        message = state.message,
                        onRetry = ::load
                    )
                }

                is LoadState.Loaded -> {

                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {

                        Text(
                            text = "AVAILABLE RELEASES",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                horizontal = 20.dp,
                                vertical = 12.dp
                            )
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 24.dp
                            )
                        ) {
                            items(
                                items = state.releases,
                                key = { it.tagName }
                            ) { release ->

                                val isInstalled =
                                    release.tagName == mainState.installedVersion

                                VersionCard(
                                    release = release,
                                    isInstalled = isInstalled,
                                    isInstalling = installingTag == release.tagName,
                                    isUninstalling = uninstalling,
                                    enabled = installingTag == null,
                                    onInstall = {

                                        installingTag = release.tagName
                                        errorMessage = null

                                        scope.launch {

                                            when (
                                                val result =
                                                    repository.downloadAndInstall(release)
                                            ) {

                                                is InstallResult.Success -> {
                                                    onDone()
                                                }

                                                is InstallResult.Failed -> {
                                                    errorMessage = result.reason
                                                    installingTag = null
                                                }
                                            }
                                        }
                                    },
                                    onUninstall = {
                                        showUninstallDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        if (showUninstallDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!uninstalling) {
                        showUninstallDialog = false
                    }
                },
                title = {
                    Text("Uninstall Frida Server?")
                },
                text = {
                    Text(
                        "This will stop the running Frida server, if necessary, " +
                                "and remove version ${mainState.installedVersion}."
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = !uninstalling,
                        onClick = {
                            showUninstallDialog = false
                            uninstalling = true
                            errorMessage = null

                            scope.launch {
                                val success = repository.uninstall()

                                if (success) {
                                    uninstalling = false
                                } else {
                                    uninstalling = false
                                    errorMessage =
                                        "Failed to uninstall Frida server."
                                }
                            }
                        }
                    ) {
                        Text(
                            "UNINSTALL",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !uninstalling,
                        onClick = {
                            showUninstallDialog = false
                        }
                    ) {
                        Text("CANCEL")
                    }
                }
            )
        }
    }
}


/**
 * Shows the currently installed Frida version.
 */
@Composable
private fun CurrentVersionCard(
    version: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "CURRENT VERSION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    text = version.ifBlank { "Unknown" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ) {
                Text(
                    text = "INSTALLED",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 7.dp
                    )
                )
            }
        }
    }
}


/**
 * Individual Frida release.
 */
@Composable
private fun VersionCard(
    release: FridaRelease,
    isInstalled: Boolean,
    isInstalling: Boolean,
    isUninstalling: Boolean,
    enabled: Boolean,
    onInstall: () -> Unit,
    onUninstall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    Text(
                        text = release.tagName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = formatReleaseDate(release.publishedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isInstalled) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 7.dp
                            )
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

            if (isInstalled) {

                Text(
                    text = "This version is currently installed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    enabled = enabled,
                    onClick = onUninstall,
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isUninstalling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )

                        Spacer(modifier = Modifier.size(10.dp))
                    }

                    Text(
                        text = if (isUninstalling) {
                            "UNINSTALLING..."
                        } else {
                            "UNINSTALL VERSION"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }

            } else {

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    enabled = enabled,
                    onClick = onInstall,
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isInstalling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )

                        Spacer(modifier = Modifier.size(10.dp))
                    }

                    Text(
                        text = if (isInstalling) {
                            "INSTALLING..."
                        } else {
                            "INSTALL VERSION"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}


/**
 * Loading state.
 */
@Composable
private fun LoadingVersions() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            CircularProgressIndicator()

            Text(
                text = "Loading Frida releases...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


/**
 * GitHub/network failure state.
 */
@Composable
private fun ReleaseErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Unable to load releases",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = onRetry
            ) {
                Text("RETRY")
            }
        }
    }
}

@Composable
private fun EmptyReleasesState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "GitHub returned no Frida releases.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


/**
 * Error displayed when downloading, verifying,
 * decompressing or installing a release fails.
 */
@Composable
private fun ErrorCard(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text = "INSTALLATION FAILED",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "DISMISS",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}


/**
 * Converts GitHub's ISO timestamp into a simpler date.
 *
 * Example:
 * 2026-08-30T12:34:56Z
 * becomes:
 * 30 Aug 2026
 */
private fun formatReleaseDate(
    publishedAt: String
): String {
    return try {
        val date = publishedAt.substringBefore("T")

        val parts = date.split("-")

        if (parts.size == 3) {
            val year = parts[0]
            val month = parts[1].toInt()
            val day = parts[2]

            val monthName = when (month) {
                1 -> "Jan"
                2 -> "Feb"
                3 -> "Mar"
                4 -> "Apr"
                5 -> "May"
                6 -> "Jun"
                7 -> "Jul"
                8 -> "Aug"
                9 -> "Sep"
                10 -> "Oct"
                11 -> "Nov"
                12 -> "Dec"
                else -> parts[1]
            }

            "$day $monthName $year"
        } else {
            publishedAt.take(10)
        }
    } catch (_: Exception) {
        publishedAt.take(10)
    }
}
