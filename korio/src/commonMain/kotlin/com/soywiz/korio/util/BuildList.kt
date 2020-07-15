package com.soywiz.korio.util

inline fun <T> buildList(callback: ArrayList<T>.() -> Unit): List<T> = arrayListOf<T>().apply(callback)
