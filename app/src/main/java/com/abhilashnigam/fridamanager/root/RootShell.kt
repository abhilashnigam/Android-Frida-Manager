package com.abhilashnigam.fridamanager.root

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val FRIDA_DIR = "/data/local/tmp"
private const val FRIDA_BIN = "$FRIDA_DIR/frida-server"

/**
 * Low-level wrapper around libsu.
 *
 * This class is intentionally unaware of:
 * - Magisk
 * - KernelSU
 * - APatch
 * - Frida
 *
 * It only provides a way to execute commands through a shell.
 */
object RootShell {

    /**
     * Checks whether a root shell is available.
     *
     * libsu handles the underlying root implementation,
     * whether that is Magisk, KernelSU, APatch, etc.
     */
    suspend fun isRootAvailable(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Shell.getShell().isRoot
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Executes one or more commands through the root shell.
     *
     * Returns true when all commands execute successfully.
     */
    suspend fun execute(vararg commands: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val result = Shell.cmd(*commands).exec()
                result.isSuccess
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Executes a command and returns its output.
     */
    suspend fun executeForOutput(command: String): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                Shell.cmd(command)
                    .exec()
                    .out
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}