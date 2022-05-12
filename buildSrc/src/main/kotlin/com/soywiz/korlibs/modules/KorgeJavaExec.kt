package com.soywiz.korlibs.modules

import com.soywiz.korlibs.kotlin
import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.provideDelegate

val beforeJava9 = System.getProperty("java.version").startsWith("1.")

val javaAddOpens = ArrayList<String>().apply {
    add("--add-opens=java.desktop/sun.java2d.opengl=ALL-UNNAMED")
    add("--add-opens=java.desktop/java.awt=ALL-UNNAMED")
    add("--add-opens=java.desktop/sun.awt=ALL-UNNAMED")
    if (isMacos) {
        add("--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        add("--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED")
    }
    if (isLinux) add("--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED")
}.toTypedArray()

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
        if (!beforeJava9) jvmArgs(*javaAddOpens)
        //project.afterEvaluate {
            //if (firstThread == true && OS.isMac) task.jvmArgs("-XstartOnFirstThread")
        //}
    }
}
