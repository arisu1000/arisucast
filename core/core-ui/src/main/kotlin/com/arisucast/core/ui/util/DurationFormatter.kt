package com.arisucast.core.ui.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd")

fun formatPublishedDate(instant: Instant): String =
    DATE_FORMATTER.format(instant.atZone(ZoneId.systemDefault()))

fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, secs)
    } else {
        "%d:%02d".format(minutes, secs)
    }
}

fun formatDurationMs(milliseconds: Long): String = formatDuration((milliseconds / 1000).toInt())
