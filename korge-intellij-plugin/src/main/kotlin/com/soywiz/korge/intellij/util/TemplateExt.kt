package com.soywiz.korge.intellij.util

import org.apache.velocity.*
import org.apache.velocity.app.*
import org.apache.velocity.runtime.*
import org.apache.velocity.runtime.log.*
import java.io.*

fun renderTemplate(template: String, info: Map<String, Any?>): String {
	val writer = StringWriter()
	val velocityEngine = VelocityEngine()
	//velocityEngine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, NullLogChute::class.java.name)
	velocityEngine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, SystemLogChute::class.java.name)
	velocityEngine.init()
	velocityEngine.evaluate(VelocityContext(info), writer, "", template)
	return writer.toString()
}
