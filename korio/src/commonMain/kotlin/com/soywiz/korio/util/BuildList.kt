package com.soywiz.korio.util

import com.soywiz.kds.*

inline fun <T> buildList(callback: FastArrayList<T>.() -> Unit): List<T> = FastArrayList<T>().apply(callback)
