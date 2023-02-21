package com.soywiz.korio.util

inline fun <T> Result<T>.getOrNullLoggingError(): T? {
    this.exceptionOrNull()?.printStackTrace()
    return getOrNull()
}
