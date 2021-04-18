package a3

import kotlin.math.floor

class SimResultsWithHist(simDuration: Double, startTime: Double) : SimResults(simDuration, startTime) {
    companion object {
        /**
         * Number of buckets for each histogram (not used for queue length)
         */
        const val N_BINS = 100

        /**
         * When normalization is performed, the sum of all values is this.
         * NB: Normalizing to 1 and creating doubles could be done, however those are harder to
         * insert in Excel, so high enough ints is used.
         */
        const val NORMALIZE_TO = 10000000

        /**
         * The maximum value to show in the histogram of each metric
         */
        const val MAX_RESPONSE_TIME = 10.0
    }

    /**
     * The actual histogram
     */
    val histResponseTime = IntArray(N_BINS)

    val histResponseTimeCumulative: DoubleArray
        get() {
            val res = DoubleArray(N_BINS)
            var cumsum = 0.0
            for ((idx, item) in histResponseTime.withIndex()) {
                cumsum += item
                res[idx] = cumsum
            }
            // normalize
            val max = res.maxOrNull() ?: 1.0
            for (idx in res.indices) {
                res[idx] /= max
            }
            return res
        }


    override fun reportResponseTime(responseTime: Double) {
        super.reportResponseTime(responseTime)
        val bucketSize: Double = MAX_RESPONSE_TIME / N_BINS
        val bucket = floor(responseTime / bucketSize).toInt()
        if (bucket < N_BINS) histResponseTime[bucket]++
    }

    /**
     * Normalize the histogram such that the sum of all values is the set NORMALIZE_TO.
     */
    fun normalize() {
        for (i in 0 until N_BINS) {
            histResponseTime[i] /= histResponseTime.sum() / NORMALIZE_TO
        }
    }

}
