package slw.nightrunning

import android.location.Location
import android.location.LocationManager
import kotlin.math.absoluteValue


val List<Location>.geoLength: Float
    get() {
        var sum = 0f
        for (i in 1 until size) {
            sum += this[i - 1].distanceTo(this[i])
        }
        return sum
    }

val List<Location>.liveSpeed: Float
    get() = if (size >= 2) this[size - 1].velocityTo(this[size - 2]) else 0f

val List<Location>.averageSpeed: Float
    get() = if (size >= 2) geoLength / (last().time - first().time).toFloat() * 1000f else 0f

fun List<Location>.zone(): Triple<Float, Float, Location> {
    val latN = map { it.latitude }.max()!!
    val latS = map { it.latitude }.min()!!
    val lngE = map { it.longitude }.max()!!
    val lngW = map { it.longitude }.min()!!

    val pointN = Location(LocationManager.GPS_PROVIDER).apply {
        latitude = latN
        longitude = (lngE + lngW) / 2.0
    }
    val pointS = Location(LocationManager.GPS_PROVIDER).apply {
        latitude = latS
        longitude = (lngE + lngW) / 2.0
    }
    val pointE = Location(LocationManager.GPS_PROVIDER).apply {
        latitude = (latN + latS) / 2.0
        longitude = lngE
    }
    val pointW = Location(LocationManager.GPS_PROVIDER).apply {
        latitude = (latN + latS) / 2.0
        longitude = lngW
    }
    val pointCenter = Location(LocationManager.GPS_PROVIDER).apply {
        latitude = (latN + latS) / 2.0
        longitude = (lngE + lngW) / 2.0
    }

    val distanceNS = pointN.distanceTo(pointS)
    val distanceEW = pointE.distanceTo(pointW)
    return Triple(distanceNS, distanceEW, pointCenter)
}

fun Location.velocityTo(other: Location): Float {
    val seconds = (other.time - this.time).absoluteValue.toFloat() / 1000.0f
    return other.distanceTo(this) / seconds
}
