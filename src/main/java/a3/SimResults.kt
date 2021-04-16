package a3

import kotlin.math.pow

class SimResults(private val simDuration: Double, startTime: Double) {
    private var cost = 0.0
    val costAvg get() = cost / simDuration

    private var responseTimeSum = 0.0
    private var responseTimeSumOfSquares = 0.0
    private var responseTimeCount = 0
    val responseTimeMean get() = responseTimeSum / responseTimeCount
    val responseTimeVar get() = responseTimeSumOfSquares / responseTimeCount - responseTimeMean.pow(2)

    private var lastUpdateTime = startTime
    private var operationalSum = 0.0
    val operationalRatio get() = operationalSum / simDuration


    fun reportCost(cost: Double) {
        this.cost += cost
    }

    fun reportMachineOnline(currentTime: Double) { // todo: maybe make this more robust. Doesn't work in case of double calls
        // operationalSum += 0 * (currentTime - lastUpdateTime)
        lastUpdateTime = currentTime
    }

    fun reportMachineOffline(currentTime: Double) {
        operationalSum += 1 * (currentTime - lastUpdateTime)
        lastUpdateTime = currentTime
    }

    fun reportResponseTime(responseTime: Double) {
        responseTimeSum += responseTime
        responseTimeSumOfSquares += responseTime.pow(2)
        responseTimeCount++
    }
}
