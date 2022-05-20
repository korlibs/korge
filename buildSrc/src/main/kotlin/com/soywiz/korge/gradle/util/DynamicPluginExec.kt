package com.soywiz.korge.gradle.util

import java.io.File
import java.net.URLClassLoader

fun executeInPlugin(classPaths: Iterable<File>, className: String, methodName: String, throws: Boolean = false, args: (ClassLoader) -> List<Any?>): Any? {
    val classPaths = classPaths.toList().map { it.toURL() }
    //println(classPaths)
    return URLClassLoader(classPaths.toTypedArray(), ClassLoader.getSystemClassLoader()).use { classLoader ->
        val clazz = classLoader.loadClass(className)
        try {
            clazz.methods.first { it.name == methodName }.invoke(null, *args(classLoader).toTypedArray())
        } catch (e: java.lang.reflect.InvocationTargetException) {
            val re = (e.targetException ?: e)
            if (throws) throw re
            re.printStackTrace()
            System.err.println(re.toString())
            null
        }
    }.also {
        System.gc()
    }
}
