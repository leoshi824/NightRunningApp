package slw.nightrunning

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.PendingIntent.getActivity
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
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat.checkSelfPermission

class MainService : Service() {

    companion object {
        const val NotificationChannelId_running = "slw.nightrunning:running"
    }

    // lifecycle

    override fun onCreate() {
        super.onCreate()
        val stepCountSensor = sensorManager.getDefaultSensor(TYPE_STEP_COUNTER)
        sensorManager.registerListener(stepCountListener, stepCountSensor, SENSOR_DELAY_NORMAL)
        if (checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(GPS_PROVIDER, 1000, 5f, locationListener)
            locationManager.getLastKnownLocation(GPS_PROVIDER)?.let { locationListener.onLocationChanged(it) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(stepCountListener)
        locationManager.removeUpdates(locationListener)
        if (binder.isRunning) {
            val log = binder.stopRunning()
            if (log.route.size >= 2) saveRunningLog(log)
        }
    }


    // binder

    val binder = object : MainServiceBinder() {
        override fun showNotification() = this@MainService.showNotification()
        override fun hideNotification() = this@MainService.hideNotification()
    }

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


    // notification

    private var notificationShowing = false

    private fun showNotification() {
        if (notificationShowing) return

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        val pendingIntent = getActivity(this, 1, intent, FLAG_UPDATE_CURRENT)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(NotificationChannelId_running, getString(R.string.running), IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
            Notification.Builder(this, NotificationChannelId_running)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val notification = builder.run {
            setSmallIcon(R.drawable.ic_notification_running)
            setContentTitle(getString(R.string.notification_title_running))
            setContentText(getString(R.string.notification_message_click_to_view))
            setContentIntent(pendingIntent)
            build()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            startForeground(STOP_FOREGROUND_REMOVE, notification)
        } else {
            startForeground(1, notification)
        }
        notificationShowing = true
    }

    private fun hideNotification() {
        if (!notificationShowing) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        notificationShowing = false
    }

}

abstract class MainServiceBinder : Binder() {

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

    fun startRunning(nowTime: Long = System.currentTimeMillis()) {
        startTime = nowTime
        isRunning = true
    }

    fun stopRunning(stopTime: Long = System.currentTimeMillis()): RunningLog {
        val log = RunningLog(startTime, stopTime, runningStepCount, runningRoute)
        startTime = -1L
        isRunning = false
        return log
    }


    var startTime: Long = -1L


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


    abstract fun showNotification()

    abstract fun hideNotification()

}
