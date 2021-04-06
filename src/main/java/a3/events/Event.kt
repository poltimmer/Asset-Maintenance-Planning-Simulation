package a3.events

import a3.Counter
import a3.Machine

abstract class Event(val time: Double, val machine: Machine) : Comparable<Event> {
    private val creationOrder = Counter.next()

    /**
     * Compare on event time, with FIFO fallback on ties
     * @param other Event to be compared to
     * @return comparison of time
     */
    override operator fun compareTo(other: Event) = compareValuesBy(this, other, { it.time }, { it.creationOrder })
}
