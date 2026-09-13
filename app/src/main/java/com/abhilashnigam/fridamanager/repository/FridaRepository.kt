package com.abhilashnigam.fridamanager.repository

import android.content.Context
import com.abhilashnigam.fridamanager.arch.ArchDetector
import com.abhilashnigam.fridamanager.data.SettingsStore
import com.abhilashnigam.fridamanager.model.FridaRelease
import com.abhilashnigam.fridamanager.network.GitHubReleaseApi
import com.abhilashnigam.fridamanager.network.ReleaseFetchResult
import com.abhilashnigam.fridamanager.frida.FridaManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.tukaani.xz.XZInputStream
import java.io.File

enum class AppLoadState {
    LOADING,
    READY
}
data class MainScreenState(
    val loadState: AppLoadState = AppLoadState.LOADING,
    val fridaInstalled: Boolean = false,
    val fridaRunning: Boolean = false,
    val installedVersion: String = "",
    val updateAvailable: Boolean = false,
    val latestKnownVersion: String? = null,
    val checkForUpdatesEnabled: Boolean = true
)

sealed class InstallResult {
    data object Success : InstallResult()
    data class Failed(val reason: String) : InstallResult()
}

sealed class AnonymizerResult {
    data object Success : AnonymizerResult()
    data class Failed(val reason: String) : AnonymizerResult()
}

class FridaRepository(
    private val context: Context,
    private val api: GitHubReleaseApi = GitHubReleaseApi(),
    private val settings: SettingsStore = SettingsStore(context)
) {
    private val _state = MutableStateFlow(MainScreenState())
    val state: StateFlow<MainScreenState> = _state

    private val httpClient = OkHttpClient()

    private suspend fun binaryPath(): String = settings.fridaBinaryLocation.first().binaryPath

    /** Step: Detect Architecture -> Check Frida Server -> Read Installed Version ->
     *  Update Check Enabled? -> Latest Version? (all collapsed into one refresh, as
     *  the finalized flowchart routes every branch back to the same Main Screen). */
    suspend fun refresh() {
        val binaryPath = binaryPath()
        val checkEnabled = settings.checkForUpdates.first()
        val installed = FridaManager.isInstalled(binaryPath)
        val running = if (installed) FridaManager.isRunning(binaryPath) else false
        val installedVersion = if (installed) {
            FridaManager.installedVersion(binaryPath).orEmpty()
        } else {
            ""
        }

        var updateAvailable = false
        var latest: String? = null

        if (installed && checkEnabled) {
            when (val result = api.fetchReleases()) {
                is ReleaseFetchResult.Success -> {
                    latest = result.releases.firstOrNull()?.tagName
                    if (latest != null && installedVersion.isNotBlank()) {
                        updateAvailable = latest != installedVersion
                    }
                }
                else -> Unit // Update information remains unavailable; the version screen exposes the cause.
            }
        }

        _state.value = MainScreenState(
            loadState = AppLoadState.READY,
            fridaInstalled = installed,
            fridaRunning = running,
            installedVersion = installedVersion,
            updateAvailable = updateAvailable,
            latestKnownVersion = latest,
            checkForUpdatesEnabled = checkEnabled
        )
    }

    suspend fun availableReleases(): ReleaseFetchResult = api.fetchReleases()

    suspend fun setCheckForUpdates(enabled: Boolean) {
        settings.setCheckForUpdates(enabled)
        refresh()
    }

    suspend fun toggleServer(): Boolean {
        val binaryPath = binaryPath()
        val running = FridaManager.isRunning(binaryPath)

        val result = if (running) {
            FridaManager.stop(binaryPath)
        } else {
            val ip = settings.bindAddress.first()
            val port = settings.fridaPort.first()

            FridaManager.start(binaryPath, ip, port)
        }

        refresh()
        return result
    }


    suspend fun uninstall(): Boolean {
        val binaryPath = binaryPath()
        if (FridaManager.isRunning(binaryPath)) {
            val stopped = FridaManager.stop(binaryPath)

            if (!stopped) {
                return false
            }
        }

        if (!FridaManager.isInstalled(binaryPath)) {
            return true
        }

        val uninstalled = FridaManager.uninstall(binaryPath)

        if (!uninstalled) {
            return false
        }

        refresh()
        return true
    }

    /** Stops the managed server, moves the binary, then commits the new path. */
    suspend fun setAnonymizerEnabled(enabled: Boolean): AnonymizerResult {
        val currentLocation = settings.fridaBinaryLocation.first()
        if (currentLocation.anonymized == enabled) return AnonymizerResult.Success

        val sourcePath = currentLocation.binaryPath
        val targetPath = if (enabled) {
            settings.ensureAnonymizerLocation()
                .copy(anonymized = true)
                .binaryPath
        } else {
            "/data/local/tmp/frida-server"
        }

        if (FridaManager.isRunning(sourcePath) && !FridaManager.stop(sourcePath)) {
            return AnonymizerResult.Failed("Could not stop the managed Frida server.")
        }

        if (FridaManager.isInstalled(sourcePath) &&
            !FridaManager.moveBinary(sourcePath, targetPath)
        ) {
            return AnonymizerResult.Failed("Could not move the Frida binary.")
        }

        settings.setAnonymizerEnabled(enabled)
        refresh()
        return AnonymizerResult.Success
    }
    /**
     * Download -> Verify -> Install -> Set Active Version, per the finalized flow.
     * Any failure at any stage returns Failed so the UI can route back to the
     * Available Versions screen, matching the flowchart's error-recovery edges.
     */
    suspend fun downloadAndInstall(release: FridaRelease): InstallResult =
        withContext(Dispatchers.IO) {
            val binaryPath = binaryPath()
            val arch = try {
                ArchDetector.detectFridaArch()
            } catch (e: ArchDetector.UnsupportedArchException) {
                return@withContext InstallResult.Failed("Unsupported device architecture: ${e.message}")
            }

            val asset = release.assetForArch(arch)
                ?: return@withContext InstallResult.Failed("No build for arch '$arch' in ${release.tagName}")

            val compressedFile = File(context.cacheDir, asset.name)
            val decompressedFile = File(context.cacheDir, "frida-server-${release.tagName}")

            // Download
            try {
                val request = Request.Builder().url(asset.downloadUrl).build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext InstallResult.Failed("Download failed: HTTP ${response.code}")
                    }
                    val body = response.body ?: return@withContext InstallResult.Failed("Empty response body")
                    compressedFile.outputStream().use { out -> body.byteStream().copyTo(out) }
                }
            } catch (e: Exception) {
                return@withContext InstallResult.Failed("Download failed: ${e.message}")
            }

            // Verify: confirm the download is non-empty and the declared size matches
            // (frida doesn't publish per-asset checksums, so a size check plus a
            // successful decompress is the practical integrity gate here).
            if (compressedFile.length() != asset.sizeBytes) {
                compressedFile.delete()
                return@withContext InstallResult.Failed("Verification failed: size mismatch")
            }

            // Decompress (.xz assets use LZMA2; frida also ships plain .gz on some
            // older tags -- this scaffold handles the common .xz case via a helper
            // you can swap for org.tukaani:xz if you need broader coverage).
            try {
                decompressXz(compressedFile, decompressedFile)
            } catch (e: Exception) {
                compressedFile.delete()
                return@withContext InstallResult.Failed("Verification failed: could not decompress (${e.message})")
            } finally {
                compressedFile.delete()
            }

            // Install: stop any running instance first, then push the new binary
            if (FridaManager.isRunning(binaryPath)) {
                FridaManager.stop(binaryPath)
            }

            // Remove the existing Frida server
            if (FridaManager.isInstalled(binaryPath)) {
                val uninstalled = FridaManager.uninstall(binaryPath)

                if (!uninstalled) {
                    decompressedFile.delete()
                    return@withContext InstallResult.Failed(
                        "Could not uninstall existing Frida server"
                    )
                }
            }

            // Install the new binary
            val installed = FridaManager.install(
                decompressedFile.absolutePath,
                binaryPath
            )

            if (!FridaManager.isInstalled(binaryPath)) {
                return@withContext InstallResult.Failed(
                    "Installation completed but frida-server was not found"
                )
            }

            decompressedFile.delete()

            if (!installed) {
                return@withContext InstallResult.Failed(
                    "Root install step failed (chmod/copy denied)"
                )
            }

            refresh()
            InstallResult.Success
        }

    private fun decompressXz(input: File, output: File) {
        XZInputStream(input.inputStream().buffered()).use { xz ->
            output.outputStream().use { out -> xz.copyTo(out) }
        }
    }
}
