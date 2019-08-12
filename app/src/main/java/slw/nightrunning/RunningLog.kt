package slw.nightrunning

import android.location.Location


data class RunningLog(
    val startTime: Long, val stopTime: Long,
    val stepCount: Int, val route: List<Location>
)

val RunningLog.timeSpan get() = stopTime - startTime
