package korlibs.io.util

import korlibs.datastructure.FastArrayList

inline fun <T> buildList(callback: FastArrayList<T>.() -> Unit): List<T> = FastArrayList<T>().apply(callback)
