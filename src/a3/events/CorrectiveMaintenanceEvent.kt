package a3.events

import a3.FSE
import a3.Machine

class CorrectiveMaintenanceEvent(time: Double, machine: Machine, fse: FSE) : MaintenanceEvent(time, machine, fse) {
}
