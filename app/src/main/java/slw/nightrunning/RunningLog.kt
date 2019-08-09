package slw.nightrunning

import android.content.Context
import android.location.Location
import android.location.LocationManager
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.Calendar.getInstance


data class RunningLog(
    val startTime: Long, val stopTime: Long,
    val stepCount: Int, val route: List<Location>
)

val RunningLog.filename get() = route.run { "${first().time}-${last().time}" }

fun String.parseAsRunningLogFilename(): Pair<Calendar, Calendar> {
    val timeList = split("-").map { getInstance().apply { timeInMillis = it.toLong() } }
    return Pair(timeList[0], timeList[1])
}

const val latestVersion = 2

fun Context.saveRunningLog(runningLog: RunningLog): String? {
    val dir = getDir("runningLogs", Context.MODE_PRIVATE)
    val filename = runningLog.filename
    val file = dir.resolve(filename)
    return try {
        file.outputStream().use { it.writeRunningLog(runningLog) }
        filename
    } catch (e: Exception) {
        null
    }
}

fun Context.loadRunningLog(filename: String): RunningLog? {
    val dir = getDir("runningLogs", Context.MODE_PRIVATE)
    val file = dir.resolve(filename)
    return try {
        file.inputStream().use { it.readRunningLog() }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun Context.deleteRunningLog(filename: String): Boolean {
    val dir = getDir("runningLogs", Context.MODE_PRIVATE)
    val file = dir.resolve(filename)
    return try {
        file.delete()
    } catch (e: Exception) {
        false
    }
}

fun OutputStream.writeRunningLog(runningLog: RunningLog) = DataOutputStream(this).writeRunningLog(runningLog)

fun InputStream.readRunningLog(): RunningLog = DataInputStream(this).readRunningLog()

fun DataOutputStream.writeRunningLog(runningLog: RunningLog) = DataOutputStream(this).run {
    writeInt(latestVersion)
    writeLong(runningLog.startTime)
    writeLong(runningLog.stopTime)
    writeInt(runningLog.stepCount)
    writeInt(runningLog.route.size)
    runningLog.route.forEach(this::writeLocation)
}

fun DataInputStream.readRunningLog(): RunningLog {
    return when (val version = readInt()) {
        latestVersion -> {
            val startTime = readLong()
            val stopTime = readLong()
            val stepCount = readInt()
            val route = Array(readInt()) { readLocation() }.asList()
            RunningLog(startTime, stopTime, stepCount, route)
        }
        1 -> {
            val stepCount = readInt()
            val route = Array(readInt()) { readLocation() }.asList()
            val startTime = route.first().time
            val stopTime = route.last().time
            RunningLog(startTime, stopTime, stepCount, route)
        }
        else -> throw RuntimeException("Unresolved version $version")
    }
}

fun DataOutputStream.writeLocation(location: Location) {
    writeLong(location.time)
    writeDouble(location.latitude)
    writeDouble(location.longitude)
    writeDouble(location.altitude)
}

fun DataInputStream.readLocation() = Location(LocationManager.GPS_PROVIDER).apply {
    time = readLong()
    latitude = readDouble()
    longitude = readDouble()
    altitude = readDouble()
}

