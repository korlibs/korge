package com.soywiz.kds

@Deprecated("", ReplaceWith("Array<R>(size) { func(this[it]) }"))
inline fun <reified T, reified R> Array<T>.mapArray(func: (T) -> R): Array<R> = Array<R>(size) { func(this[it]) }
