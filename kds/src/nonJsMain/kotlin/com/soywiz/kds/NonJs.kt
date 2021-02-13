package com.soywiz.kds

@Suppress("UNCHECKED_CAST")
actual inline fun <T> Any?.fastCastTo(): T = this as T

//actual typealias FastArrayList<E> = ArrayList<E>
