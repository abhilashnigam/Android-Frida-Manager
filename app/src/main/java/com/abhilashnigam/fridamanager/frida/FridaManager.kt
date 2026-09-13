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
            appendLog(binaryPath, "Start requested; server is already running.")
            return true
        }

        appendLog(binaryPath, "Starting frida-server on $ip:$port.")
        val started = RootManager.execute(
            "chmod 755 ${shellQuote(binaryPath)}",
            "nohup ${shellQuote(binaryPath)} -l ${shellQuote("$ip:$port")} >> ${shellQuote(logPath(binaryPath))} 2>&1 &"
        )

        if (!started) {
            appendLog(binaryPath, "Failed to launch frida-server.")
            return false
        }

        // Give the process a moment to start.
        withContext(Dispatchers.IO) {
            Thread.sleep(500)
        }

        val running = isRunning(binaryPath)
        appendLog(
            binaryPath,
            if (running) "frida-server started successfully." else "frida-server exited during startup."
        )
        return running
    }

    /**
     * Stops frida-server.
     */
    suspend fun stop(binaryPath: String): Boolean {
        if (!isRunning(binaryPath)) {
            appendLog(binaryPath, "Stop requested; server is not running.")
            return true
        }

        appendLog(binaryPath, "Stopping frida-server.")
        val stopped = RootManager.execute(
            "pkill -f ${shellQuote(binaryPath)}"
        )

        if (!stopped) {
            appendLog(binaryPath, "Failed to send stop signal to frida-server.")
            return false
        }

        withContext(Dispatchers.IO) {
            Thread.sleep(300)
        }

        val stoppedRunningServer = !isRunning(binaryPath)
        appendLog(
            binaryPath,
            if (stoppedRunningServer) "frida-server stopped." else "frida-server is still running after stop request."
        )
        return stoppedRunningServer
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

    /** Returns the most recent server stdout/stderr lines, if any. */
    suspend fun readLogs(binaryPath: String, maxLines: Int = 500): List<String> =
        RootManager.executeForOutput(
            "tail -n $maxLines ${shellQuote(logPath(binaryPath))} 2>/dev/null"
        )

    /** Reads logcat only for the currently running managed Frida process. */
    suspend fun readLogcat(binaryPath: String, maxLines: Int = 500): List<String> =
        RootManager.executeForOutput(
            "pid=\$(pgrep -f ${shellQuote(binaryPath)} | head -n 1); " +
                "[ -n \"\$pid\" ] && logcat -d --pid=\"\$pid\" -t $maxLines"
        )

    private suspend fun appendLog(binaryPath: String, message: String) {
        val timestampedMessage = "${System.currentTimeMillis()} $message"
        RootManager.execute(
            "printf '%s\\n' ${shellQuote(timestampedMessage)} >> ${shellQuote(logPath(binaryPath))}"
        )
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

    private fun logPath(binaryPath: String): String = "$binaryPath.log"
}
