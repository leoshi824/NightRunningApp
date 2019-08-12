package slw.nightrunning

import java.util.*


// time description

fun Pair<Long, Long>.timePeriodDescription(): String {

    val start = Calendar.getInstance().apply { timeInMillis = first }
    val stop = Calendar.getInstance().apply { timeInMillis = second }

    val sb = StringBuilder()
    sb.append(start.get(Calendar.YEAR), "-")
    sb.append(start.get(Calendar.MONTH), "-")
    sb.append(start.get(Calendar.DAY_OF_MONTH), " ")
    sb.append(start.get(Calendar.HOUR_OF_DAY).let { "%02d".format(it) }, ":")
    sb.append(start.get(Calendar.MINUTE).let { "%02d".format(it) })

    sb.append(" ~ ")
    var skipSame = true

    skipSame = skipSame && start.get(Calendar.YEAR) == stop.get(Calendar.YEAR)
    if (!skipSame) sb.append(sb.append(stop.get(Calendar.YEAR), "-"))

    skipSame = skipSame
            && start.get(Calendar.MONTH) == stop.get(Calendar.MONTH)
            && start.get(Calendar.DAY_OF_MONTH) == stop.get(Calendar.DAY_OF_MONTH)
    if (!skipSame) {
        sb.append(sb.append(stop.get(Calendar.MONTH), "-"))
        sb.append(sb.append(stop.get(Calendar.DAY_OF_MONTH), " "))
    }

    sb.append(stop.get(Calendar.HOUR_OF_DAY).let { "%02d".format(it) }, ":")
    sb.append(stop.get(Calendar.MINUTE).let { "%02d".format(it) })

    return sb.toString()
}

fun Long.timeSpanDescription(): String {
    var time = this

//    val microSecond = time % 1000
    time /= 1000

    val second = time % 60
    time /= 60

    val minute = time % 60
    time /= 60

    val hour = time % 24
    time /= 24

//    val day = time

    return if (hour == 0L) {
        "%02d:%02d".format(minute, second)
    } else {
        "%d:%02d:%02d".format(hour, minute, second)
    }
}


// timer

fun Timer.schedule(period: Long, delay: Long = 0L, block: () -> Unit): TimerTask {
    val timerTask = object : TimerTask() {
        override fun run() = block()
    }
    schedule(timerTask, delay, period)
    return timerTask
}