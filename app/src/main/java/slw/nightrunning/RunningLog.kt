package slw.nightrunning

import android.content.Context
import android.location.Location
import android.location.LocationManager
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream


data class RunningLog(val stepCount: Int, val route: List<Location>) {
    val startTime get() = route.first().time
    val stopTime get() = route.last().time
    val timeSpan get() = stopTime - startTime
}

const val version = 1

fun Context.saveRunningLog(runningLog: RunningLog): String? {
    val dir = getDir("runningLogs", Context.MODE_PRIVATE)
    val filename = "${runningLog.startTime}-${runningLog.stopTime}"
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
    writeInt(version)
    writeInt(runningLog.stepCount)
    writeInt(runningLog.route.size)
    runningLog.route.forEach(this::writeLocation)
}

fun DataInputStream.readRunningLog(): RunningLog {
    assert(readInt() == 1)
    val stepCount = readInt()
    val route = Array(readInt()) { readLocation() }.asList()
    return RunningLog(stepCount, route)
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

