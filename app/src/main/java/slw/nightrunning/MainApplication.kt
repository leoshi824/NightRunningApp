package slw.nightrunning

import android.app.Application
import com.baidu.mapapi.SDKInitializer

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SDKInitializer.initialize(applicationContext)
    }
}