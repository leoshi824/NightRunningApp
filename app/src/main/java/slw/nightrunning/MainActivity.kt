package slw.nightrunning

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.PendingIntent
import android.content.ComponentName
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
        updateEmergencyButton()
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

    override fun onStop() {
        super.onStop()
        stopService()
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }


    // service & binder

    var binder: MainServiceBinder? = null

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            service as MainServiceBinder
            this@MainActivity.binder = service
            service.apply {
                onStateUpdated = {
                    updateControlButton()
                    updateInfoText()
                }
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

    private fun updateControlButton() {
        val isRunning = binder?.isRunning ?: false
        if (!isRunning) {
            titleTextView.text = getString(R.string.lets_start_running)
            controlButton.text = getString(R.string.start)
            controlButton.setOnClickListener {
                binder?.startRunning()
            }
        } else {
            titleTextView.text = getString(R.string.you_are_running)
            controlButton.text = getString(R.string.stop)
            controlButton.setOnClickListener {
                saveRunningLogWithCheck()
                binder?.stopRunning()
            }
        }
    }

    private fun updateInfoText() {
        binder?.run {
            var string = ""
            nowLocation?.run {
                string += "latitude=$latitude\n" +
                        "longitude=$longitude\n" +
                        "altitude=$altitude\n"
            }
            if (isRunning) {
                string += "nowStepCount=$runningStepCount\n" +
                        "routeLength=${runningRoute.geoLength}"
            }
            infoTextView.text = string
        }
    }

    private fun updateMapView() {
        binder?.run {
            nowLocation?.toLatLng()?.let { latLng ->
                mapView.map.setMapStatus(MapStatusUpdateFactory.newLatLngZoom(latLng, 18f))
                mapView.map.addLocation(latLng)
            }
            mapView.map.clear()
            runningRoute.takeIf { it.size >= 2 }?.let { route ->
                mapView.map.addRoute(route.map(Location::toLatLng))
            }
        }
    }

    private fun updateEmergencyButton() = getSettingsPreferences().run {
        if (!emergencyContactEnabled) {
            emergencyButton.visibility = View.GONE
        } else {
            emergencyButton.visibility = View.VISIBLE
            emergencyButton.setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.long_click_to_make_emergency_call),
                    Toast.LENGTH_LONG
                ).show()
            }

            val phoneNumber = emergencyPhoneNumber
            var message = emergencyMessage
            emergencyButton.setOnLongClickListener {
                binder?.nowLocation?.let { location -> message += "\n" + location.toString() }
                makeEmergencyCall(phoneNumber, message)
                false
            }
        }
    }

    // other actions

    private fun saveRunningLogWithCheck(): Boolean {
        val log = binder?.stopRunning() ?: return false
        if (log.route.size < 2) {
            Toast.makeText(this, getString(R.string.got_few_data), Toast.LENGTH_LONG)
                .show()
            return false
        }
        val filename = saveRunningLog(log) ?: return false
        val intent = Intent(this, LogActivity::class.java)
        intent.putExtra("filename", filename)
        startActivity(intent)
        return true
    }

    private fun makeEmergencyCall(number: String, message: String) {
        val smsIntent = Intent("com.android.TinySMS.RESULT")
        val pendingIntent = PendingIntent.getBroadcast(this, 0, smsIntent, PendingIntent.FLAG_ONE_SHOT)
        SmsManager.getDefault().sendTextMessage(number, null, message, pendingIntent, null)

        val phoneIntent = Intent(Intent.ACTION_CALL)
        phoneIntent.data = Uri.parse("tel:$number")
        startActivity(phoneIntent)
    }

}
