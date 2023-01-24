package com.soywiz.korge.gradle.util

import groovy.lang.*
import org.gradle.api.*
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.tasks.TaskContainer

operator fun <T> Project.invoke(callback: Project.() -> T): T = callback(this)
operator fun <T> DependencyHandler.invoke(callback: DependencyHandler.() -> T): T = callback(this)

open class LambdaClosure<T, TR>(val lambda: (value: T) -> TR) : Closure<T>(Unit) {
	@Suppress("unused")
	fun doCall(vararg arguments: T) = lambda(arguments[0])

	override fun getProperty(property: String): Any = "lambda"
}

//inline class TaskName(val name: String)

inline fun <reified T : Task> TaskContainer.create(name: String, callback: T.() -> Unit = {}) = create(name, T::class.java).apply(callback)
inline fun <reified T : Task> TaskContainer.createTyped(name: String, callback: T.() -> Unit = {}) = create(name, T::class.java).apply(callback)


inline fun ignoreErrors(action: () -> Unit) {
	try {
		action()
	} catch (e: Throwable) {
		e.printStackTrace()
	}
}

fun <T> Project.getIfExists(name: String): T? = if (this.hasProperty(name)) this.property(name) as T else null

operator fun <T> NamedDomainObjectSet<T>.get(key: String): T = this.getByName(key)
