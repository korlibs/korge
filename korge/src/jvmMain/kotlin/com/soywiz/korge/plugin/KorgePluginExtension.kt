package com.soywiz.korge.plugin

import kotlin.reflect.*

open class KorgePluginExtension(
	@Suppress("unused")
	vararg val params: KMutableProperty1<out KorgePluginExtension, String>
) {
	class InitContext()

	open fun init(context: InitContext) {
	}

	open fun getAndroidInit(): String? = null
	open fun getAndroidManifestApplication(): String? = null
	open fun getAndroidDependencies(): List<String> = listOf()
	open fun getCordovaPlugins(): List<CordovaPlugin> = listOf()

	data class CordovaPlugin(val name: String, val args: Map<String, String>)
}
