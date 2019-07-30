package slw.nightrunning

enum class RecordState {
    Ready, InProcess, Stopped
}

class Record {

    var stateListener: (RecordState) -> Unit = {}
        set(newStateListener) {
            newStateListener.invoke(state)
            field = newStateListener
        }

    var state: RecordState = RecordState.Ready
        set(newState) {
            stateListener.invoke(newState)
            field = newState
        }

}