package slw.nightrunning

import android.location.Location
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import com.baidu.mapapi.map.MapStatusUpdateFactory
import kotlinx.android.synthetic.main.activity_log.*

class LogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)
        mapView.onCreate(this, savedInstanceState)

        val filename = intent.getStringExtra("filename") ?: run {
            finish()
            return
        }
        val runningLog = loadRunningLog(filename) ?: run {
            finish()
            return
        }

        mapView.map.apply {
            val latLngList = runningLog.route.map(Location::toLatLng)
            val latLng = latLngList.last()

            clear()
            setMapStatus(MapStatusUpdateFactory.newLatLngZoom(latLng, 18f))
            addRoute(latLngList)
            addLocation(latLng)
        }

        titleTextView.text = filename
        infoTextView.text = "timeSpan=${runningLog.timeSpan}\n" +
                "nowStepCount=${runningLog.stepCount}\n" +
                "routeLength=${runningLog.route.geoLength}"
        deleteButton.setOnClickListener {
            deleteRunningLog(filename)
            finish()
        }
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


}
