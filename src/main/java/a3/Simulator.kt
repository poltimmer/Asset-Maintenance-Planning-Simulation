package a3

import a3.events.DegradationEvent
import a3.events.FSEArrivalEvent
import a3.events.MaintenanceEvent
import java.util.*

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

                            results[machineToRepair]!!.reportCost(machineToRepair.correctiveMaintenanceCost)
                            val repairTime = machineToRepair.correctiveMaintenanceTimeDistribution.sample()
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
                                fse.arrivalDistributionMatrix[event.machine.id][machineToRepair.id].sample()
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
                        if (fes.peek() == null) {
                            println("yeah, it's null")
                        }
                        fes.add(FSEArrivalEvent(time = fes.peek().time, machine = event.machine, fse = fse)) //todo: what if time is null?
                    }
                } else if (fse.policy == Policy.GREEDY) { //TODO: FES goes empty under greedy policy
                    // Machine with max degradation, then shortest travel time
                    val machineToRepair =
                        machines.maxWithOrNull( //TODO: pick uniformly at random, or prove it already does so
                            compareBy({ it.degradation },
                                { -fse.arrivalDistributionMatrix[event.machine.id][it.id].numericalMean })
                        )!!

                    if (machineToRepair.degradation == 0.0) {
                        // Stay idle until next state change
                        // Add event at same time as next event. Will be scheduled right after next event in queue,
                        // because the queue is FIFO on ties.
                        if (fes.peek() == null) {
                            println("yeah, it's null")
                        }
                        fes.add(FSEArrivalEvent(time = fes.peek().time, machine = event.machine, fse = fse)) //todo: this might be where it goes wrong?
                    } else {
                        if (machineToRepair == event.machine) {
                            // Preventive maintenance
                            results[machineToRepair]!!.reportCost(machineToRepair.preventiveMaintenanceCost)

                            val repairTime = machineToRepair.preventiveMaintenanceTimeDistribution.sample()
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
                                fse.arrivalDistributionMatrix[event.machine.id][machineToRepair.id].sample()
                            fes.add(
                                FSEArrivalEvent(
                                    time = currentTime + travelTime,
                                    machine = machineToRepair,
                                    fse = fse
                                )
                            )
                        }
                    }
                }
            }

            if (event is DegradationEvent) {
                // degrade machine and schedule new degradation event if it is still operational
                event.machine.degrade(currentTime)
                if (event.machine.hasFailed()) {
                    results[event.machine]!!.reportMachineFailed(currentTime)
                } else {
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
                results[event.machine]!!.reportMachineRepaired(currentTime)
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
