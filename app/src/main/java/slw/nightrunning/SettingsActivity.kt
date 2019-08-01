package slw.nightrunning

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var settingsPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        settingsPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    override fun onStart() {
        super.onStart()
        emergencyContactNumberField.setText(settingsPreferences.getString("emergencyContactNumber", ""))
    }

    override fun onStop() {
        super.onStop()
        val editor = settingsPreferences.edit()
        editor.putString("emergencyContactNumber", emergencyContactNumberField.text.toString())
        editor.apply()
    }
}
