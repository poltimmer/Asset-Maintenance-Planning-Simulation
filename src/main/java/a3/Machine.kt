package a3

import org.apache.commons.math3.distribution.BetaDistribution
import org.apache.commons.math3.distribution.ExponentialDistribution
import java.lang.Exception
import kotlin.math.floor
import kotlin.math.min

/**
 * @param id
 * @param threshold failure threshold of this machine
 * @param degradation current degradation of this machine
 * @param arrivalDistribution distribution for sampling arrival time of degradation jump
 * @param degradationDistribution distribution for sampling degradation jump
 */
class Machine(
    val id: Int,
    val threshold: Double,
    val downTimeCost: Double,
    val arrivalDistribution: ExponentialDistribution,
    val degradationDistribution: BetaDistribution,
    var degradation: Double = 0.0,
    var lastFailedAtTime: Double = 0.0
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
            // Only whole timesteps
            floor(currentTime - lastFailedAtTime) * downTimeCost
        } else {
            0.0
        }
    }
}
