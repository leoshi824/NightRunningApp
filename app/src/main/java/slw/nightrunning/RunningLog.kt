package slw.nightrunning

import android.location.Location
import android.location.LocationManager
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream


data class RunningLog(val stepCount: Int, val route: List<Location>) {
    val startTime get() = route.first().time
    val stopTime get() = route.last().time
    val filename get() = "$startTime-$stopTime"
}

const val version = 1

fun OutputStream.writeRunningLog(runningLog: RunningLog) = DataOutputStream(this).writeRunningLog(runningLog)

fun InputStream.readRunningLog(): RunningLog = DataInputStream(this).readRunningLog()

fun DataOutputStream.writeRunningLog(runningLog: RunningLog) = DataOutputStream(this).run {
    writeInt(version)
    writeInt(runningLog.stepCount)
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

