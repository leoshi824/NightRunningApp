package slw.nightrunning

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import slw.nightrunning.RecordState.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val record = Record()
        record.stateListener = { state ->
            when (state) {
                Ready -> {
                    controlButton.text = "Start"
                    controlButton.setOnClickListener {
                        record.state = InProcess
                    }
                }
                InProcess -> {
                    controlButton.text = "Stop"
                    controlButton.setOnClickListener {
                        record.state = Stopped
                    }
                }
                Stopped -> {
                    controlButton.text = "Reset"
                    controlButton.setOnClickListener {
                        record.state = Ready
                    }
                }
            }
        }

    }

}
