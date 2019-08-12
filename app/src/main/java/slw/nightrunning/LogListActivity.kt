package slw.nightrunning

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_log_list.*
import slw.nightrunning.LogActivity.Companion.EXTRA_FILENAME

class LogListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_list)
    }

    override fun onStart() {
        super.onStart()
        val files = listRunningLogs()
        if (files.isNotEmpty()) {
            logListView.visibility = View.VISIBLE
            emptyListLabel.visibility = View.GONE
            val filenames = files.reversed().map { it.name }
            val fileDescription = filenames.map { parseRunningLogFilename(it).timePeriodDescription() }
            logListView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, fileDescription)
            logListView.setOnItemClickListener { _, _, position, _ ->
                val filename = filenames[position]
                val intent = Intent(this, LogActivity::class.java)
                intent.putExtra(EXTRA_FILENAME, filename)
                startActivity(intent)
            }
        } else {
            logListView.visibility = View.GONE
            emptyListLabel.visibility = View.VISIBLE
        }
    }

}
