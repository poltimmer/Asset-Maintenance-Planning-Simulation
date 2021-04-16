package a3.events

import a3.Machine

class MaintenanceEvent(time: Double, machine: Machine) : Event(time, machine)
