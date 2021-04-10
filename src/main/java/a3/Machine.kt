package a3

import org.apache.commons.math3.distribution.AbstractRealDistribution
import kotlin.math.floor
import kotlin.math.min

/**
 * @param id
 * @param threshold failure threshold of this machine
 * @param downTimeCost cost of downtime per single time step
 * @param preventiveMaintenanceCost cost of a preventive maintenance action
 * @param correctiveMaintenanceCost cost of a corrective maintenance action
 * @param arrivalDistribution distribution for sampling arrival time of degradation jump
 * @param degradationDistribution distribution for sampling degradation jump
 * @param preventiveMaintenanceTimeDistribution distribution for sampling the repair time of preventive maintenance
 * @param correctiveMaintenanceTimeDistribution distribution for sampling the repair time of corrective maintenance
 * @param degradation current degradation of this machine
 */
class Machine(
    val id: Int,
    val threshold: Double,
    val downTimeCost: Double,
    val preventiveMaintenanceCost: Double,
    val correctiveMaintenanceCost: Double,
    val arrivalDistribution: AbstractRealDistribution,
    val degradationDistribution: AbstractRealDistribution,
    val preventiveMaintenanceTimeDistribution: AbstractRealDistribution,
    val correctiveMaintenanceTimeDistribution: AbstractRealDistribution,
    var degradation: Double = 0.0,
    var lastFailedAtTime: Double = 0.0,
) {
    // unique location
    // subject to degradation

    fun degrade(currentTime: Double) {
        if (hasFailed()) return
        // cap degradation to threshold
        degradation = min(degradation + degradationDistribution.sample(), threshold)
        if (hasFailed()) {
            lastFailedAtTime = currentTime
        }
    }

    fun repair() {
        degradation = 0.0
    }

    fun hasFailed(): Boolean {
        return degradation >= threshold
    }

    fun downTimePenaltyAtTime(currentTime: Double): Double {
        return if (hasFailed()) {
            if (currentTime - lastFailedAtTime < 0) {
                throw Exception("negative downtime penalty")
            }
            // Only whole time steps
            floor(currentTime - lastFailedAtTime) * downTimeCost
        } else {
            0.0
        }
    }
}
