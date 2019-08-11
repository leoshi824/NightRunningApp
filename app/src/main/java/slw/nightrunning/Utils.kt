package slw.nightrunning

import android.content.Context
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.utils.CoordinateConverter
import com.baidu.mapapi.utils.CoordinateConverter.CoordType.GPS
import java.util.*
import kotlin.math.*


val List<Location>.geoLength: Double
    get() = (1 until size).sumByDouble { this[it - 1].distanceTo(this[it]).toDouble() }

val List<Location>.timeSpan: Long
    get() = if (size != 0) last().time - first().time else 0L

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

fun Location.toLatLng(): LatLng {
    return gpsToBdl(latitude, longitude)
}

fun gpsToBdl(latitude: Double, longitude: Double): LatLng {
    return CoordinateConverter()
        .from(GPS)
        .coord(LatLng(latitude, longitude))
        .convert()
}


fun BaiduMap.addLivePoint(latLng: LatLng) {
    val circleOptions = CircleOptions()
        .center(latLng)
        .radius(12)
        .stroke(Stroke(5, Color.BLACK))
        .fillColor(Color.YELLOW)
    addOverlay(circleOptions)
}

fun BaiduMap.addEndPoint(latLng: LatLng) {
    val circleOptions = CircleOptions()
        .center(latLng)
        .radius(12)
        .stroke(Stroke(5, Color.BLACK))
        .fillColor(Color.RED)
    addOverlay(circleOptions)
}

fun BaiduMap.addStartPoint(latLng: LatLng) {
    val circleOptions = CircleOptions()
        .center(latLng)
        .radius(12)
        .stroke(Stroke(5, Color.BLACK))
        .fillColor(Color.GREEN)
    addOverlay(circleOptions)
}

fun BaiduMap.addRoutePolyline(route: List<Location>) {
    val velocityList = (1 until route.size).map { route[it].velocityTo(route[it - 1]) }
    val maxV = 5f
    val midV = 2f
    val colorList = velocityList.map { it.coerceIn(0f, maxV) }.map { v ->
        if (v < midV) {
            val r = v / midV
            Color.rgb((r * 255).roundToInt(), 0, ((1 - r) * 255).roundToInt())
        } else {
            val r = (v - midV) / (maxV - midV)
            Color.rgb(255, (r * 255).roundToInt(), 0)
        }
    }
    val latLngList = route.map { it.toLatLng() }
    val polylineOptions = PolylineOptions()
        .points(latLngList)
        .colorsValues(colorList)
        .width(10)
    addOverlay(polylineOptions)
}


fun MapView.zoomToViewRoute(route: List<Location>, maxZoom: Float = 18f, minZoom: Float = 4f) {
    val (distanceNS, distanceEW, pointCenter) = route.zone()
    val zoomNS = meterPerDpToZoom(distanceNS.toDouble() * 1.2 / pxToDp(context, height))
    val zoomEW = meterPerDpToZoom(distanceEW.toDouble() * 1.2 / pxToDp(context, width))
    val zoom = min(zoomNS, zoomEW).coerceIn(minZoom, maxZoom)
    map.setMapStatus(MapStatusUpdateFactory.newLatLngZoom(pointCenter.toLatLng(), zoom))
}

fun zoomToMeterPerDp(zoom: Float): Double {
    val a = 29.0 / 4.0
    val b = -5.0 / 16.0
    return (10.0).pow(b * zoom.toDouble() + a) / 25.0
}

fun meterPerDpToZoom(meterPerDp: Double): Float {
    val a = 29.0 / 4.0
    val b = -5.0 / 16.0
    return ((log10(meterPerDp * 25) - a) / b).toFloat()
}

fun dpToPx(context: Context, px: Float): Int {
    val d = context.resources.displayMetrics.density
    return (px * d).roundToInt()
}

fun pxToDp(context: Context, dp: Int): Float {
    val d = context.resources.displayMetrics.density
    return dp.toFloat() / d
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

fun Timer.schedule(period: Long, delay: Long = 0L, block: () -> Unit): TimerTask {
    val timerTask = object : TimerTask() {
        override fun run() = block()
    }
    schedule(timerTask, delay, period)
    return timerTask
}
