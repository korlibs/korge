package com.soywiz.korinject.util

fun suspendTest(block: suspend () -> Unit): Unit = syncTestImpl(ignoreJs = false, block = block)
fun suspendTestIgnoreJs(block: suspend () -> Unit): Unit = syncTestImpl(ignoreJs = true, block = block)
