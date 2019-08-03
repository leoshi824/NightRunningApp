package slw.nightrunning

import android.graphics.Color
import android.location.Location
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.CircleOptions
import com.baidu.mapapi.map.PolylineOptions
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.utils.CoordinateConverter
import com.baidu.mapapi.utils.CoordinateConverter.CoordType.GPS

val List<Location>.geoLength: Double
    get() {
        var length = 0.0
        for (i in (0 until size - 1)) {
            length += this[i].distanceTo(this[i + 1])
        }
        return length
    }


fun Location.toLatLng(): LatLng {
    return CoordinateConverter()
        .from(GPS)
        .coord(LatLng(latitude, longitude))
        .convert()
}

fun BaiduMap.addLocation(latLng: LatLng) {
    val circleOptions = CircleOptions()
        .center(latLng)
        .radius(10)
        .fillColor(Color.RED)
    addOverlay(circleOptions)
}

fun BaiduMap.addRoute(route: List<LatLng>) {
    val polylineOptions = PolylineOptions()
        .points(route)
        .color(Color.BLUE)
        .width(7)
    addOverlay(polylineOptions)
}

