package a3

import org.apache.commons.math3.distribution.AbstractRealDistribution

/**
 * Field service engineer
 */
class FSE(
    val travelTimeDistributionMatrix: List<List<AbstractRealDistribution>>,
    val policy: Policy
)
