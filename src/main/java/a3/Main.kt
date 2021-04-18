package a3

import org.apache.commons.math3.distribution.BetaDistribution
import org.apache.commons.math3.distribution.ConstantRealDistribution
import org.apache.commons.math3.distribution.ExponentialDistribution
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths


fun main() {
    println("hello world")
    // TODO: investigate costs. Only enable downtime costs.
    // TOD: put results tables into report
    // TOD: write introduction
    // TODO: convert number of runs talk
    // TOD: implement histograms for response time
    // TODO: optionally a short histogram for the warm-up time
    // TOD: input in report
    // TODO: custom policy
    // TODO: results, with graphs
    // TODO: discussion
    // TODO: grammarly, and check for remaining keywords (station, server, etc.)
//    val simulator = getSimulator(Policy.GREEDY, false)
//    for (i in 0 until 4) {
//        val results = simulator.simulate(100000.0)
//        println("_______________________")
//        for ((machine, result) in results.toSortedMap(compareBy { it.id })) {
//            println()
//            println("Machine id: ${machine.id}")
//            println("Average cost: ${result.costAvg}")
//            println("Response time mean: ${result.responseTimeMean}  |  var: ${result.responseTimeVar}")
//            println("Operational ratio: ${result.operationalRatio}")
//        }
//        println("${Counter.count} events fired")
//    }

//    RunCombiner(simulator, 1000, 1000.0, 10000.0).printLatexTables()

    printHists()

//    simulator.simulate(10000.0)
//    exportHistogram("responseTime", simulator.simulate(100000.0) as Map<Machine, SimResultsWithHist>)


}

fun getSimulator(
    policy: Policy,
    loadSharingDegradation: Boolean = false,
    inputFilePath: String = "./input/input.txt",
    matrixFilePath: String = "./input/matrix.txt"
): Simulator {
    /*
    0  thresholds
    1  arrivalLambdas
    2  alphas
    3  betas
    4  preventiveMaintenanceTimes
    5  correctiveMaintenanceTimes
    6  preventiveMaintenanceCosts
    7  correctiveMaintenanceCosts
    8  downTimeCost
     */
    val inputData = File(inputFilePath).readLines().map { line -> line.split("\\s".toRegex()).map { it.toDouble() } }

    val machines = ArrayList<Machine>()

    // Create Machines
    for (i in inputData[0].indices) {
        machines.add(
            Machine(
                id = i,
                threshold = inputData[0][i],
                downTimeCost = inputData[8][i],
                preventiveMaintenanceCost = inputData[6][i],
                correctiveMaintenanceCost = inputData[7][i],
                arrivalDistribution = ExponentialDistribution(1 / inputData[1][i]),
                degradationDistribution = BetaDistribution(inputData[2][i], inputData[3][i]),
                preventiveMaintenanceTimeDistribution = ExponentialDistribution(inputData[4][i]),
                correctiveMaintenanceTimeDistribution = ExponentialDistribution(inputData[5][i])
            )
        )
    }

    // Create a matrix of ConstantRealDistribution objects. This allows usage of a distribution instead of a constant.
    val travelTimeDistributionMatrix = File(matrixFilePath).readLines().map { line ->
        line.split("\\s".toRegex()).map { ConstantRealDistribution(it.toDouble()) }
    }

    // Create FSE
    val fse = FSE(
        policy = policy,
        travelTimeDistributionMatrix = travelTimeDistributionMatrix,
    )

    return Simulator(
        fes = FES(),
        fse = fse,
        machines = machines,
        loadSharingDegradation = loadSharingDegradation,
    )
}

/**
 * Export the histogram of 1 metric (for each list entry) from a set of results
 */
private fun exportHistogram(
    fileName: String,
    results: Map<Machine, SimResultsWithHist>
) {
    val bucketSize = SimResultsWithHist.MAX_RESPONSE_TIME / SimResultsWithHist.N_BINS

    // Construct the lines for each bucket
    val lines = ArrayList<String>()
    for (bucketId in 0 until SimResultsWithHist.N_BINS) {
        // On the line, put the (middle of) the value the bucket represents and for each station how often it occurs
        val line = StringBuilder(String.format("%.2f", (bucketId + 0.5) * bucketSize))
        for ((_, result) in results.toSortedMap(compareBy { it.id })) {
            line.append("\t").append(String.format("%.4f", result.histResponseTimeCumulative[bucketId]))
//            line.append("\t").append(result.histResponseTime[bucketId])
        }
        lines.add(line.toString())
    }

    // Output this table to a file
    try {
        println("Exporting histogram to hist-$fileName.txt")
        Files.write(Paths.get("hist","hist-$fileName.txt"), lines)
    } catch (exception: IOException) {
        exception.printStackTrace()
    }
}

fun printWarmup() {
    for (warmup in listOf(0.01, 1.0, 2.5, 5.0, 7.5, 10.0, 25.0, 50.0, 100.0, 500.0, 1000.0, 5000.0)) {
        val resList = ArrayList<Map<Machine, SimResults>>()
        for (i in 0 until 10000) {
            val simulator = getSimulator(Policy.REACTIVE, false)
            if (warmup > 0) {
                simulator.simulate(warmup)
            }
            resList.add(simulator.simulate(25.0))
        }
        println("warmup: $warmup")
        println("cost sum: ${resList.map { res -> res.map { it.value.costAvg }.sum() }.average()}")
        println(
            "response time avg: ${
                resList.map { res -> res.map { it.value.responseTimeMean }.average() }.average()
            }"
        )
        println("op ratio avg: ${resList.map { res -> res.map { it.value.operationalRatio }.average() }.average()}")
        println()
    }
}

fun printHists() {
    for (policy in Policy.values()) {
        for (loadSharing in listOf(true, false)) {
            val simulator = getSimulator(policy, loadSharing)
            simulator.simulate(10000.0)
            exportHistogram(
                "${policy.name}${if (loadSharing) " - LS" else ""}",
                simulator.simulate(500000.0) as Map<Machine, SimResultsWithHist>
            )
        }
    }
}
