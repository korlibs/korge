package com.soywiz.korge.gradle.util

import groovy.lang.*
import org.gradle.api.*

open class LambdaClosure<T, TR>(val lambda: (value: T) -> TR) : Closure<T>(Unit) {
	@Suppress("unused")
	fun doCall(vararg arguments: T) = lambda(arguments[0])

	override fun getProperty(property: String): Any = "lambda"
}

//inline class TaskName(val name: String)

inline fun <reified T : Task> Project.addTask(
	name: String,
	group: String = "",
	description: String = "",
	overwrite: Boolean = true,
	dependsOn: List<Any?> = listOf(),
	noinline configure: T.(T) -> Unit = {}
): T {
	return project.task(
		mapOf(
			"type" to T::class.java,
			"group" to group,
			"description" to description,
			"overwrite" to overwrite
		), name, LambdaClosure { it: T ->
			configure(it, it)
		}
	).dependsOn(dependsOn.map { when (it) {
		is Task -> it.name
		else -> it.toString()
	}}) as T
}

inline fun ignoreErrors(action: () -> Unit) {
	try {
		action()
	} catch (e: Throwable) {
		e.printStackTrace()
	}
}

fun <T> Project.getIfExists(name: String): T? = if (this.hasProperty(name)) this.property(name) as T else null

operator fun <T> NamedDomainObjectSet<T>.get(key: String): T = this.getByName(key)
