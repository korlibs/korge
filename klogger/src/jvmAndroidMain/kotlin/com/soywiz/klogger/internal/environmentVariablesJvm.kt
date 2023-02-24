package com.soywiz.klogger.internal

internal actual val miniEnvironmentVariables: Map<String, String> by lazy { System.getenv() }
