package a3

import a3.events.DegradationEvent
import a3.events.FSEArrivalEvent
import a3.events.MaintenanceEvent

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
                if (fse.policy == Policy.REACTIVE) {
                    val failedMachines = ArrayList<Machine>()
                    for (machine in machines) {
                        if (machine.hasFailed()) {
                            failedMachines.add(machine)
                        }
                    }
                    if (failedMachines.isNotEmpty()) {
                        // Pick the single machine with maximum downtime penalty. Arbitrary choice in case of ties.
                        val machineToRepair = failedMachines.maxByOrNull { it.downTimePenaltyAtTime(currentTime) }!!

                        if (machineToRepair == event.machine) {
//                    fes.add(CorrectiveMaintenanceEvent(time = , machine = machineToRepair, fse = fse))
                            // Corrective Maintenance

                            results[machineToRepair]!!.reportCost(1.0) // TODO: report actual repair cost
                            val repairTime = 1.0 // TODO: sample repair time
                            fes.add(
                                MaintenanceEvent(
                                    time = currentTime + repairTime,
                                    machine = machineToRepair,
                                    fse = fse
                                )
                            )
                            fes.add(
                                FSEArrivalEvent(
                                    time = currentTime + repairTime,
                                    machine = machineToRepair,
                                    fse = fse
                                )
                            )
                        } else {
                            val travelTime =
                                fse.arrivalDistributionMatrix[event.machine.id][machineToRepair.id].sample() //TODO: travel time from matrix?
                            fes.add(
                                FSEArrivalEvent(
                                    time = currentTime + travelTime,
                                    machine = machineToRepair,
                                    fse = fse
                                )
                            )
                        }
                    } else {
                        // Stay idle until next state change
                        // Add event at same time as next event. Will be scheduled right after next event in queue,
                        // because the queue is FIFO on ties.
                        fes.add(FSEArrivalEvent(time = fes.peek().time, machine = event.machine, fse = fse))
                    }
                } else if (fse.policy == Policy.GREEDY) {
                    // Machine with max degradation, then shortest travel time
                    val machineToRepair =
                        machines.maxWithOrNull( //TODO: pick uniformly at random, or prove it already does so
                            compareBy({ it.degradation },
                                { -fse.arrivalDistributionMatrix[event.machine.id][it.id].mean }) //TODO: travel time
                        )!!

                    if (machineToRepair.degradation == 0.0) {
                        // Stay idle until next state change
                        // Add event at same time as next event. Will be scheduled right after next event in queue,
                        // because the queue is FIFO on ties.
                        fes.add(FSEArrivalEvent(time = fes.peek().time, machine = event.machine, fse = fse))
                    } else {
                        if (machineToRepair == event.machine) {
                            // Preventive maintenance
                            results[machineToRepair]!!.reportCost(1.0) // TODO: report repair penalty

                            val repairTime = 1.0 // TODO: sample repair time
                            fes.add(
                                MaintenanceEvent(
                                    time = currentTime + repairTime,
                                    machine = machineToRepair,
                                    fse = fse
                                )
                            )
                            fes.add(
                                FSEArrivalEvent(
                                    time = currentTime + repairTime,
                                    machine = machineToRepair,
                                    fse = fse
                                )
                            )
                        }
                    }
                }
            }

            if (event is DegradationEvent) {
                // degrade machine and schedule new degradation event
                event.machine.degrade(currentTime)
                if (!event.machine.hasFailed()) {
                    fes.add(
                        DegradationEvent(
                            time = currentTime + event.machine.arrivalDistribution.sample(),
                            machine = event.machine
                        )
                    )
                }
            }

            if (event is MaintenanceEvent) {
                // Repair and report downtime penalty
                event.machine.repair()
                results[event.machine]!!.reportCost(event.machine.downTimePenaltyAtTime(currentTime))
                // When maintenance is finished, restart degradation process.
                fes.add(
                    DegradationEvent(
                        time = currentTime + event.machine.arrivalDistribution.sample(),
                        machine = event.machine
                    )
                )
            }
        }

        // Collect final downtime penalties
        for (machine in machines) {
            results[machine]!!.reportCost(machine.downTimePenaltyAtTime(currentTime))
        }

        return results
    }
}
