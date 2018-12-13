package com.soywiz.korge.gradle

import java.io.*
import java.net.*

object KorgeBuildServiceProxy {
    private val korgeClassLoader = (com.soywiz.korge.build.swf.SwfResourceProcessor::class.java.classLoader as URLClassLoader)
    private val rclassLoader = URLClassLoader(korgeClassLoader.urLs, ClassLoader.getSystemClassLoader())
    private val myClass = rclassLoader.loadClass(com.soywiz.korge.build.KorgeBuildService::class.java.name)
    private val service = myClass.constructors.first().newInstance()
    private val initMethod = service.javaClass.declaredMethods.first { it.name == "init" }
    private val processResourcesFolderMethod = service.javaClass.declaredMethods.first { it.name == "processResourcesFolder" }
    private val versionMethod = service.javaClass.declaredMethods.first { it.name == "version" }

    fun init() {
        initMethod.invoke(service)
    }

    fun version(): String {
        return versionMethod.invoke(service) as String
    }

    fun processResourcesFolder(src: File, dst: File) {
        processResourcesFolderMethod.invoke(service, src, dst)
    }
}
