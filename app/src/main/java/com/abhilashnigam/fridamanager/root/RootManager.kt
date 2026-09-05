package com.abhilashnigam.fridamanager.root

/**
 * High-level root management.
 *
 * The rest of the application should use this class instead
 * of interacting directly with libsu.
 *
 * This keeps the application independent from the actual
 * root implementation being used on the device.
 */
object RootManager {

    sealed class RootState {
        data object Granted : RootState()
        data object Denied : RootState()
    }

    /**
     * Checks whether the application currently has root access.
     */
    suspend fun checkRoot(): RootState {
        return if (RootShell.isRootAvailable()) {
            RootState.Granted
        } else {
            RootState.Denied
        }
    }

    /**
     * Requests/initializes a root shell through libsu.
     *
     * Magisk / KernelSU / APatch handles the actual
     * authorization prompt.
     */
    suspend fun requestRoot(): RootState {
        return checkRoot()
    }

    /**
     * Executes a privileged command.
     */
    suspend fun execute(command: String): Boolean {
        return RootShell.execute(command)
    }

    /**
     * Executes multiple privileged commands.
     */
    suspend fun execute(vararg commands: String): Boolean {
        return RootShell.execute(*commands)
    }

    /**
     * Executes a command and returns its output.
     */
    suspend fun executeForOutput(command: String): List<String> {
        return RootShell.executeForOutput(command)
    }
}