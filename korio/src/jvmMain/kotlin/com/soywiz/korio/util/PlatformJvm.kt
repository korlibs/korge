package com.soywiz.korio.util

internal actual val rawPlatformName: String = "jvm"
internal actual val rawOsName: String by lazy { System.getProperty("os.name") }
internal actual val rawIsDebug: Boolean get() = java.lang.management.ManagementFactory.getRuntimeMXBean().inputArguments.toString().indexOf("-agentlib:jdwp") > 0
