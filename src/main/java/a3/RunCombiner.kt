package a3

class RunCombiner(
    private val simulator: Simulator,
    private val numRuns: Int,
    private val runLength: Double,
    private val warmupTime: Double
) {
    init {
        val resultsList = ArrayList<Map<Machine, SimResults>>()
        // warm-up
        simulator.simulate(warmupTime)

        for (i in 0 until numRuns) {
            resultsList.add(simulator.simulate(runLength))
        }
    }
}
