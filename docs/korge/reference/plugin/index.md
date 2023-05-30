---
layout: default
title: "Plugins"
title_prefix: KorGE
fa-icon: fa-plug
priority: 900
#status: new
---

KorGE allows to create compile-time extensions to process resources and to do special stuff
like describing the `AndroidManifest.xml` programmatically by just including a library.

## `ResourceProcessor` plugin

The `ResourceProcessor` plugin, allows to process a resource, by generating another file out of it.
It is usually used to reduce dependencies, to simplify what the game has to do for loading a file,
or to group several resources.

For example, when including the `korge-swf` library, the `swf` vectorial files will be processed
and converted into `ani` files, that are simpler and have all the graphics rasterized. 

```kotlin
open class SwfResourceProcessor : ResourceProcessor("swf") {
	companion object : SwfResourceProcessor()

	override val version: Int = AniFile.VERSION
	override val outputExtension: String = "ani"

	override suspend fun processInternal(inputFile: VfsFile, outputFile: VfsFile) {
		val viewsLog = ViewsLog(coroutineContext)
		val lib = inputFile.readSWF(viewsLog.views)
		val config = lib.swfExportConfig
		lib.writeTo(outputFile, config.toAnLibrarySerializerConfig(compression = 1.0))
	}
}
```

To create a plugin of this kind in an external korge library, you have to:

* Create a `src/jvmMain/kotlin` folder and place the `ResourceProcessor` subclass there (since ResourceProcessor is just defined on korge-jvm).
* Create a `src/jvmMain/resources/META-INF/services/com.soywiz.korge.resources.ResourceProcessor` with a line including the fqname of your class. This is usually assisted by IntelliJ IDEA.

Then you have to compile your artifact and use it in another project (since the plugin is loaded from a class loader, you won't be able to use it in that same project).

```kotlin
korge {
    dependencyMulti("com.your.group:artifact-base-name:artifact-version")
}
```

## `KorgePluginExtension` plugin

In some cases, you will need to configure extra stuff like the `AndroidManifest.xml`. 

```kotlin
class AdmobKorgePluginExtension : KorgePluginExtension(
	AdmobKorgePluginExtension::ADMOB_APP_ID
) {
	lateinit var ADMOB_APP_ID: String

	override fun getAndroidInit(): String? =
		"""try { com.google.android.gms.ads.MobileAds.initialize(com.soywiz.korio.android.androidContext(), ${ADMOB_APP_ID.quoted}) } catch (e: Throwable) { e.printStackTrace() }"""

	override fun getAndroidManifestApplication(): String? =
		"""<meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" android:value=${ADMOB_APP_ID.quoted} >"""

	override fun getAndroidDependencies() =
		listOf("com.google.android.gms:play-services-ads:16.0.0")
}
```

To create this kind of plugin, check the ResourcesProcessor plugin section, but you will have to create the following file and include the fqname of your class:

`src/jvmMain/resources/META-INF/services/com.soywiz.korge.plugin.KorgePluginExtension`
