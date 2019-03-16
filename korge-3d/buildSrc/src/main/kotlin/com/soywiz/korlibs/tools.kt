package com.soywiz.korlibs

import groovy.util.Node
import groovy.xml.XmlUtil
import org.gradle.api.*
import org.gradle.api.artifacts.dsl.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import java.io.*

class MultiOutputStream(val outs: List<OutputStream>) : OutputStream() {
    override fun write(b: Int) = run { for (out in outs) out.write(b) }
    override fun write(b: ByteArray, off: Int, len: Int) = run { for (out in outs) out.write(b, off, len) }
    override fun flush() = run { for (out in outs) out.flush() }
    override fun close() = run { for (out in outs) out.close() }
}

// Extensions
operator fun File.get(name: String) = File(this, name)

var File.text get() = this.readText(); set(value) = run { this.also { it.parentFile.mkdirs() }.writeText(value) }

// Gradle extensions
operator fun Project.invoke(callback: Project.() -> Unit) = callback(this)
operator fun DependencyHandler.invoke(callback: DependencyHandler.() -> Unit) = callback(this)
operator fun KotlinMultiplatformExtension.invoke(callback: KotlinMultiplatformExtension.() -> Unit) = callback(this)
fun Project.tasks(callback: TaskContainer.() -> Unit) = this.tasks.apply(callback)

operator fun <T> NamedDomainObjectCollection<T>.get(name: String) = this.getByName(name)
val NamedDomainObjectCollection<KotlinTarget>.js get() = this["js"] as KotlinOnlyTarget<KotlinJsCompilation>
val NamedDomainObjectCollection<KotlinTarget>.jvm get() = this["jvm"] as KotlinOnlyTarget<KotlinJvmCompilation>
val NamedDomainObjectCollection<KotlinTarget>.metadata get() = this["metadata"] as KotlinOnlyTarget<KotlinCommonCompilation>
val <T : KotlinCompilation<*>> NamedDomainObjectContainer<T>.main get() = this["main"]
val <T : KotlinCompilation<*>> NamedDomainObjectContainer<T>.test get() = this["test"]

inline fun <reified T : Task> TaskContainer.create(name: String, callback: T.() -> Unit) = create(name, T::class.java).apply(callback)

val Project.gkotlin get() = extensions.getByType(KotlinMultiplatformExtension::class.java)
fun Project.gkotlin(callback: KotlinMultiplatformExtension.() -> Unit) = gkotlin.apply(callback)

val Project.kotlin get() = extensions.getByType(KotlinMultiplatformExtension::class.java)
fun Project.kotlin(callback: KotlinMultiplatformExtension.() -> Unit) = gkotlin.apply(callback)

// Groovy tools
fun Node.toXmlString() = XmlUtil.serialize(this)
