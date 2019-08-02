package slw.nightrunning

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.app.ActivityCompat.requestPermissions
import android.support.v4.content.ContextCompat.checkSelfPermission
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import com.baidu.mapapi.map.CircleOptions
import com.baidu.mapapi.map.MapStatusUpdateFactory
import com.baidu.mapapi.map.PolylineOptions
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.utils.CoordinateConverter
import com.baidu.mapapi.utils.CoordinateConverter.CoordType.GPS
import kotlinx.android.synthetic.main.activity_main.*
import slw.nightrunning.RunningState.*

class MainActivity : AppCompatActivity() {

    private val running = Running()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapView.onCreate(this, savedInstanceState)

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        running.stateListener = { state ->
            when (state) {
                Ready -> {
                    controlButton.text = "Start"
                    controlButton.setOnClickListener {
                        running.state = InProcess
                    }
                }
                InProcess -> {
                    controlButton.text = "Stop"
                    controlButton.setOnClickListener {
                        saveRunningLog()
                        running.state = Stopped
                    }
                }
                Stopped -> {
                    controlButton.text = "Reset"
                    controlButton.setOnClickListener {
                        running.state = Ready
                    }
                }
            }
        }

        requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION), 0)

        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
            override fun onSensorChanged(event: SensorEvent) {
                val nowStepCount = event.values[0].toInt()
                if (running.state == InProcess) {
                    if (running.startStepCount == -1) running.startStepCount = nowStepCount
                    running.stopStepCount = nowStepCount
                    updateInfoText()
                }
            }
        }, sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER), SensorManager.SENSOR_DELAY_NORMAL)

    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            if (checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
                val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                locationManager.requestLocationUpdates(GPS_PROVIDER, 1000, 5f, object : LocationListener {
                    override fun onLocationChanged(location: Location) = this@MainActivity.onLocationChanged(location)
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String?) {}
                    override fun onProviderDisabled(provider: String?) {}
                })
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Location not accessible!")
                    .setMessage("Not granted permission to access location! This app will not work properly!")
                    .setPositiveButton("All right!") { _, _ ->
                        requestPermissions(this, arrayOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION), 0)
                    }
                    .setNegativeButton("No way!") { _, _ -> finish() }
                    .show()
            }
        }
    }

    private fun onLocationChanged(location: Location) {
        if (running.state == InProcess) running.route.add(location)

        updateInfoText()

        val latLng = locationToLatLng(location)
        mapView.map.setMapStatus(MapStatusUpdateFactory.newLatLngZoom(latLng, 18f))

        mapView.map.clear()
        if (running.route.size >= 2) {
            mapView.map.addOverlay(
                PolylineOptions().points(running.route.map { loc -> locationToLatLng(loc) })
                    .color(Color.BLUE).width(7)
            )
        }

        mapView.map.addOverlay(CircleOptions().center(latLng).radius(10).fillColor(Color.RED))
    }

    private fun locationToLatLng(location: Location) = CoordinateConverter().from(GPS)
        .coord(LatLng(location.latitude, location.longitude)).convert()


    private fun updateInfoText() {
        val stepCount = running.stepCount

        var routeLength = 0.0
        for (i in (0 until running.route.size - 1)) {
            routeLength += running.route[i].distanceTo(running.route[i + 1])
        }

        infoTextView.text = "stepCount=$stepCount\nrouteLength=$routeLength"
    }


    private fun saveRunningLog() {
        val runningLog = running.toLog()
        getFileStreamPath(runningLog.filename).outputStream().use { outputStream ->
            outputStream.writeRunningLog(runningLog)
        }
    }

}
