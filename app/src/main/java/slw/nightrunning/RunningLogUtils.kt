package slw.nightrunning

import android.content.Context
import android.location.Location
import android.location.LocationManager
import java.io.*


// file

fun getRunningLogFilename(runningLog: RunningLog): String {
    return runningLog.run { "$startTime-$stopTime" }
}

fun parseRunningLogFilename(filename: String): Pair<Long, Long> {
    val timeList = filename.split("-").map { it.toLong() }
    return Pair(timeList[0], timeList[1])
}

fun Context.saveRunningLog(runningLog: RunningLog): String? {
    val dir = getDir("runningLogs", Context.MODE_PRIVATE)
    val filename = getRunningLogFilename(runningLog)
    val file = dir.resolve(filename)
    return try {
        file.outputStream().use { it.writeRunningLog(runningLog) }
        filename
    } catch (e: Exception) {
        null
    }
}

fun Context.listRunningLogs(): Array<out File> {
    val dir = getDir("runningLogs", Context.MODE_PRIVATE)
    return dir.listFiles() ?: arrayOf()
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


// serialization

const val latestVersion = 1

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
    assert(readInt() == latestVersion)
    val startTime = readLong()
    val stopTime = readLong()
    val stepCount = readInt()
    val route = Array(readInt()) { readLocation() }.asList()
    return RunningLog(startTime, stopTime, stepCount, route)
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
