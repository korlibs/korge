package korlibs.datastructure

inline fun count(cond: (index: Int) -> Boolean): Int {
    var counter = 0
    while (cond(counter)) counter++
    return counter
}
