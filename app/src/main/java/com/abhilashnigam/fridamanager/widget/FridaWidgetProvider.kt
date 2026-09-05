package com.abhilashnigam.fridamanager.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

import com.abhilashnigam.fridamanager.MainActivity
import com.abhilashnigam.fridamanager.R
import com.abhilashnigam.fridamanager.data.SettingsStore
import com.abhilashnigam.fridamanager.repository.FridaRepository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FridaWidgetProvider : AppWidgetProvider() {

    companion object {

        private const val ACTION_TOGGLE =
            "com.abhilashnigam.fridamanager.ACTION_TOGGLE"

        /**
         * Called by the Frida monitor service when the server
         * state changes.
         */
        fun pushStatusUpdate(
            context: Context,
            running: Boolean
        ) {
            CoroutineScope(Dispatchers.IO).launch {

                val appContext = context.applicationContext

                val settings = SettingsStore(appContext)

                val bindAddress =
                    settings.bindAddress.first()

                val port =
                    settings.fridaPort.first()

                updateAllWidgets(
                    context = appContext,
                    running = running,
                    bindAddress = bindAddress,
                    port = port
                )
            }
        }

        /**
         * Updates every instance of the Frida widget.
         */
        private fun updateAllWidgets(
            context: Context,
            running: Boolean,
            bindAddress: String,
            port: Int
        ) {
            val manager =
                AppWidgetManager.getInstance(context)

            val component =
                ComponentName(
                    context,
                    FridaWidgetProvider::class.java
                )

            val widgetIds =
                manager.getAppWidgetIds(component)

            widgetIds.forEach { widgetId ->

                renderWidget(
                    context = context,
                    manager = manager,
                    widgetId = widgetId,
                    running = running,
                    bindAddress = bindAddress,
                    port = port
                )
            }
        }

        /**
         * Updates one widget instance.
         */
        private fun renderWidget(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            running: Boolean,
            bindAddress: String,
            port: Int
        ) {
            val views =
                RemoteViews(
                    context.packageName,
                    R.layout.widget_frida
                )

            /*
             * ---------------------------------------------------------
             * Status
             * ---------------------------------------------------------
             */

            if (running) {

                views.setTextViewText(
                    R.id.widget_status_text,
                    "SERVER RUNNING"
                )

                views.setTextViewText(
                    R.id.widget_status_indicator,
                    ""
                )

                views.setInt(
                    R.id.widget_status_indicator,
                    "setBackgroundResource",
                    R.drawable.widget_status_running
                )

                views.setTextViewText(
                    R.id.widget_endpoint_label,
                    "Running at"
                )

                views.setTextViewText(
                    R.id.widget_toggle_button,
                    "STOP"
                )
                views.setInt(
                    R.id.widget_toggle_button,
                    "setBackgroundResource",
                    R.drawable.widget_button_stop
                )

            } else {

                views.setTextViewText(
                    R.id.widget_status_text,
                    "SERVER STOPPED"
                )

                views.setTextViewText(
                    R.id.widget_status_indicator,
                    ""
                )

                views.setInt(
                    R.id.widget_status_indicator,
                    "setBackgroundResource",
                    R.drawable.widget_status_stopped
                )

                views.setTextViewText(
                    R.id.widget_endpoint_label,
                    "Configured at"
                )

                views.setTextViewText(
                    R.id.widget_toggle_button,
                    "START"
                )
                views.setInt(
                    R.id.widget_toggle_button,
                    "setBackgroundResource",
                    R.drawable.widget_button_start
                )
            }

            /*
             * ---------------------------------------------------------
             * Endpoint
             * ---------------------------------------------------------
             */

            views.setTextViewText(
                R.id.widget_endpoint_text,
                "$bindAddress:$port"
            )

            /*
             * ---------------------------------------------------------
             * Toggle button
             * ---------------------------------------------------------
             */

            val toggleIntent =
                Intent(context, FridaWidgetProvider::class.java).apply {
                    action = ACTION_TOGGLE
                }

            val togglePendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    widgetId,
                    toggleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                )

            views.setOnClickPendingIntent(
                R.id.widget_toggle_button,
                togglePendingIntent
            )

            /*
             * ---------------------------------------------------------
             * Open application when widget body is clicked
             * ---------------------------------------------------------
             */

            val openAppIntent =
                Intent(context, MainActivity::class.java)

            val openAppPendingIntent =
                PendingIntent.getActivity(
                    context,
                    widgetId + 10000,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                )

            views.setOnClickPendingIntent(
                R.id.widget_title,
                openAppPendingIntent
            )

            /*
             * ---------------------------------------------------------
             * Apply update
             * ---------------------------------------------------------
             */

            manager.updateAppWidget(
                widgetId,
                views
            )
        }
    }

    /**
     * Called by Android when widgets need to be updated.
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val appContext =
                    context.applicationContext

                val repository =
                    FridaRepository(appContext)

                /*
                 * Get the real Frida state.
                 */
                repository.refresh()

                val state =
                    repository.state.value

                /*
                 * Get configured endpoint.
                 */
                val settings =
                    SettingsStore(appContext)

                val bindAddress =
                    settings.bindAddress.first()

                val port =
                    settings.fridaPort.first()

                /*
                 * Render every widget instance.
                 */
                appWidgetIds.forEach { widgetId ->

                    renderWidget(
                        context = appContext,
                        manager = appWidgetManager,
                        widgetId = widgetId,
                        running = state.fridaRunning,
                        bindAddress = bindAddress,
                        port = port
                    )
                }

            } finally {

                pendingResult.finish()
            }
        }
    }

    /**
     * Handles widget button presses.
     */
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        super.onReceive(context, intent)

        if (intent.action != ACTION_TOGGLE) {
            return
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val appContext =
                    context.applicationContext

                val repository =
                    FridaRepository(appContext)

                /*
                 * Toggle uses the same repository logic as
                 * the main application.
                 */
                repository.toggleServer()

                /*
                 * Read the resulting state.
                 */
                val state =
                    repository.state.value

                /*
                 * Read current endpoint configuration.
                 */
                val settings =
                    SettingsStore(appContext)

                val bindAddress =
                    settings.bindAddress.first()

                val port =
                    settings.fridaPort.first()

                /*
                 * Update all widget instances.
                 */
                val manager =
                    AppWidgetManager.getInstance(appContext)

                val component =
                    ComponentName(
                        appContext,
                        FridaWidgetProvider::class.java
                    )

                val widgetIds =
                    manager.getAppWidgetIds(component)

                widgetIds.forEach { widgetId ->

                    renderWidget(
                        context = appContext,
                        manager = manager,
                        widgetId = widgetId,
                        running = state.fridaRunning,
                        bindAddress = bindAddress,
                        port = port
                    )
                }

            } finally {

                pendingResult.finish()
            }
        }
    }

    /**
     * Called when the last widget is removed.
     */
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
    }
}