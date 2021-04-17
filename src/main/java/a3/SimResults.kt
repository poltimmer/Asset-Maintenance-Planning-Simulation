package a3

import kotlin.math.pow

open class SimResults(private val simDuration: Double, startTime: Double) {
    private var cost = 0.0
    val costAvg get() = cost / simDuration

    private var responseTimeSum = 0.0
    private var responseTimeSumOfSquares = 0.0
    private var responseTimeCount = 0
    val responseTimeMean get() = responseTimeSum / responseTimeCount
    val responseTimeVar get() = responseTimeSumOfSquares / responseTimeCount - responseTimeMean.pow(2)

    private var lastUpdateTime = startTime
    private var lastReportStatus = -1
    private var operationalSum = 0.0
    val operationalRatio get() = operationalSum / simDuration


    fun reportCost(cost: Double) {
        this.cost += cost
    }

    fun reportMachineOnline(currentTime: Double) {
        if (lastReportStatus >= 0) {
            operationalSum += lastReportStatus * (currentTime - lastUpdateTime)
        }
        lastReportStatus = 1
        lastUpdateTime = currentTime
    }

    fun reportMachineOffline(currentTime: Double) {
        operationalSum += if (lastReportStatus < 0) {
            1 * (currentTime - lastUpdateTime)
        } else {
            lastReportStatus * (currentTime - lastUpdateTime)
        }
        lastReportStatus = 0
        lastUpdateTime = currentTime
    }

    open fun reportResponseTime(responseTime: Double) {
        responseTimeSum += responseTime
        responseTimeSumOfSquares += responseTime.pow(2)
        responseTimeCount++
    }
}
