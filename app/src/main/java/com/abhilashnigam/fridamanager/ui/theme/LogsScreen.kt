package com.abhilashnigam.fridamanager.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abhilashnigam.fridamanager.repository.FridaRepository
import kotlinx.coroutines.launch

private enum class LogLevel {
    ALL,
    INFO,
    WARN,
    ERROR,
    DEBUG
}

private data class LogEntry(
    val level: LogLevel,
    val timestamp: String,
    val tag: String,
    val message: String
)

/*
 * Severity colours
 */
private val infoColor = Color(0xFF16A085)
private val warnColor = Color(0xFFF39C12)
private val errorColor = Color(0xFFE74C3C)
private val debugColor = Color(0xFFD45A91)

/*
 * Severity gradients
 */
private val infoGradient = Brush.linearGradient(
    listOf(
        Color(0xFF11998E),
        Color(0xFF38EF7D)
    )
)

private val warnGradient = Brush.linearGradient(
    listOf(
        Color(0xFFF7971E),
        Color(0xFFFFD200)
    )
)

private val errorGradient = Brush.linearGradient(
    listOf(
        Color(0xFFEB3349),
        Color(0xFFF45C43)
    )
)

private val debugGradient = Brush.linearGradient(
    listOf(
        Color(0xFFE0609E),
        Color(0xFFF0954B)
    )
)

private fun levelColor(level: LogLevel): Color {
    return when (level) {
        LogLevel.INFO -> infoColor
        LogLevel.WARN -> warnColor
        LogLevel.ERROR -> errorColor
        LogLevel.DEBUG -> debugColor
        LogLevel.ALL -> debugColor
    }
}

private fun levelGradient(level: LogLevel): Brush {
    return when (level) {
        LogLevel.INFO -> infoGradient
        LogLevel.WARN -> warnGradient
        LogLevel.ERROR -> errorGradient
        LogLevel.DEBUG -> debugGradient
        LogLevel.ALL -> debugGradient
    }
}

private fun levelLabel(level: LogLevel): String {
    return when (level) {
        LogLevel.INFO -> "INFO"
        LogLevel.WARN -> "WARNING"
        LogLevel.ERROR -> "ERROR"
        LogLevel.DEBUG -> "DEBUG"
        LogLevel.ALL -> "ALL"
    }
}

private fun levelLetter(level: LogLevel): String {
    return when (level) {
        LogLevel.INFO -> "I"
        LogLevel.WARN -> "W"
        LogLevel.ERROR -> "E"
        LogLevel.DEBUG -> "D"
        LogLevel.ALL -> "•"
    }
}

@Composable
fun LogsScreen(
    repository: FridaRepository,
    onDone: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var logs by remember { mutableStateOf<List<LogEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedLevel by remember { mutableStateOf(LogLevel.ALL) }
    var showClearDialog by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            isLoading = true

            val rawLogs = repository.readServerLogcat()

            logs = rawLogs.map { parseLogLine(it) }

            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    val filteredLogs = remember(logs, selectedLevel) {
        if (selectedLevel == LogLevel.ALL) {
            logs
        } else {
            logs.filter { it.level == selectedLevel }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp)
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            /*
             * Header
             */
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Debug Logs",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(
                                    color = infoColor,
                                    shape = CircleShape
                                )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "Frida Server  •  ${logs.size} entries",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                /*
                 * Refresh
                 */
                LogActionButton(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh logs",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = ::refresh
                )

                Spacer(modifier = Modifier.width(8.dp))

                /*
                 * Clear
                 */
                LogActionButton(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear logs",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = {
                        showClearDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            /*
             * Severity filters
             */
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                item {
                    LogFilterChip(
                        text = "ALL",
                        level = LogLevel.ALL,
                        selected = selectedLevel == LogLevel.ALL,
                        onClick = {
                            selectedLevel = LogLevel.ALL
                        }
                    )
                }

                item {
                    LogFilterChip(
                        text = "INFO",
                        level = LogLevel.INFO,
                        selected = selectedLevel == LogLevel.INFO,
                        onClick = {
                            selectedLevel = LogLevel.INFO
                        }
                    )
                }

                item {
                    LogFilterChip(
                        text = "WARN",
                        level = LogLevel.WARN,
                        selected = selectedLevel == LogLevel.WARN,
                        onClick = {
                            selectedLevel = LogLevel.WARN
                        }
                    )
                }

                item {
                    LogFilterChip(
                        text = "ERROR",
                        level = LogLevel.ERROR,
                        selected = selectedLevel == LogLevel.ERROR,
                        onClick = {
                            selectedLevel = LogLevel.ERROR
                        }
                    )
                }

                item {
                    LogFilterChip(
                        text = "DEBUG",
                        level = LogLevel.DEBUG,
                        selected = selectedLevel == LogLevel.DEBUG,
                        onClick = {
                            selectedLevel = LogLevel.DEBUG
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            /*
             * Content
             */
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                filteredLogs.isEmpty() -> {
                    EmptyLogsState(
                        filter = selectedLevel
                    )
                }

                else -> {
                    LogList(filteredLogs)
                }
            }
        }
    }

    /*
     * Clear confirmation
     */
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = {
                showClearDialog = false
            },
            title = {
                Text("Clear displayed logs?")
            },
            text = {
                Text(
                    "This clears the logs currently displayed in Frida Manager. " +
                            "It does not stop or restart the Frida server."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        logs = emptyList()
                        showClearDialog = false
                    }
                ) {
                    Text(
                        text = "Clear",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LogActionButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        onClick = onClick
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
    }
}

@Composable
private fun LogFilterChip(
    text: String,
    level: LogLevel,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = levelColor(level)

    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (selected) {
                    Modifier.background(levelGradient(level))
                } else {
                    Modifier
                        .background(
                            MaterialTheme.colorScheme.surface
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.25f
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                }
            )
            .clickable(
                interactionSource = remember {
                    MutableInteractionSource()
                },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 17.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) {
                Color.White
            } else {
                color
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LogList(
    entries: List<LogEntry>
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                vertical = 6.dp
            )
        ) {
            items(
                items = entries,
                key = {
                    "${it.timestamp}-${it.tag}-${it.message.hashCode()}"
                }
            ) { entry ->

                LogRow(entry)

                HorizontalDivider(
                    modifier = Modifier.padding(start = 78.dp),
                    color = MaterialTheme.colorScheme.outline.copy(
                        alpha = 0.07f
                    )
                )
            }
        }
    }
}

@Composable
private fun LogRow(
    entry: LogEntry
) {
    val color = levelColor(entry.level)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 14.dp
            ),
        verticalAlignment = Alignment.Top
    ) {

        /*
         * Gradient severity badge
         */
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    brush = levelGradient(entry.level)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = levelLetter(entry.level),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            /*
             * Severity + timestamp
             */
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = levelLabel(entry.level),
                    color = color,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                if (entry.timestamp.isNotBlank()) {
                    Text(
                        text = entry.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            /*
             * Tag
             */
            if (entry.tag.isNotBlank()) {

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = entry.tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            /*
             * Message
             */
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = when (entry.level) {
                    LogLevel.ERROR,
                    LogLevel.WARN -> color

                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun EmptyLogsState(
    filter: LogLevel
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (filter == LogLevel.ALL) {
                    "No logs available"
                } else {
                    "No ${levelLabel(filter).lowercase()} logs"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (filter == LogLevel.ALL) {
                    "Frida Server has not produced any log output yet."
                } else {
                    "No log entries match the selected severity."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/*
 * Parse Android logcat output.
 *
 * Example:
 *
 * 09-13 11:11:40.706  9103  9108 W ziparchive:
 * Unable to open /data/local/tmp/example
 */
private fun parseLogLine(
    line: String
): LogEntry {

    val regex = Regex(
        """^\d{2}-\d{2}\s+(\d{2}:\d{2}:\d{2}\.\d+).*?\s([VDIWEF])\s+([^:]+):?\s*(.*)$"""
    )

    val match = regex.find(line)

    if (match != null) {

        val timestamp = match.groupValues[1]
        val priority = match.groupValues[2]
        val tag = match.groupValues[3].trim()
        val message = match.groupValues[4].trim()

        val level = when (priority) {
            "W" -> LogLevel.WARN
            "E", "F" -> LogLevel.ERROR
            "D", "V" -> LogLevel.DEBUG
            else -> LogLevel.INFO
        }

        return LogEntry(
            level = level,
            timestamp = timestamp,
            tag = tag,
            message = message.ifBlank { line }
        )
    }

    /*
     * Frida output doesn't always follow logcat formatting.
     * Keep those lines instead of throwing them away.
     */
    val level = when {
        line.contains("exception", ignoreCase = true) ||
                line.contains("fatal", ignoreCase = true) ||
                line.contains("error", ignoreCase = true) -> {
            LogLevel.ERROR
        }

        line.contains("warning", ignoreCase = true) ||
                line.contains("warn", ignoreCase = true) -> {
            LogLevel.WARN
        }

        line.contains("debug", ignoreCase = true) -> {
            LogLevel.DEBUG
        }

        else -> {
            LogLevel.INFO
        }
    }

    return LogEntry(
        level = level,
        timestamp = "",
        tag = "",
        message = line
    )
}