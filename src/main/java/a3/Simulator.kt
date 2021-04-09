package a3

import a3.events.DegradationEvent
import a3.events.FSEArrivalEvent
import a3.events.MaintenanceEvent
import kotlin.random.Random

class Simulator(private val fes: FES, private val fse: FSE, private val machines: List<Machine>) {
    private var currentTime = 0.0

    init {
        // Initialize FSE
        fes.add(FSEArrivalEvent(time = 0.0, fse = fse, machine = machines[0]))

        // initialize degradations
        for (machine in machines) {
            fes.add(DegradationEvent(machine.arrivalDistribution.sample(), machine))
        }
    }


    fun simulate(duration: Double): HashMap<Machine, SimResults> {
        val results = HashMap<Machine, SimResults>()
        val startTime = currentTime

        for (machine in machines) {
            // Reset downtime penalties and create SimResults objects
            machine.lastFailedAtTime = startTime
            results[machine] = SimResults(simDuration = duration, startTime = startTime)
        }

        while (currentTime < startTime + duration) {
            val event = fes.poll()
            currentTime = event.time

            if (event is FSEArrivalEvent) {
                val machineToRepair = when (fse.policy) {
                    // Pick the single machine with maximum downtime penalty. Random choice in case of ties.
                    Policy.REACTIVE -> machines.filter { it.hasFailed() }
                        .maxWithOrNull(compareBy({ it.downTimePenaltyAtTime(currentTime) }, { Random.nextDouble() }))

                    // Pick the single machine with maximal degradation, then by minimal expected travel time, then random in case of ties.
                    Policy.GREEDY -> machines.maxWithOrNull(
                        compareBy(
                            { it.degradation },
                            { -fse.travelTimeDistributionMatrix[event.machine.id][it.id].numericalMean },
                            { Random.nextDouble() })
                    )!!
                    else -> throw Exception("Policy not recognized")
                }

                if (machineToRepair != null && machineToRepair.degradation > 0) {
                    if (machineToRepair != event.machine) {
                        // Travel to machine that needs repairs
                        val travelTime =
                            fse.travelTimeDistributionMatrix[event.machine.id][machineToRepair.id].sample()

                        fes.add(FSEArrivalEvent(currentTime + travelTime, machineToRepair, fse))
                    } else {
                        // No need to travel. Repair machine.

                        val repairTime =
                            if (machineToRepair.hasFailed()) { // Report maintenance cost and sample repair time
                                results[machineToRepair]!!.reportCost(machineToRepair.correctiveMaintenanceCost)
                                machineToRepair.correctiveMaintenanceTimeDistribution.sample()
                            } else {
                                results[machineToRepair]!!.reportCost(machineToRepair.preventiveMaintenanceCost)
                                machineToRepair.preventiveMaintenanceTimeDistribution.sample()
                            }

                        fes.add(MaintenanceEvent(currentTime + repairTime, machineToRepair, fse))
                        fes.add(FSEArrivalEvent(currentTime + repairTime, machineToRepair, fse))
                    }
                } else {
                    // Stay idle until next state change
                    // Add event at same time as next event. Will be scheduled right after next event in queue,
                    // because the queue is FIFO on ties.
                    println("idle") //todo: remove
                    if (fes.peek() == null) {
                        println("yeah, it's null")
                    }
                    fes.add(FSEArrivalEvent(fes.peek().time, event.machine, fse)) //todo: what if time is null?
                }
            }

            if (event is DegradationEvent) {
                // degrade machine and schedule new degradation event if it is still operational
                event.machine.degrade(currentTime)
                if (event.machine.hasFailed()) {
                    results[event.machine]!!.reportMachineFailed(currentTime)
                } else {
                    fes.add(DegradationEvent(currentTime + event.machine.arrivalDistribution.sample(), event.machine))
                }
            }

            if (event is MaintenanceEvent) {
                // Repair and report downtime penalty
                event.machine.repair()
                results[event.machine]!!.reportCost(event.machine.downTimePenaltyAtTime(currentTime))
                results[event.machine]!!.reportMachineRepaired(currentTime)
                // When maintenance is finished, restart degradation process.
                fes.add(DegradationEvent(currentTime + event.machine.arrivalDistribution.sample(), event.machine))
            }
        }

        // Collect final downtime penalties
        for (machine in machines) {
            results[machine]!!.reportCost(machine.downTimePenaltyAtTime(currentTime))
        }

        return results
    }
}
