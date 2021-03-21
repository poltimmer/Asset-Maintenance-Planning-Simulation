package dist

import java.util.*

/**
 * Beta distribution
 * \frac{\Gamma(\alpha+\beta)}{\Gamma(\alpha)\Gamma(\beta)}x^{\alpha-1}(1-x)^{\beta-1}
 * @param alpha
 * @param beta
 * @param random
 */
class BetaDistribution(val alpha: Double, val beta: Double, val random: Random): Distribution() {
    val gammaDistribution: GammaDistribution = GammaDistribution(alpha, beta, this.random)

    override fun variance(): Double {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun nextRandom(): Double {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun expectation(): Double {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
