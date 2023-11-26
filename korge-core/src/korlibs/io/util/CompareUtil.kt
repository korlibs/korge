package korlibs.io.util

inline fun Int.compareToChain(callback: () -> Int): Int = if (this != 0) this else callback()
