package slw.nightrunning

import android.graphics.Color
import android.location.Location
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.CircleOptions
import com.baidu.mapapi.map.PolylineOptions
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.utils.CoordinateConverter
import com.baidu.mapapi.utils.CoordinateConverter.CoordType.GPS
import java.util.*

val List<Location>.geoLength: Double
    get() = (1 until size).sumByDouble { this[it - 1].distanceTo(this[it]).toDouble() }

val List<Location>.timeSpan: Long
    get() = if (size != 0) last().time - first().time else 0L


fun Location.toLatLng(): LatLng {
    return CoordinateConverter()
        .from(GPS)
        .coord(LatLng(latitude, longitude))
        .convert()
}

fun BaiduMap.addLocationPoint(latLng: LatLng) {
    val circleOptions = CircleOptions()
        .center(latLng)
        .radius(10)
        .fillColor(Color.RED)
    addOverlay(circleOptions)
}

fun BaiduMap.addRouteLines(route: List<LatLng>) {
    val polylineOptions = PolylineOptions()
        .points(route)
        .color(Color.BLUE)
        .width(7)
    addOverlay(polylineOptions)
}


fun Pair<Calendar, Calendar>.timeSpanDescription(): String {
    val sb = StringBuilder()
    sb.append(first.get(Calendar.YEAR), "-")
    sb.append(first.get(Calendar.MONTH), "-")
    sb.append(first.get(Calendar.DAY_OF_MONTH), " ")
    sb.append(first.get(Calendar.HOUR_OF_DAY).let { "%02d".format(it) }, ":")
    sb.append(first.get(Calendar.MINUTE).let { "%02d".format(it) })

    sb.append(" ~ ")
    var skipSame = true

    skipSame = skipSame && first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
    if (!skipSame) sb.append(sb.append(second.get(Calendar.YEAR), "-"))

    skipSame = skipSame
            && first.get(Calendar.MONTH) == second.get(Calendar.MONTH)
            && first.get(Calendar.DAY_OF_MONTH) == second.get(Calendar.DAY_OF_MONTH)
    if (!skipSame) {
        sb.append(sb.append(second.get(Calendar.MONTH), "-"))
        sb.append(sb.append(second.get(Calendar.DAY_OF_MONTH), " "))
    }

    sb.append(second.get(Calendar.HOUR_OF_DAY).let { "%02d".format(it) }, ":")
    sb.append(second.get(Calendar.MINUTE).let { "%02d".format(it) })

    return sb.toString()
}

fun Long.timeDescription(): String {
    var time = this

//    val microSecond = time % 1000
    time /= 1000

    val second = time % 60
    time /= 60

    val minute = time % 60
    time /= 60

    val hour = time % 24
    time /= 24

//    val day = time

    if (hour == 0L) {
        return "%02d:%02d".format(minute, second)
    } else {
        return "%d:%02d:%02d".format(hour, minute, second)
    }
}