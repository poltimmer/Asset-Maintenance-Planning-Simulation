package a3.events

import a3.FSE
import a3.Machine

class FSEArrivalEvent(time: Double, machine: Machine, val fse: FSE) : Event(time, machine) { //todo: remove FSE
}
