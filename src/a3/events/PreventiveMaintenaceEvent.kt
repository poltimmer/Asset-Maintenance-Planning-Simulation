package a3.events

import a3.FSE
import a3.Machine

class PreventiveMaintenaceEvent(time: Double, machine: Machine, fse: FSE) : MaintenanceEvent(time, machine, fse)
