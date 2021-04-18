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
    private val loadSharingDegradation: Boolean = false,
    private val withHist: Boolean = false
) {
    private var currentTime = 0.0

    init {
        // Initialize FSE
        fes.add(FSEArrivalEvent(time = 0.0, machine = machines[0]))

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
            results[machine] =
                if (withHist) {
                    SimResultsWithHist(simDuration = duration, startTime = startTime)
                } else {
                    SimResults(simDuration = duration, startTime = startTime)
                }
        }

        val avgCorrectiveMaintenanceCost = machines.map { it.correctiveMaintenanceCost }.average()

        while (currentTime < startTime + duration) {
            val event = fes.poll()
            currentTime = event.time

            if (event is FSEArrivalEvent) {
                val machineToRepair = when (fse.policy) {
                    // Pick the single machine with maximum downtime penalty. Random choice in case of ties.
                    Policy.REACTIVE -> machines.filter { it.hasFailed }
                        .maxWithOrNull(compareBy<Machine> { it.downTimeCost }.thenBy { Random.nextDouble() })

                    // Pick the single machine with maximal degradation, then by minimal expected travel time, then random in case of ties.
                    Policy.GREEDY -> machines.maxWithOrNull(
                        compareBy<Machine> { it.degradation }
                            .thenBy { -fse.travelTimeDistributionMatrix[event.machine.id][it.id].numericalMean }
                            .thenBy { Random.nextDouble() })!!
                    Policy.CUSTOM -> machines.maxWithOrNull(
                        compareBy<Machine> {
                            it.degradation * it.downTimeCost *
                                    (if (!it.hasFailed) it.correctiveMaintenanceCost else avgCorrectiveMaintenanceCost)
                        }
                            .thenBy { -fse.travelTimeDistributionMatrix[event.machine.id][it.id].numericalMean }
                            .thenBy { Random.nextDouble() })
                    else -> throw Exception("Policy not recognized")
                }

                if (machineToRepair != null && machineToRepair.degradation > 0) {
                    if (machineToRepair != event.machine) {
                        // Travel to machine that needs repairs
                        val travelTime =
                            fse.travelTimeDistributionMatrix[event.machine.id][machineToRepair.id].sample()

                        fes.add(FSEArrivalEvent(currentTime + travelTime, machineToRepair))
                    } else {
                        // No need to travel. Repair machine.
                        val repairTime =
                            if (machineToRepair.hasFailed) {
                                // Report corrective maintenance cost and sample repair time
                                results[machineToRepair]!!.reportResponseTime(currentTime - machineToRepair.lastFailedAtTime)
                                results[machineToRepair]!!.reportCost(machineToRepair.correctiveMaintenanceCost)
                                machineToRepair.correctiveMaintenanceTimeDistribution.sample()
                            } else {
                                // Bring machine offline for preventive maintenance, and start downtime penalty
                                machineToRepair.fail(currentTime)
                                results[machineToRepair]!!.reportMachineOffline(currentTime)
                                fes.add(DownTimeEvent(currentTime + 1, machineToRepair))

                                results[machineToRepair]!!.reportCost(machineToRepair.preventiveMaintenanceCost)
                                machineToRepair.preventiveMaintenanceTimeDistribution.sample()
                            }
                        // Schedule a maintenance event for when the repair is finished
                        fes.add(MaintenanceEvent(currentTime + repairTime, machineToRepair))
                    }
                } else {
                    // Stay idle until next state change
                    // Add event at same time as next event. Will be scheduled right after next event in queue,
                    // because the queue is FIFO on ties.
                    fes.add(
                        FSEArrivalEvent(
                            fes.peek()?.time ?: currentTime,
                            event.machine
                        )
                    )
                }
            } else if (event is DegradationEvent) {
                var machineToDegrade: Machine? = null
                if (event.machine.hasFailed) {
                    if (loadSharingDegradation) {
                        // Pick a running machine to degrade, uniform at random.
                        // This simulates the uniform load distribution.
                        val runningMachines = machines.filterNot { it.hasFailed }
                        if (runningMachines.isNotEmpty()) {
                            machineToDegrade = runningMachines.random()
                        }
                    }
                } else {
                    machineToDegrade = event.machine
                }
                // If there is a machine to degrade, degrade that machine and check for failure
                if (machineToDegrade != null) {
                    machineToDegrade.degrade(currentTime)
                    if (machineToDegrade.hasFailed) {
                        // If the machine has gone from operational to failed, start downtime cost penalty
                        results[machineToDegrade]!!.reportMachineOffline(currentTime)
                        fes.add(DownTimeEvent(currentTime + 1, machineToDegrade))
                    }
                }
                // Schedule a new degradation event for this machine
                fes.add(DegradationEvent(currentTime + event.machine.arrivalDistribution.sample(), event.machine))
            } else if (event is MaintenanceEvent) {
                // Repair and report the machine being online
                event.machine.repair()
                results[event.machine]!!.reportMachineOnline(currentTime)
                // Make the FSE available again
                fes.add(FSEArrivalEvent(currentTime, event.machine))
            } else if (event is DownTimeEvent) {
                // Keep reporting downtime penalties while the machine is down
                if (event.machine.hasFailed) {
                    results[event.machine]!!.reportCost(event.machine.downTimeCost)
                    fes.add(DownTimeEvent(currentTime + 1, event.machine))
                }
            } else {
                throw Exception("Unexpected event")
            }
        }

        return results
    }
}
