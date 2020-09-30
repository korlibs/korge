package com.soywiz.korge.gradle

class KorgePluginExtensions {
    /*
    val pluginExts: List<KorgePluginExtension> by lazy {
        val exts = ServiceLoader.load(KorgePluginExtension::class.java, classLoader).toList()
        val ctx = KorgePluginExtension.InitContext()
        for (ext in exts) {
            for (param in ext.params) {
                @Suppress("UNCHECKED_CAST")
                (param as KMutableProperty1<KorgePluginExtension, String>).set(ext, globalParams[param.name]!!)
            }
            ext.init(ctx)
        }
        exts
    }
    */

    fun getAndroidDependencies(): List<String> {
        // pluginExts.flatMap { it.getAndroidDependencies() }
        //TODO()
        return listOf()
    }

    fun getAndroidManifestApplication(): List<String> = listOf()
    fun getAndroidInit(): List<String> = listOf()

}
