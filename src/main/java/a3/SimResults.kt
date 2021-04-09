package a3

class SimResults(private val simDuration: Double, private val startTime: Double) {
    private var cost = 0.0
    val costAvg get() = cost/simDuration

    private var responseTimeSum = 0.0
    private var responseTimeCount = 0
    val responseTime get() = responseTimeSum/responseTimeCount

    private var lastUpdateTime = startTime
    private var operationalSum = 0.0
    val operationalRatio get() = operationalSum/simDuration



    fun reportCost(cost: Double) {
        this.cost += cost
    }

    fun reportMachineRepaired(currentTime: Double) {
        // operationalSum += 0 * (currentTime - lastUpdateTime)
        lastUpdateTime = currentTime
    }

    fun reportMachineFailed(currentTime: Double) {
        operationalSum += 1 * (currentTime - lastUpdateTime)
        lastUpdateTime = currentTime
    }

    fun reportResponseTime(responseTime: Double) {
        responseTimeSum += responseTime
        responseTimeCount++
    }
}
