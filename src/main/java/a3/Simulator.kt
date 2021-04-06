package a3

import a3.events.DegradationEvent
import a3.events.FSEArrivalEvent
import a3.events.MaintenanceEvent
import org.apache.commons.math3.distribution.ExponentialDistribution

class Simulator {
    private val machines = ArrayList<Machine>()

    fun simulate(maxTime: Double) {
        val fes = FES()
        val fse = FSE(
            arrivalDistributionMatrix = arrayOf(arrayOf(ExponentialDistribution(0.1))),
            policy = Policy.REACTIVE
        ) //TODO: FSE from parameter, maybe in constructor

        fes.add(FSEArrivalEvent(time = 0.0, fse = fse, machine = machines[0]))

        // initialize degradations
        for (machine in machines) {
            fes.add(DegradationEvent(machine.arrivalDistribution.sample(), machine))
        }

        var currentTime: Double = 0.0 // t
        while (currentTime < maxTime) {
            val event = fes.poll()
            currentTime = event.time

            if (event is FSEArrivalEvent) {
                if (fse.policy == Policy.REACTIVE) {
                    var failedMachines = ArrayList<Machine>()
                    for (machine in machines) {
                        if (machine.hasFailed()) {
                            failedMachines.add(machine)
                        }
                    }
                    if (failedMachines.isNotEmpty()) {
                        // Pick the single machine with maximum downtime penalty. Arbitrary choice in case of ties.
                        var machineToRepair = failedMachines.maxByOrNull { it.downTimePenaltyAtTime(currentTime) }

                        if (machineToRepair == null) {
                            throw Exception("machine to repair is null while list of failed machines is not empty")
                        }

                        if (machineToRepair == event.machine) {
//                    fes.add(CorrectiveMaintenanceEvent(time = , machine = machineToRepair, fse = fse))
                            // Corrective Maintenance
                            // TODO: report repair cost
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
                    val machineToRepair = machines.maxWithOrNull( //TODO: pick uniformly at random, or prove it already does so
                        compareBy({ it.degradation },
                            { -fse.arrivalDistributionMatrix[event.machine.id][it.id].mean }) //TODO: travel time
                    )

                    if (machineToRepair == null || machineToRepair.degradation == 0.0) {
                        // Stay idle until next state change
                        // Add event at same time as next event. Will be scheduled right after next event in queue,
                        // because the queue is FIFO on ties.
                        fes.add(FSEArrivalEvent(time = fes.peek().time, machine = event.machine, fse = fse))
                    } else {
                        if (machineToRepair == event.machine) {
                            // Preventive maintenance
                            // TODO: report repair penalty
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
                event.machine.repair()
                // TODO: report downtime penalty
                // When maintenance is finished, restart degradation process.
                fes.add(
                    DegradationEvent(
                        time = currentTime + event.machine.arrivalDistribution.sample(),
                        machine = event.machine
                    )
                )
            }
        }
        // TODO: collect downtime penalties
    }
}
