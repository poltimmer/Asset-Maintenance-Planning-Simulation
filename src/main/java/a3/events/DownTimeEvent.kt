package a3.events

import a3.Machine

class DownTimeEvent(time: Double, machine: Machine) : Event(time, machine)
