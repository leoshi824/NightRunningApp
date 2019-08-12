package slw.nightrunning

import android.Manifest.permission.CALL_PHONE
import android.Manifest.permission.SEND_SMS
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.support.v4.app.ActivityCompat.requestPermissions
import android.support.v4.content.ContextCompat.checkSelfPermission
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_settings.*


class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBaiduMapStyle()
        setContentView(R.layout.activity_settings)
        nightModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            nightModeOn = isChecked
        }
        emergencyContactEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (checkSelfPermission(this, SEND_SMS) == PERMISSION_GRANTED
                    && checkSelfPermission(this, CALL_PHONE) == PERMISSION_GRANTED
                ) {
                    emergencyPhoneNumberField.isEnabled = true
                    emergencyContactMessageField.isEnabled = true
                } else {
                    emergencyContactEnabledSwitch.isChecked = false
                    requestPermissions(this, arrayOf(SEND_SMS, CALL_PHONE), 0)
                }
            } else {
                emergencyPhoneNumberField.isEnabled = false
                emergencyContactMessageField.isEnabled = false
            }
        }
    }

    override fun onStart() {
        super.onStart()
        loadSettingsPreferences()
    }

    override fun onPause() {
        super.onPause()
        saveSettingsPreferences()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            if (checkSelfPermission(this, SEND_SMS) == PERMISSION_GRANTED
                && checkSelfPermission(this, CALL_PHONE) == PERMISSION_GRANTED
            ) emergencyContactEnabledSwitch.isChecked = true
        }
    }


    private fun loadSettingsPreferences() = getSettingsPreferences().run {
        nightModeSwitch.isChecked = nightModeOn
        emergencyContactEnabledSwitch.isChecked = emergencyContactEnabled
        emergencyPhoneNumberField.setText(emergencyPhoneNumber)
        emergencyContactMessageField.setText(emergencyMessage.takeIf { it.isNotBlank() }
            ?: getString(R.string.default_emergency_contact_message))
    }

    private fun saveSettingsPreferences() = getSettingsPreferences().run {
        emergencyContactEnabled = emergencyContactEnabledSwitch.isChecked
        emergencyPhoneNumber = emergencyPhoneNumberField.text.toString()
        emergencyMessage = emergencyContactMessageField.text.toString()
    }

}
