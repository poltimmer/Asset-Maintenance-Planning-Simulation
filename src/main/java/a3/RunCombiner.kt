package a3

import kotlin.math.pow
import kotlin.math.sqrt

class RunCombiner(
    private val simulator: Simulator,
    private val numRuns: Int,
    private val runLength: Double,
    private val warmupTime: Double
) {
    private val operationalRatio: Array<DoubleArray>
    private val responseTimeMean: Array<DoubleArray>
    private val responseTimeVar: Array<DoubleArray>
    private val costMean: Array<DoubleArray>
    private val costSumMean: Array<DoubleArray>

    init {
        val resultsList = ArrayList<Map<Machine, SimResults>>()
        // warm-up
        simulator.simulate(warmupTime)

        for (i in 0 until numRuns) {
            resultsList.add(simulator.simulate(runLength))
        }

        val numMachines: Int = resultsList[0].size

        // Combine the results for each statistic

        // Combine the results for each statistic
        operationalRatio = Array(numMachines) { DoubleArray(numRuns) }
        responseTimeMean = Array(numMachines) { DoubleArray(numRuns) }
        responseTimeVar = Array(numMachines) { DoubleArray(numRuns) }
        costMean = Array(numMachines) { DoubleArray(numRuns) }
        costSumMean = Array(numMachines) { DoubleArray(numRuns) }
        for ((run, resultsMap) in resultsList.withIndex()) {
            for ((machine, result) in resultsMap) {
//                val result: SimResults? = resultsList[run].get(machine)
                operationalRatio[machine.id][run] = result.operationalRatio
                responseTimeMean[machine.id][run] = result.responseTimeMean
                responseTimeVar[machine.id][run] = result.responseTimeVar
                costMean[machine.id][run] = result.costAvg
                costSumMean[machine.id][run] = result.costAvg
            }
        }
    }

    fun printLatexTables() {
        val stats = arrayOf(operationalRatio, responseTimeMean, responseTimeVar, costMean, costSumMean)
        val numMachines: Int = operationalRatio.size
        println("== LaTeX Experimental results table")
        for (id in 0 until numMachines) {
            val values: MutableList<String> = ArrayList()
            for (stat in stats) {
                val mean: Double = getMeanFromDoubles(stat[id])
                values.add(String.format("%.3f", mean))
            }
            println((id + 1).toString() + " & " + java.lang.String.join(" & ", values) + " \\\\")
        }
        println()
        println("== LaTeX CI table")
        val labels = arrayOf("\\rho_m", "\\mathds{E}[\\tau_m]", "\\mathds{V}[\\tau_m]", "g_m(\\pi)", "g(\\pi)")
        for (statId in stats.indices) {
            val label = labels[statId]
            val stat = stats[statId]
            println("\\hline \\multirow{2}{*}{$${label}$}")
            val lowers: MutableList<String> = ArrayList()
            val uppers: MutableList<String> = ArrayList()
            for (station in 0 until numMachines) {
                val mean: Double = getMeanFromDoubles(stat[station])
                val error: Double = getErrorFromDoubles(stat[station])
                lowers.add(String.format("%.3f", mean - error))
                uppers.add(String.format("%.3f", mean + error))
            }
            println("\t& lower & " + java.lang.String.join(" & ", lowers) + " \\\\")
            println("\t& upper & " + java.lang.String.join(" & ", uppers) + " \\\\")
        }
    }

    /**
     * Get the mean value from a set of results
     * @param arr Results
     * @return Mean
     */
    fun getMeanFromDoubles(arr: DoubleArray): Double {
        return arr.average()
    }

    /**
     * Get the variance of a set of results
     * @param arr Results
     * @return Variance
     */
    fun getVarianceFromDoubles(arr: DoubleArray): Double {
        return arr.map { it * it }.average() - getMeanFromDoubles(arr).pow(2)
    }

    /**
     * Compute the 95% confidence error on a set of results
     * @param arr Results
     * @return Error
     */
    fun getErrorFromDoubles(arr: DoubleArray): Double {
        return 1.96 * sqrt(getVarianceFromDoubles(arr) / arr.size)
    }
}
