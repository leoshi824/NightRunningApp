package slw.nightrunning

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_log_list.*

class LogListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_list)
    }

    override fun onStart() {
        super.onStart()
        val dir = getDir("runningLogs", Context.MODE_PRIVATE)
        val filenames = dir.listFiles().reversed().map { it.name }
        val fileDescription = filenames.map { it.parseAsRunningLogFilename().timeSpanDescription() }
        logListView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, fileDescription)
        logListView.setOnItemClickListener { _, _, position, _ ->
            val filename = filenames[position]
            val intent = Intent(this, LogActivity::class.java)
            intent.putExtra("filename", filename)
            startActivity(intent)
        }
    }

}
