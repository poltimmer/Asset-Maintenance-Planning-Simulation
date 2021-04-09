package a3

import org.apache.commons.math3.distribution.AbstractRealDistribution
import org.apache.commons.math3.distribution.BetaDistribution
import org.apache.commons.math3.distribution.ConstantRealDistribution
import org.apache.commons.math3.distribution.ExponentialDistribution
import java.io.File
import java.util.*
import kotlin.collections.HashMap


fun main() {
    println("hello world");
//    var event = CorrectiveMaintenanceEvent(0.1, Machine(1, threshold = 1.0), fse = FSE())
//    println(event.time)
    val results = getSimulator(Policy.REACTIVE).simulate(100000.0)
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
    val inputScanner = File(inputFilePath)
    val keys = listOf(
        "arrivalLambdas",
        "alphas",
        "betas",
        "preventiveMaintenanceTimes",
        "correctiveMaintenanceTimes",
        "downTimeCosts"
    )
    val data = HashMap<String, List<Double>>()
    for ((line, key) in inputScanner.readLines().zip(keys)) {
        data[key] = line.split("\\s".toRegex()).map { it.toDouble() }
    }
//    val thresholds = inputScanner.nextLine().split(regex).map { it.toDouble() }
//    val arrivalLambdas = inputScanner.nextLine().split(regex).map { it.toDouble() }
//    val alphas = inputScanner.nextLine().split(regex).map { it.toDouble() }
//    val betas = inputScanner.nextLine().split(regex).map { it.toDouble() }
//    val preventiveMaintenanceTimes = inputScanner.nextLine().split(regex).map { it.toDouble() }
//    val correctiveMaintenanceTimes = inputScanner.nextLine().split(regex).map { it.toDouble() }
//    val preventiveMaintenanceCosts = inputScanner.nextLine().split(regex).map { it.toDouble() }
//    val correctiveMaintenanceCosts = inputScanner.nextLine().split(regex).map { it.toDouble() }
//    val downTimeCosts = inputScanner.nextLine().split(regex).map { it.toDouble() }
    val machines = ArrayList<Machine>()

    for (i in thresholds.indices) {
        machines.add(
            Machine(
                id = i,
                threshold = data["thresholds"]!![i],
                downTimeCost = data["downTimeCosts"]!![i],
                preventiveMaintenanceCost = data["preventiveMaintenanceCosts"]!![i],
                correctiveMaintenanceCost = data["correctiveMaintenanceCosts"]!![i],
                arrivalDistribution = ExponentialDistribution(1 / data["arrivalLambdas"]!![i]),
                degradationDistribution = BetaDistribution(data["alphas"]!![i], data["betas"]!![i]),
                preventiveMaintenanceTimeDistribution = ExponentialDistribution(data["preventiveMaintenanceTimes"]!![i]),
                correctiveMaintenanceTimeDistribution = ExponentialDistribution(data["correctiveMaintenanceTimes"]!![i])
            )
        )
    }
    inputScanner.close()

    val matrixScanner = Scanner(File(matrixFilePath))

    val arrivalDistributionMatrix = ArrayList<List<AbstractRealDistribution>>()

    // Add a matrix of ConstantRealDistribution objects. This allows usage of a distribution instead of a constant.
    while (matrixScanner.hasNextLine()) {
        arrivalDistributionMatrix.add(
            matrixScanner.nextLine().split(regex).map { ConstantRealDistribution(it.toDouble()) })
    }
    matrixScanner.close()

    val fse = FSE(
        policy = policy,
        arrivalDistributionMatrix = arrivalDistributionMatrix,
    )

    return Simulator(
        fes = FES(),
        fse = fse,
        machines = machines,
    )
}
