package a3

import org.apache.commons.math3.distribution.BetaDistribution
import org.apache.commons.math3.distribution.ConstantRealDistribution
import org.apache.commons.math3.distribution.ExponentialDistribution
import java.io.File


fun main() {
    println("hello world")
    // TODO: investigate costs. Only enable downtime costs.
    val simulator = getSimulator(Policy.REACTIVE, true)
    for (i in 0 until 4) {
        val results = simulator.simulate(100000.0)
        println("_______________________")
        for ((machine, result) in results.toSortedMap(compareBy { it.id })) {
            println()
            println("Machine id: ${machine.id}")
            println("Average cost: ${result.costAvg}")
            println("Response time mean: ${result.responseTimeMean}  |  var: ${result.responseTimeVar}")
            println("Operational ratio: ${result.operationalRatio}")
        }
        // todo: print cost sum
        println("${Counter.count} events fired")
    }
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
