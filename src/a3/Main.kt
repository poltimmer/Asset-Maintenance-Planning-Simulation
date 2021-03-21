package a3

import a3.events.Event
import a3.events.MaintenanceEvent

fun main() {
    println("hello world");
    var event = MaintenanceEvent(0.1, Machine(1))
    println(event.time)
}
