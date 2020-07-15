package com.soywiz.korio.lang

inline fun assert(cond: Boolean): Unit {
	if (!cond) throw AssertionError()
}