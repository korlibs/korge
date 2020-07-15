package com.soywiz.korio.util

inline fun Int.compareToChain(callback: () -> Int): Int = if (this != 0) this else callback()
