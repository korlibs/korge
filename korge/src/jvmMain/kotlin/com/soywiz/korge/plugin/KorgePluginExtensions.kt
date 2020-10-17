package com.soywiz.korge.plugin

import java.util.*

@Suppress("unused")
object KorgePluginExtensions {
    @JvmStatic
    fun getAndroidInfo(classLoader: ClassLoader, props: Map<String, String>): Map<String, Any> {
        val plugins = ServiceLoader.load(KorgePluginExtension::class.java, classLoader).toList().filterNotNull()
        for (plugin in plugins) plugin.initProps(props)
        val androidInit = plugins.mapNotNull { it.getAndroidInit() }
        val androidManifest = plugins.mapNotNull { it.getAndroidManifestApplication() }
        val androidDependencies = plugins.flatMap { it.getAndroidDependencies() }
        return mapOf(
            "androidInit" to androidInit,
            "androidManifest" to androidManifest,
            "androidDependencies" to androidDependencies,
        )
    }
}
