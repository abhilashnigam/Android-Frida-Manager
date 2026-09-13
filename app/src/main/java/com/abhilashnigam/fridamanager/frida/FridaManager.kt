package com.abhilashnigam.fridamanager.frida


import com.abhilashnigam.fridamanager.root.RootManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles Frida server operations.
 *
 * This class is responsible only for Frida.
 * It does not care whether root is provided by Magisk,
 * KernelSU, APatch, or another root implementation.
 *
 * All privileged operations are delegated to RootManager.
 */
object FridaManager {

    /**
     * Checks whether frida-server is installed.
     */
    suspend fun isInstalled(binaryPath: String): Boolean {
        return RootManager.execute(
            "[ -f ${shellQuote(binaryPath)} ]"
        )
    }

    /**
     * Checks whether frida-server is currently running.
     */
    suspend fun isRunning(binaryPath: String): Boolean {
        return RootManager.execute(
            "pgrep -f ${shellQuote(binaryPath)} >/dev/null 2>&1"
        )
    }

    /**
     * Starts frida-server.
     */
    suspend fun start(binaryPath: String, ip: String, port: Int): Boolean {
        if (isRunning(binaryPath)) {
            return true
        }

        val started = RootManager.execute(
            "chmod 755 ${shellQuote(binaryPath)}",
            "nohup ${shellQuote(binaryPath)} -l ${shellQuote("$ip:$port")} >/dev/null 2>&1 &"
        )

        if (!started) {
            return false
        }

        // Give the process a moment to start.
        withContext(Dispatchers.IO) {
            Thread.sleep(500)
        }

        return isRunning(binaryPath)
    }

    /**
     * Stops frida-server.
     */
    suspend fun stop(binaryPath: String): Boolean {
        if (!isRunning(binaryPath)) {
            return true
        }

        val stopped = RootManager.execute(
            "pkill -f ${shellQuote(binaryPath)}"
        )

        if (!stopped) {
            return false
        }

        withContext(Dispatchers.IO) {
            Thread.sleep(300)
        }

        return !isRunning(binaryPath)
    }

    /**
     * Installs a frida-server binary.
     *
     * sourcePath is a path accessible to the root shell,
     * for example a downloaded file in the application's
     * cache directory.
     */
    suspend fun install(sourcePath: String, binaryPath: String): Boolean {
        val parentPath = binaryPath.substringBeforeLast('/')
        return RootManager.execute(
            "mkdir -p ${shellQuote(parentPath)}",
            "cp ${shellQuote(sourcePath)} ${shellQuote(binaryPath)}",
            "chmod 755 ${shellQuote(binaryPath)}"
        )
    }

    /**
     * Uninstall the currently installed frida-server binary.
     */
    suspend fun uninstall(binaryPath: String): Boolean {
        return RootManager.execute(
            "rm -f ${shellQuote(binaryPath)}"
        )
    }

    /**
     * Returns the installed frida-server version.
     */
    suspend fun installedVersion(binaryPath: String): String? {
        val output = RootManager.executeForOutput(
            "${shellQuote(binaryPath)} --version"
        )

        return output
            .firstOrNull()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    /** Moves a stopped binary without changing its mode bits or ownership. */
    suspend fun moveBinary(sourcePath: String, destinationPath: String): Boolean {
        val destinationDirectory = destinationPath.substringBeforeLast('/')
        return RootManager.execute(
            "mkdir -p ${shellQuote(destinationDirectory)}",
            "mv ${shellQuote(sourcePath)} ${shellQuote(destinationPath)}",
            "[ -f ${shellQuote(destinationPath)} ]"
        )
    }

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\\"'\\\"'")}'"
}
