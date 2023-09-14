package korlibs.datastructure

/**
 * This acts as a [lazy] delegate but for functions.
 */
fun <T : Any> (() -> T).memoize(): (() -> T) {
    val func = this
    var set = false
    lateinit var cached: T
    return {
        if (!set) {
            cached = func()
            set = true
        }
        cached
    }
}
