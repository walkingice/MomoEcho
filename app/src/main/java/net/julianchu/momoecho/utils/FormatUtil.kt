package net.julianchu.momoecho.utils

import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

private const val regex = "(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d\\d)"
private val pattern = Pattern.compile(regex)

fun Int.toReadable(): String {
    val millis = this.toLong()
    return String.format(
        "%02d:%02d:%02d.%03d", TimeUnit.MILLISECONDS.toHours(millis),
        TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
        TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1),
        millis - TimeUnit.MILLISECONDS.toSeconds(millis) * 1000
    )
}

fun String.toMillis(): Int {
    val matcher = pattern.matcher(this)
    if (matcher.find()) {
        val hour = matcher.group(1).toInt()
        val mins = hour * 60 + matcher.group(2).toInt()
        val secs = mins * 60 + matcher.group(3).toInt()
        val mill = secs * 1000 + matcher.group(4).toInt()
        return mill
    }
    return 0
}
