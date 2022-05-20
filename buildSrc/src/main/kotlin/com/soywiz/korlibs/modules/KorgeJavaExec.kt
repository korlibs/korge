package com.soywiz.korlibs.modules

import com.soywiz.korge.gradle.targets.isLinux
import com.soywiz.korge.gradle.targets.isMacos
import com.soywiz.korlibs.kotlin
import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.provideDelegate
import com.soywiz.korge.gradle.targets.jvm.JvmAddOpens

open class KorgeJavaExec : JavaExec() {
    private val jvmCompilation by lazy { project.kotlin.targets.getByName("jvm").compilations as NamedDomainObjectSet<*> }
    private val mainJvmCompilation by lazy { jvmCompilation.getByName("main") as org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation }

    @get:InputFiles
    val korgeClassPath by lazy {
        mainJvmCompilation.runtimeDependencyFiles + mainJvmCompilation.compileDependencyFiles + mainJvmCompilation.output.allOutputs + mainJvmCompilation.output.classesDirs
    }

    override fun exec() {
        classpath = korgeClassPath
        super.exec()
    }

    init {
        systemProperties = (System.getProperties().toMutableMap() as MutableMap<String, Any>) - "java.awt.headless"
        if (!JvmAddOpens.beforeJava9) jvmArgs(*JvmAddOpens.createAddOpensTypedArray())
        //project.afterEvaluate {
            //if (firstThread == true && OS.isMac) task.jvmArgs("-XstartOnFirstThread")
        //}
    }
}
