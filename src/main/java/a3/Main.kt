package a3

import org.apache.commons.math3.distribution.BetaDistribution
import org.apache.commons.math3.distribution.ConstantRealDistribution
import org.apache.commons.math3.distribution.ExponentialDistribution
import java.io.File


fun main() {
    println("hello world");
//    var event = CorrectiveMaintenanceEvent(0.1, Machine(1, threshold = 1.0), fse = FSE())
//    println(event.time)
    val results = getSimulator(Policy.GREEDY).simulate(100000.0)
    for (result in results) {
        println()
        println("Average cost: ${result.value.costAvg}")
        println("Response time: ${result.value.responseTime}")
        println("Operational ratio: ${result.value.operationalRatio}")
    }
}

fun getSimulator(
    policy: Policy,
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
    )
}
