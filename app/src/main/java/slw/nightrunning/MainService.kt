package slw.nightrunning

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_STEP_COUNTER
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.SENSOR_DELAY_NORMAL
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.os.Binder
import android.os.Bundle
import android.support.v4.content.ContextCompat.checkSelfPermission

class MainService : Service() {

    // lifecycle

    override fun onCreate() {
        super.onCreate()
        val stepCountSensor = sensorManager.getDefaultSensor(TYPE_STEP_COUNTER)
        sensorManager.registerListener(stepCountListener, stepCountSensor, SENSOR_DELAY_NORMAL)
        if (checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(GPS_PROVIDER, 1000, 5f, locationListener)
            locationListener.onLocationChanged(locationManager.getLastKnownLocation(GPS_PROVIDER))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(stepCountListener)
        locationManager.removeUpdates(locationListener)
    }


    // binder

    val binder = MainServiceBinder()

    override fun onBind(intent: Intent): MainServiceBinder = binder


    // stepCount

    private val sensorManager by lazy { getSystemService(Context.SENSOR_SERVICE) as SensorManager }

    private val stepCountListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        override fun onSensorChanged(event: SensorEvent) {
            val nowStepCount = event.values[0].toInt()
            binder.nowStepCount = nowStepCount
        }
    }


    // location

    private val locationManager by lazy { getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    private val locationListener = object : LocationListener {
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String?) {}
        override fun onProviderDisabled(provider: String?) {}
        override fun onLocationChanged(location: Location) {
            binder.nowLocation = location
        }
    }


}

class MainServiceBinder : Binder() {

    var isRunning = false
        private set(value) {
            field = value
            if (!value) {
                runningStepCount = 0
                runningRoute = arrayListOf()
            }
            onStateUpdated?.invoke(value)
        }
    var onStateUpdated: ((Boolean) -> Unit)? = null
        set(value) {
            field = value
            value?.invoke(isRunning)
        }

    fun startRunning() {
        isRunning = true
    }

    fun stopRunning(): RunningLog {
        val log = RunningLog(runningStepCount, runningRoute)
        isRunning = false
        return log
    }


    var nowStepCount: Int = -1
        set(value) {
            if (isRunning && field != -1) runningStepCount += value - field
            field = value
            onStepCountUpdated?.invoke(value)
        }
    var onStepCountUpdated: ((Int) -> Unit)? = null
        set(value) {
            field = value
            value?.invoke(nowStepCount)
        }


    var nowLocation: Location? = null
        set(value) {
            if (isRunning && value != null) runningRoute.add(value)
            field = value
            onLocationUpdated?.invoke(value)
        }
    var onLocationUpdated: ((Location?) -> Unit)? = null
        set(value) {
            field = value
            value?.invoke(nowLocation)
        }


    var runningStepCount = 0
        private set

    var runningRoute = ArrayList<Location>()
        private set

}