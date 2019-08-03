package slw.nightrunning

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.PersistableBundle
import android.support.v4.app.ActivityCompat.requestPermissions
import android.support.v4.content.ContextCompat.checkSelfPermission
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.telephony.SmsManager
import android.view.View
import android.widget.Toast
import com.baidu.mapapi.map.MapStatusUpdateFactory
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {


    // lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapView.onCreate(this, savedInstanceState)

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        logListButton.setOnClickListener {
            startActivity(Intent(this, LogListActivity::class.java))
        }

    }

    override fun onStart() {
        super.onStart()

        val settingsPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (settingsPreferences.getBoolean("emergencyContactEnabled", false)) {
            emergencyButton.visibility = View.VISIBLE
            emergencyButton.setOnClickListener {
                Toast.makeText(this, "Long click to make the emergency call!", Toast.LENGTH_LONG).show()
            }
            emergencyButton.setOnLongClickListener {
                val number = settingsPreferences.getString("emergencyContactNumber", "") ?: ""
                val defaultMessage = getString(R.string.default_emergency_contact_message)
                var message = settingsPreferences.getString("emergencyContactMessage", defaultMessage) ?: ""
                binder?.nowLocation?.let { location -> message += "\n" + location.toString() }
                emergencyCall(number, message)
                false
            }
        } else {
            emergencyButton.visibility = View.GONE
        }



        startServiceWithPermissionRequest()
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
        stopService()
    }


    // service & binder

    var binder: MainServiceBinder? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder = (service as MainServiceBinder).apply {
                onStateUpdated = this@MainActivity::updateButton
                onStepCountUpdated = {
                    updateInfoText()
                }
                onLocationUpdated = {
                    updateMapView()
                    updateInfoText()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder?.run {
                onStateUpdated = null
                onStepCountUpdated = null
                onLocationUpdated = null
            }
            binder = null
        }
    }

    private fun startService(): Boolean {
        if (checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
            startService(Intent(this, MainService::class.java))
            bindService(Intent(this, MainService::class.java), serviceConnection, 0)
            return true
        }
        return false
    }

    private fun stopService() {
        val isRunning = binder?.isRunning ?: false
        unbindService(serviceConnection)
        if (!isRunning) stopService(Intent(this, MainService::class.java))
    }

    private fun startServiceWithPermissionRequest(): Boolean {
        val succeed = startService()
        if (!succeed) requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION), 0)
        return succeed
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            if (!startService()) {
                AlertDialog.Builder(this)
                    .setTitle("Location not accessible!")
                    .setMessage("Not granted permission to access nowLocation! This app will not work properly!")
                    .setPositiveButton("All right!") { _, _ -> startServiceWithPermissionRequest() }
                    .setNegativeButton("No way!") { _, _ -> finish() }
                    .show()
            }
        }
    }


    // views

    private fun updateButton(isRunning: Boolean) {
        if (!isRunning) {
            controlButton.text = "Start"
            controlButton.setOnClickListener {
                binder?.startRunning()
            }
        } else {
            controlButton.text = "Stop"
            controlButton.setOnClickListener {
                saveRunningLogWithCheck()
                binder?.stopRunning()
            }
        }
    }

    private fun updateInfoText() {
        binder?.run {
            if (isRunning) {
                titleTextView.text = "You are running"
            } else {
                titleTextView.text = "Let's start running!"
            }
            infoTextView.text = "" +
                    "nowStepCount=$runningStepCount\n" +
                    "routeLength=${runningRoute.geoLength}"
        }
    }

    private fun updateMapView() {
        binder?.run {
            nowLocation?.let { location ->
                val latLng = location.toLatLng()
                mapView.map.setMapStatus(MapStatusUpdateFactory.newLatLngZoom(latLng, 18f))
                mapView.map.addLocation(latLng)
            }
            mapView.map.clear()
            runningRoute.takeIf { it.size >= 2 }?.let { route ->
                mapView.map.addRoute(route.map(Location::toLatLng))
            }
        }
    }


    // other actions

    private fun saveRunningLogWithCheck(): Boolean {
        val log = binder?.stopRunning() ?: return false
        if (log.route.size < 2) {
            Toast.makeText(this, "Got too few data. This log will not be saved.", Toast.LENGTH_LONG)
                .show()
            return false
        }
        val filename = saveRunningLog(log) ?: return false
        val intent = Intent(this, LogActivity::class.java)
        intent.putExtra("filename", filename)
        startActivity(intent)
        return true
    }

    private fun emergencyCall(number: String, message: String) {
        val smsIntent = Intent("com.android.TinySMS.RESULT")
        val pendingIntent = PendingIntent.getBroadcast(this, 0, smsIntent, PendingIntent.FLAG_ONE_SHOT)
        SmsManager.getDefault().sendTextMessage(number, null, message, pendingIntent, null)

        val phoneIntent = Intent(Intent.ACTION_CALL)
        phoneIntent.data = Uri.parse("tel:$number")
        startActivity(phoneIntent)
    }

}
