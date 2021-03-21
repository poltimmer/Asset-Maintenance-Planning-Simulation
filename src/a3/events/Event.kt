package a3.events

import a3.Machine

abstract class Event(val time: Double, val machine: Machine): Comparable<Event> {
    /**
     * @param other Event to be compared to
     * @return comparison of time property
     */
    override operator fun compareTo(other: Event) = compareValues(this.time, other.time)
}
