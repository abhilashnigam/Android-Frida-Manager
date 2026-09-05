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

    private const val FRIDA_DIR = "/data/local/tmp"
    private const val FRIDA_BIN = "$FRIDA_DIR/frida-server"


    /**
     * Checks whether frida-server is installed.
     */
    suspend fun isInstalled(): Boolean {
        return RootManager.execute(
            "[ -f $FRIDA_BIN ]"
        )
    }

    /**
     * Checks whether frida-server is currently running.
     */
    suspend fun isRunning(): Boolean {
        return RootManager.execute(
            "pgrep -f frida-server >/dev/null 2>&1"
        )
    }

    /**
     * Starts frida-server.
     */
    suspend fun start(ip: String, port: Int): Boolean {
        if (isRunning()) {
            return true
        }

        val started = RootManager.execute(
            "chmod 755 $FRIDA_BIN",
            "nohup $FRIDA_BIN -l $ip:$port >/dev/null 2>&1 &"
        )

        if (!started) {
            return false
        }

        // Give the process a moment to start.
        withContext(Dispatchers.IO) {
            Thread.sleep(500)
        }

        return isRunning()
    }

    /**
     * Stops frida-server.
     */
    suspend fun stop(): Boolean {
        if (!isRunning()) {
            return true
        }

        val stopped = RootManager.execute(
            "pkill -f frida-server"
        )

        if (!stopped) {
            return false
        }

        withContext(Dispatchers.IO) {
            Thread.sleep(300)
        }

        return !isRunning()
    }

    /**
     * Installs a frida-server binary.
     *
     * sourcePath is a path accessible to the root shell,
     * for example a downloaded file in the application's
     * cache directory.
     */
    suspend fun install(sourcePath: String): Boolean {
        return RootManager.execute(
            "cp \"$sourcePath\" \"$FRIDA_BIN\"",
            "chmod 755 \"$FRIDA_BIN\""
        )
    }

    /**
     * Uninstall the currently installed frida-server binary.
     */
    suspend fun uninstall(): Boolean {
        return RootManager.execute(
            "rm -f \"$FRIDA_BIN\""
        )
    }

    /**
     * Returns the installed frida-server version.
     */
    suspend fun installedVersion(): String? {
        val output = RootManager.executeForOutput(
            "$FRIDA_BIN --version"
        )

        return output
            .firstOrNull()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }
}