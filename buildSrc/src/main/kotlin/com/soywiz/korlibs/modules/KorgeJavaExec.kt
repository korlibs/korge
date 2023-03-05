package com.soywiz.korlibs.modules

import com.soywiz.korlibs.kotlin
import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.JavaExec
import com.soywiz.korge.gradle.targets.jvm.JvmAddOpens
import org.gradle.api.file.*

open class KorgeJavaExec : JavaExec() {
    private val jvmCompilation get() = project.kotlin.targets.getByName("jvm").compilations as NamedDomainObjectSet<*>
    private val mainJvmCompilation get() = jvmCompilation.getByName("main") as org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation

    @get:InputFiles
    val korgeClassPath: FileCollection = mainJvmCompilation.runtimeDependencyFiles + mainJvmCompilation.compileDependencyFiles + mainJvmCompilation.output.allOutputs + mainJvmCompilation.output.classesDirs

    override fun exec() {
        systemProperties = (System.getProperties().toMutableMap() as MutableMap<String, Any>) - "java.awt.headless"
        if (!JvmAddOpens.beforeJava9) jvmArgs(*JvmAddOpens.createAddOpensTypedArray())
        classpath = korgeClassPath
        super.exec()
        //project.afterEvaluate {
        //if (firstThread == true && OS.isMac) task.jvmArgs("-XstartOnFirstThread")
        //}
    }
}
