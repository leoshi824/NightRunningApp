package slw.nightrunning

import android.app.UiModeManager
import android.app.UiModeManager.MODE_NIGHT_NO
import android.app.UiModeManager.MODE_NIGHT_YES
import android.content.Context
import android.content.SharedPreferences

var Context.nightModeOn: Boolean
    get() {
        val uiManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return when (uiManager.nightMode) {
            MODE_NIGHT_YES -> true
            MODE_NIGHT_NO -> false
            else -> {
                uiManager.nightMode = MODE_NIGHT_NO
                false
            }
        }
    }
    set(value) {
        val uiManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        uiManager.nightMode = if (value) MODE_NIGHT_YES else MODE_NIGHT_NO
    }

fun Context.getSettingsPreferences(): SharedPreferences {
    return getSharedPreferences("settings", Context.MODE_PRIVATE)
}

fun SharedPreferences.edit(block: SharedPreferences.Editor.() -> Unit) {
    val editor = edit()
    editor.block()
    editor.apply()
}

var SharedPreferences.emergencyContactEnabled: Boolean
    get() = getBoolean("emergencyContactEnabled", false)
    set(value) = edit { putBoolean("emergencyContactEnabled", value) }

var SharedPreferences.emergencyPhoneNumber: String
    get() = getString("emergencyPhoneNumber", null) ?: ""
    set(value) = edit { putString("emergencyPhoneNumber", value) }

var SharedPreferences.emergencyMessage: String
    get() = getString("emergencyMessage", null) ?: ""
    set(value) = edit { putString("emergencyMessage", value) }