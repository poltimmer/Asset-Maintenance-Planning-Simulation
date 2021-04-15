package a3

import a3.events.DegradationEvent
import a3.events.DownTimeEvent
import a3.events.FSEArrivalEvent
import a3.events.MaintenanceEvent
import kotlin.random.Random

class Simulator(
    private val fes: FES,
    private val fse: FSE,
    private val machines: List<Machine>,
    private val degradationLoadSharing: Boolean = false
) {
    private var currentTime = 0.0

    init {
        // Initialize FSE
        fes.add(FSEArrivalEvent(time = 0.0, machine = machines[0], fse = fse))

        // initialize degradations
        for (machine in machines) {
            fes.add(DegradationEvent(machine.arrivalDistribution.sample(), machine))
        }
    }


    fun simulate(duration: Double): Map<Machine, SimResults> {
        val results = HashMap<Machine, SimResults>()
        val startTime = currentTime

        for (machine in machines) {
            // Create SimResults objects
            // machine.lastFailedAtTime = startTime fixme: new implementation
            results[machine] = SimResults(simDuration = duration, startTime = startTime)
        }

        while (currentTime < startTime + duration) {
            val event = fes.poll()
            currentTime = event.time

            if (event is FSEArrivalEvent) {
                val machineToRepair = when (fse.policy) {
                    // Pick the single machine with maximum downtime penalty. Random choice in case of ties.
                    Policy.REACTIVE -> machines.filter { it.hasFailed }
                        .maxWithOrNull(compareBy<Machine> { it.downTimePenaltyAtTime(currentTime) }.thenBy { Random.nextDouble() })

                    // Pick the single machine with maximal degradation, then by minimal expected travel time, then random in case of ties.
                    Policy.GREEDY -> machines.maxWithOrNull(
                        compareBy<Machine> { it.degradation }
                            .thenBy { -fse.travelTimeDistributionMatrix[event.machine.id][it.id].numericalMean }
                            .thenBy { Random.nextDouble() })!!
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
                            if (machineToRepair.hasFailed) { // Report maintenance cost and sample repair time
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
                if (event.machine.hasFailed) {
                    results[event.machine]!!.reportMachineFailed(currentTime)
                    if (degradationLoadSharing) {
                        // Pick and degrade a running machine, uniform at random. This simulates the
                        // uniform load distribution.
                        val runningMachines = machines.filterNot { it.hasFailed }
                        if (runningMachines.isNotEmpty()) {
                            runningMachines.random().degrade(currentTime)
                        }
                    }
                } else {
                    // degrade machine and schedule new degradation event if it is still operational
                    event.machine.degrade(currentTime)
                    if (event.machine.hasFailed) {
                        // If the machine has went from operational to failed, start downtime cost penalty
                        fes.add(DownTimeEvent(currentTime + 1, event.machine))
                    }
                }
                fes.add(DegradationEvent(currentTime + event.machine.arrivalDistribution.sample(), event.machine))
            }

            if (event is MaintenanceEvent) {
                // Repair and report downtime penalty
                results[event.machine]!!.reportResponseTime(currentTime - event.machine.lastFailedAtTime)
                event.machine.repair()
                //  results[event.machine]!!.reportCost(event.machine.downTimePenaltyAtTime(currentTime)) // fixme: switched to new implementation
                results[event.machine]!!.reportMachineRepaired(currentTime)
                // When maintenance is finished, restart degradation process.
//                fes.add(DegradationEvent(currentTime + event.machine.arrivalDistribution.sample(), event.machine)) // fixme: switched to new implementation
            }

            if (event is DownTimeEvent) {
                // If machine is still down, report penalty, and schedule new event.
                // Else, shut down the downtime event process.
                if (event.machine.hasFailed) {
                    results[event.machine]!!.reportCost(event.machine.downTimeCost)
                    fes.add(DownTimeEvent(currentTime + 1, event.machine))
                }
            }
        }

        // Collect final downtime penalties
        // todo: resetting fail time between batches has implications for response time.
        //  possible fix is just not resetting and not collecting downtime penalty at end.
//        for (machine in machines) {
//            results[machine]!!.reportCost(machine.downTimePenaltyAtTime(currentTime))
//        }

        return results
    }
}
