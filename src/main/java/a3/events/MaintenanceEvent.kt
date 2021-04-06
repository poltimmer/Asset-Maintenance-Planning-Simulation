package a3.events

import a3.FSE
import a3.Machine

/**
 * @param time time of the event
 * @param machine machine where the maintenance is executed
 * @param fse FSE doing the maintenance
 */
abstract class MaintenanceEvent(time: Double, machine: Machine, val fse: FSE): Event(time, machine)
