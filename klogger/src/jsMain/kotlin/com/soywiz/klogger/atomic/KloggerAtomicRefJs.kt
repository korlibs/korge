package com.soywiz.klogger.atomic

actual fun <T> kloggerAtomicRef(initial: T): KloggerAtomicRef<T> = object : KloggerAtomicRef<T>() {
    override var value: T = initial
}
