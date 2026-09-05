package com.abhilashnigam.fridamanager

import android.app.Application
import com.topjohnwu.superuser.Shell

class FridaManagerApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Configure libsu once, globally, before any Shell.getShell() call is made.
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(15)
        )
    }
}