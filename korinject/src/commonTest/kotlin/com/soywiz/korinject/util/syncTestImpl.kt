package com.soywiz.korinject.util

expect fun syncTestImpl(ignoreJs: Boolean, block: suspend () -> Unit): Unit
