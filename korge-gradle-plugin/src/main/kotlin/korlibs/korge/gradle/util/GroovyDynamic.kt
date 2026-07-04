package korlibs.korge.gradle.util

import groovy.lang.Closure
import org.gradle.api.Project

fun <T> Project.closure(callback: () -> T) = GroovyClosure(this, callback)

fun <T> GroovyClosure(owner: Any?, callback: () -> T): Closure<T> = object : Closure<T>(owner) {
    override fun call(): T = callback()
}
