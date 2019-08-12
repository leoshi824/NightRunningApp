package slw.nightrunning

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.location.Location
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.utils.CoordinateConverter
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

// coordinate

fun Location.toLatLng(): LatLng {
    return gpsToBdl(latitude, longitude)
}

fun gpsToBdl(latitude: Double, longitude: Double): LatLng {
    return CoordinateConverter()
        .from(CoordinateConverter.CoordType.GPS)
        .coord(LatLng(latitude, longitude))
        .convert()
}


// overlay

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


// map zoom

fun MapView.zoomToViewRoute(route: List<Location>, maxZoom: Float = 18f, minZoom: Float = 4f) {
    val (distanceNS, distanceEW, pointCenter) = route.zone()
    val zoomNS =
        meterPerDpToZoom(distanceNS.toDouble() * 1.2 / pxToDp(context, height))
    val zoomEW =
        meterPerDpToZoom(distanceEW.toDouble() * 1.2 / pxToDp(context, width))
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


// map style

private var baiduMapStyleUsingCount = 0

fun cacheBaiduMapStyles(context: Context, cover: Boolean = false) {
    val fileNames = context.assets.list("baiduMapStyles") ?: arrayOf<String>()
    val cacheDir = context.cacheDir.resolve("baiduMapStyles").apply {
        if (cover) deleteRecursively()
        mkdirs()
    }
    fileNames.forEach { fileName ->
        val cacheFile = cacheDir.resolve(fileName)
        if (cover || !cacheFile.exists()) {
            val assetsFilePath = "baiduMapStyles/$fileName"
            context.assets.open(assetsFilePath).use { inputStream ->
                cacheFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }
}

fun Activity.initBaiduMapStyle() {
    cacheBaiduMapStyles(this)
    MapView.setCustomMapStylePath(cacheDir.resolve("baiduMapStyles").resolve("dark.json").path)
}

fun Activity.setBaiduMapStyleByUiMode() {
    val nightModeOn = nightModeOn
    MapView.setMapCustomEnable(nightModeOn)
    if (nightModeOn) baiduMapStyleUsingCount++
}

fun resetBaiduMapStyle() {
    if (--baiduMapStyleUsingCount == 0) MapView.setMapCustomEnable(false)
}
