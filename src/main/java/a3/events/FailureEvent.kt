package a3.events

import a3.Machine

class FailureEvent(time: Double, machine: Machine) : Event(time, machine) {
}
