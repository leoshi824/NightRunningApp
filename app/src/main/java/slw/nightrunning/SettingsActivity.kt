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
        emergencyContactEnabledCheckBox.setOnCheckedChangeListener { _, isChecked ->
            emergencyContactNumberField.isEnabled = isChecked
            emergencyContactMessageField.isEnabled = isChecked
            if (isChecked) {
                // todo request permissions
            }
        }
    }

    override fun onResume() {
        super.onResume()
        emergencyContactEnabledCheckBox.isChecked = settingsPreferences.getBoolean("emergencyContactEnabled", false)
        emergencyContactNumberField.setText(settingsPreferences.getString("emergencyContactNumber", ""))
        val defaultMessage = getString(R.string.default_emergency_contact_message)
        emergencyContactMessageField.setText(settingsPreferences.getString("emergencyContactMessage", defaultMessage))
    }

    override fun onStop() {
        super.onStop()
        val editor = settingsPreferences.edit()
        editor.putBoolean("emergencyContactEnabled", emergencyContactEnabledCheckBox.isChecked)
        editor.putString("emergencyContactNumber", emergencyContactNumberField.text.toString())
        editor.putString("emergencyContactMessage", emergencyContactMessageField.text.toString())
        editor.apply()
    }

}
