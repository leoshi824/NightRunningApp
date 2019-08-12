package slw.nightrunning

import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_log.*

class LogActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FILENAME = "filename"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)
        mapView.onCreate(this, savedInstanceState)

        val filename = intent.getStringExtra(EXTRA_FILENAME) ?: run {
            finish()
            return
        }
        val runningLog = loadRunningLog(filename) ?: run {
            finish()
            return
        }

        mapView.map.apply {
            uiSettings.isOverlookingGesturesEnabled = false
            val route = runningLog.route
            addRoutePolyline(route)
            addStartPoint(route.first().toLatLng())
            addEndPoint(route.last().toLatLng())
        }
        mapView.post { mapView.zoomToViewRoute(runningLog.route) }

        titleText.text = parseRunningLogFilename(filename).timePeriodDescription()
        infoText.text = runningLog.run {
            val timeString = runningLog.timeSpan.timeSpanDescription()
            val averageSpeed = runningLog.route.averageSpeed
            getString(R.string.info_log, timeString, stepCount, route.geoLength, averageSpeed)
        }

        deleteButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_confirm))
                .setMessage(getString(R.string.are_you_sure_to_delete_this_log))
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    deleteRunningLog(filename)
                    finish()
                }
                .setNegativeButton(getString(R.string.no), null)
                .show()
        }

        backButton.setOnClickListener {
            finish()
        }

    }

    override fun onStart() {
        super.onStart()
        setBaiduMapStyleByUiMode()
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

    override fun onStop() {
        super.onStop()
        resetBaiduMapStyle()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }


}
