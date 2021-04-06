package a3.events

import a3.FSE
import a3.Machine

class PreventiveMaintenaceEventAbstract(time: Double, machine: Machine, fse: FSE) : AbstractMaintenanceEvent(time, machine, fse)
