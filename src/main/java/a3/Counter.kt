package a3

/**
 * Counter singleton
 */
object Counter {
    var count = 0
    fun next(): Int {
        return ++count
    }
}
