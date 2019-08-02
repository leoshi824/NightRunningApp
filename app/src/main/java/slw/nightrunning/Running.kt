package slw.nightrunning

import android.location.Location

enum class RunningState {
    Ready, InProcess, Stopped
}

class Running {

    var stateListener: ((RunningState) -> Unit)? = null
        set(newStateListener) {
            newStateListener?.invoke(state)
            field = newStateListener
        }

    var state: RunningState = RunningState.Ready
        set(newState) {
            if (newState == RunningState.Ready) {
                route.clear()
                startStepCount = -1
                stopStepCount = -1
            }
            stateListener?.invoke(newState)
            field = newState
        }


    val route: ArrayList<Location> = arrayListOf()


    var startStepCount: Int = -1
    var stopStepCount: Int = -1
    val stepCount get() = (stopStepCount - startStepCount)


    fun toLog(): RunningLog = RunningLog(stepCount, route)

}