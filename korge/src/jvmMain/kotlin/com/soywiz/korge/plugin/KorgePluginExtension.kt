package com.soywiz.korge.plugin

import kotlin.reflect.*

open class KorgePluginExtension(
	@Suppress("unused")
	vararg val params: KMutableProperty1<out KorgePluginExtension, String>
) {
	class InitContext()

	open fun init(context: InitContext) {
	}

    open fun initProps(props: Map<String, String>) {
        for (param in params) {
            val propName = param.name
            val prop = props[propName]
            if (prop != null) {
                @Suppress("UNCHECKED_CAST")
                (param as KMutableProperty1<KorgePluginExtension, String>).set(this, prop)
            } else {
                error("Must set the custom property '$propName' for plugin '${this::class.qualifiedName}'. korge { config(\"$propName\", \"...\") }")
            }
        }
    }

	open fun getAndroidInit(): String? = null
	open fun getAndroidManifestApplication(): String? = null
	open fun getAndroidDependencies(): List<String> = listOf()

	data class CordovaPlugin(val name: String, val args: Map<String, String>)
}
