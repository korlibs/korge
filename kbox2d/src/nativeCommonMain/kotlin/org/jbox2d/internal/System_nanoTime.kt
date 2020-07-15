package org.jbox2d.internal

actual fun System_nanoTime(): Long = kotlin.system.getTimeNanos()

