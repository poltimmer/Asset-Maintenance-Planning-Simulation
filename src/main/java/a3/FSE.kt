package a3

import org.apache.commons.math3.distribution.AbstractRealDistribution
import org.apache.commons.math3.distribution.ConstantRealDistribution
/**
 * Field service engineer
 */
class FSE(
    val travelTimeDistributionMatrix: List<List<AbstractRealDistribution>>,
    val policy: Policy
) {
    // only 1 FSE
    // preventive maintenance
    //      has lower cost
    // corrective maintenance
    //      has higher cost
    //      only when a machine fails
    // downtime also has associated cost penalty
    //      either form of maintenance is downtime
    //          maintenance cost is separate from downtime cost
    //      failed machine also accumulates downtime
    //      downtime penalty = proportional to downtime (linear)
    // store costs in separate sets (at least in mathematical model)

    // at a location (machine)
    // cycles and repairs machines
}
