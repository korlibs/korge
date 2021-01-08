package com.soywiz.korge.gradle

import com.soywiz.korge.gradle.bundle.KorgeBundles
import com.soywiz.korge.gradle.targets.android.*
import com.soywiz.korge.gradle.targets.desktop.*
import com.soywiz.korge.gradle.targets.ios.*
import com.soywiz.korge.gradle.targets.isMacos
import com.soywiz.korge.gradle.targets.js.*
import com.soywiz.korge.gradle.targets.jvm.*
import com.soywiz.korge.gradle.util.*
import org.gradle.api.*
import java.io.*
import groovy.text.*
import org.gradle.api.artifacts.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import java.net.*
import java.time.*
import java.util.*
import kotlin.collections.LinkedHashMap

enum class Orientation(val lc: String) { DEFAULT("default"), LANDSCAPE("landscape"), PORTRAIT("portrait") }

class KorgePluginsContainer(val project: Project, val parentClassLoader: ClassLoader = KorgePluginsContainer::class.java.classLoader) {
    val globalParams = LinkedHashMap<String, String>()
	val plugins = LinkedHashMap<MavenLocation, KorgePluginDescriptor>()

    val files by lazy { project.resolveArtifacts(*plugins.values.map { it.jvmArtifact }.toTypedArray()) }
    val urls by lazy { files.map { it.toURI().toURL() } }
    val classLoader by lazy {
		//println("KorgePluginsContainer.classLoader: $urls")
		URLClassLoader(urls.toTypedArray(), parentClassLoader)
	}

    val pluginExts = KorgePluginExtensions(project)

	fun addPlugin(artifact: MavenLocation): KorgePluginDescriptor {
		return plugins.getOrPut(artifact) { KorgePluginDescriptor(this, artifact) }
	}
}

data class KorgePluginDescriptor(val container: KorgePluginsContainer, val artifact: MavenLocation, val args: LinkedHashMap<String, String> = LinkedHashMap()) {
	val jvmArtifact = artifact.withNameSuffix("-jvm")
    val files by lazy { container.project.resolveArtifacts(jvmArtifact) }
    val urls by lazy { files.map { it.toURI().toURL() } }
    val classLoader by lazy { URLClassLoader(urls.toTypedArray(), container.parentClassLoader) }
	fun addArgs(args: Map<String, String>) = this.apply { this.args.putAll(args) }.apply { container.globalParams.putAll(args) }
}

enum class GameCategory {
	ACTION, ADVENTURE, ARCADE, BOARD, CARD,
	CASINO, DICE, EDUCATIONAL, FAMILY, KIDS,
	MUSIC, PUZZLE, RACING, ROLE_PLAYING, SIMULATION,
	SPORTS, STRATEGY, TRIVIA, WORD
}

fun String.replaceGroovy(replacements: Map<String, Any?>): String {
	//println("String.replaceGroovy: this=$this, replacements=$replacements")
	val templateEngine = SimpleTemplateEngine()
	val template = templateEngine.createTemplate(this)
	val replaced = template.make(replacements.toMutableMap())
	return replaced.toString()
}

data class MavenLocation(val group: String, val name: String, val version: String, val classifier: String? = null) {
	val versionWithClassifier by lazy { buildString {
		append(version)
		if (classifier != null) {
			append(':')
			append(classifier)
		}
	} }

	companion object {
		operator fun invoke(location: String): MavenLocation {
			val parts = location.split(":")
			return MavenLocation(parts[0], parts[1], parts[2], parts.getOrNull(3))
		}
	}

	fun withNameSuffix(suffix: String) = copy(name = "$name$suffix")

	val full: String by lazy { "$group:$name:$versionWithClassifier" }

	override fun toString(): String = full
}

@Suppress("unused")
class KorgeExtension(val project: Project) {
    private var includeIndirectAndroid: Boolean = false
	internal fun init(includeIndirectAndroid: Boolean) {
	    this.includeIndirectAndroid = includeIndirectAndroid
	}

    internal var targets = LinkedHashSet<String>()

    private fun target(name: String, block: () -> Unit) {
        if (!targets.contains(name)) {
            targets.add(name)
            block()
        }
    }

    /**
     * Configures JVM target
     */
    fun targetJvm() {
        target("jvm") {
            project.configureJvm()
        }
    }

    /**
     * Configures JavaScript target
     */
    fun targetJs() {
        target("js") {
            project.configureJavaScript()
        }
    }

    /**
     * Configures Desktop targets depending on the host:
     *
     * - mingwX64
     * - linuxX64
     * - macosX64
     */
    fun targetDesktop() {
        target("desktop") {
            project.configureNativeDesktop()
        }
    }

    /**
     * Configures Android indirect. Alias for [targetAndroidIndirect]
     */
    fun targetAndroid() {
        targetAndroidIndirect()
    }

    /**
     * Configures android in this project tightly integrated, and creates src/main default stuff
     *
     * Android SDK IS required even if android tasks are not executed.
     */
    fun targetAndroidDirect() {
        target("android") {
            project.configureAndroidDirect()
        }
    }

    /**
     * Configures android as a separate project in: build/platforms/android
     *
     * Android SDK not required if tasks are not executed.
     * The project can be opened on Android Studio.
     */
    fun targetAndroidIndirect() {
        target("android") {
            project.configureAndroidIndirect()
        }
    }

    /**
     * Configures Kotlin/Native iOS target (only on macOS)
     */
    fun targetIos() {
        target("ios") {
            if (isMacos) {
                project.configureNativeIos()
            }
        }
    }

    /**
     * Uses gradle.properties and system environment variables to determine which targets to enable. JVM is always enabled.
     *
     * gradle.properties:
     * - korge.enable.desktop=true
     * - korge.enable.android.indirect=true
     * - korge.enable.android.direct=true
     * - korge.enable.ios=true
     * - korge.enable.js=true
     *
     * Environment Variables:
     * - KORGE_ENABLE_DESKTOP
     * - KORGE_ENABLE_ANDROID_INDIRECT
     * - KORGE_ENABLE_ANDROID_DIRECT
     * - KORGE_ENABLE_ANDROID_IOS
     * - KORGE_ENABLE_ANDROID_JS
     */
    fun targetDefault() {
        if (newDesktopEnabled) targetDesktop()
        if (newAndroidIndirectEnabled) targetAndroidIndirect()
        if (newAndroidDirectEnabled) targetAndroidDirect()
        if (newIosEnabled) targetIos()
        if (newJsEnabled) targetJs()
    }

    /**
     * Configure all the available targets unconditionally.
     */
    fun targetAll() {
        targetJvm()
        targetJs()
        targetDesktop()
        targetAndroid()
        targetIos()
    }

    val bundles = KorgeBundles(project)

    @JvmOverloads
    fun bundle(uri: String, baseName: String? = null) = bundles.bundle(uri, baseName)

    val DEFAULT_JVM_TARGET = "1.8"
    //val DEFAULT_JVM_TARGET = "1.6"
	var jvmTarget: String = project.findProject("jvm.target")?.toString() ?: DEFAULT_JVM_TARGET
	var androidLibrary: Boolean = project.findProperty("android.library") == "true"
    var overwriteAndroidFiles: Boolean = project.findProperty("overwrite.android.files") == "false"
    var id: String = "com.unknown.unknownapp"
	var version: String = "0.0.1"
    var preferredIphoneSimulatorVersion: Int = 8

	var exeBaseName: String = "app"

	var name: String = "unnamed"
	var description: String = "description"
	var orientation: Orientation = Orientation.DEFAULT

	var copyright: String = "Copyright (c) ${Year.now().getValue()} Unknown"

	var supressWarnings: Boolean = false

    /**
     * Determines whether the standard console will be available on Windows or not
     * by setting the windows subsystem to console or windows.
     *
     * When set to null, on debug builds it will include open console, and on release builds it won't open any console.
     */
    var enableConsole: Boolean? = null

	var authorName = "unknown"
	var authorEmail = "unknown@unknown"
	var authorHref = "http://localhost"

	val nativeEnabled = (project.findProperty("disable.kotlin.native") != "true") && (System.getenv("DISABLE_KOTLIN_NATIVE") != "true")

    val newDesktopEnabled get() = project.findProperty("korge.enable.desktop") == "true" || System.getenv("KORGE_ENABLE_DESKTOP") == "true"
    val newAndroidIndirectEnabled get() = project.findProperty("korge.enable.android.indirect") == "true" || System.getenv("KORGE_ENABLE_ANDROID_INDIRECT") == "true"
    val newAndroidDirectEnabled get() = project.findProperty("korge.enable.android.direct") == "true" || System.getenv("KORGE_ENABLE_ANDROID_DIRECT") == "true"
    val newIosEnabled get() = project.findProperty("korge.enable.ios") == "true" || System.getenv("KORGE_ENABLE_IOS") == "true"
    val newJsEnabled get() = project.findProperty("korge.enable.js") == "true" || System.getenv("KORGE_ENABLE_JS") == "true"

    var icon: File? = project.projectDir["icon.png"]
    var banner: File? = project.projectDir["banner.png"]

	var gameCategory: GameCategory? = null

	var fullscreen = true

	var backgroundColor: Int = 0xff000000.toInt()

	var webBindAddress = project.findProperty("web.bind.address")?.toString() ?: "0.0.0.0"
	var webBindPort = project.findProperty("web.bind.port")?.toString()?.toIntOrNull() ?: 0

	var appleDevelopmentTeamId: String? = java.lang.System.getenv("DEVELOPMENT_TEAM")
		?: java.lang.System.getProperty("appleDevelopmentTeamId")?.toString()
		?: project.findProperty("appleDevelopmentTeamId")?.toString()

	var appleOrganizationName = "User Name Name"

	var entryPoint: String? = null
	var jvmMainClassName: String = "MainKt"
	//var proguardObfuscate: Boolean = false
	var proguardObfuscate: Boolean = true

	val realEntryPoint get() = entryPoint ?: (jvmMainClassName.substringBeforeLast('.', "") + ".main").trimStart('.')
	val realJvmMainClassName get() = jvmMainClassName

	val extraEntryPoints = arrayListOf<Entrypoint>()

	class Entrypoint(val name: String, val jvmMainClassName: String) {
		val entryPoint = (jvmMainClassName.substringBeforeLast('.', "") + ".main").trimStart('.')
	}

	fun entrypoint(name: String, jvmMainClassName: String) {
		extraEntryPoints.add(Entrypoint(name, jvmMainClassName))
	}

	var androidMinSdk: Int = 16
	var androidCompileSdk: Int = 28
	var androidTargetSdk: Int = 28

	fun androidSdk(compileSdk: Int, minSdk: Int, targetSdk: Int) {
		androidMinSdk = minSdk
		androidCompileSdk = compileSdk
		androidTargetSdk = targetSdk
	}

	internal var _androidAppendBuildGradle: String? = null

	fun androidAppendBuildGradle(str: String) {
		if (_androidAppendBuildGradle == null) {
			_androidAppendBuildGradle = ""
		}
		_androidAppendBuildGradle += str
	}

	val configs = LinkedHashMap<String, String>()

	fun config(name: String, value: String) {
		configs[name] = value
	}

	val plugins = KorgePluginsContainer(project)
	val androidManifestApplicationChunks = LinkedHashSet<String>()
	val androidManifestChunks = LinkedHashSet<String>()

    fun plugin(name: String, args: Map<String, String> = mapOf()) {
		dependencyMulti(name, registerPlugin = false)
        plugins.addPlugin(MavenLocation(name)).addArgs(args)
    }

	internal val defaultPluginsClassLoader by lazy { plugins.classLoader }

    var androidReleaseSignStoreFile: String = "korge.keystore"
    var androidReleaseSignStorePassword: String = "password"
    var androidReleaseSignKeyAlias: String = "korge"
    var androidReleaseSignKeyPassword: String = "password"

	// Already included in core
	fun supportExperimental3d() = Unit
	fun support3d() = Unit

	//<uses-permission android:name="android.permission.VIBRATE" />
	fun androidManifestApplicationChunk(text: String) {
		androidManifestApplicationChunks += text
	}

	fun androidManifestChunk(text: String) {
		androidManifestChunks += text
	}

	fun androidPermission(name: String) {
		androidManifestChunk("""<uses-permission android:name="$name" />""")
	}

	fun supportVibration() {
		androidPermission("android.permission.VIBRATE")
	}

	fun supportSwf() {
		dependencyMulti("com.soywiz.korlibs.korge:korge-swf:${BuildVersions.KORGE}", registerPlugin = false)
	}

    fun supportShape() {
        dependencyMulti("com.soywiz.korlibs.korma:korma-shape:${BuildVersions.KORMA}", registerPlugin = false)
    }

    fun supportShapeOps() = supportShape()
	fun supportTriangulation() = supportShape()

	fun supportDragonbones() {
		dependencyMulti("com.soywiz.korlibs.korge:korge-dragonbones:${BuildVersions.KORGE}", registerPlugin = false)
	}

    fun supportSpine() {
        dependencyMulti("com.soywiz.korlibs.korge:korge-spine:${BuildVersions.KORGE}", registerPlugin = false)
    }

    fun supportBox2d() {
        // https://awesome.korge.org/
        bundle("https://github.com/korlibs/korge-bundles.git::korge-box2d::4ac7fcee689e1b541849cedd1e017016128624b9##d15f5b28abbc2f58ba375d7204a6db2e90342978d02e675cd29249d0cade8f9f")
	}

	fun admob(ADMOB_APP_ID: String) {
        bundle("https://github.com/korlibs/korge-bundles.git::korge-admob::4ac7fcee689e1b541849cedd1e017016128624b9##2ca2bf24ab19e4618077f57092abfc8c5c8fba50b2797a9c6d0e139cd24d8b35")
        config("ADMOB_APP_ID", ADMOB_APP_ID)
    }

    fun gameServices() {
        bundle("https://github.com/korlibs/korge-bundles.git::korge-services::4ac7fcee689e1b541849cedd1e017016128624b9##392d5ed87428c7137ae40aa7a44f013dd1d759630dca64e151bbc546eb25e28e")
    }

    fun billing() {
        bundle("https://github.com/korlibs/korge-bundles.git::korge-billing::4ac7fcee689e1b541849cedd1e017016128624b9##cbde3d386e8d792855b7ef64e5e700f43b7bb367aedc6a27198892e41d50844b")
    }

    fun author(name: String, email: String, href: String) {
		authorName = name
		authorEmail = email
		authorHref = href
	}

	/////////////////////////////////////////////////
	/////////////////////////////////////////////////


	fun dependencyProject(name: String) = project {
		dependencies {
			add("commonMainApi", project(name))
			add("commonTestImplementation", project(name))
		}
	}

	val ALL_NATIVE_TARGETS = listOf("iosArm64", "iosX64") + DESKTOP_NATIVE_TARGETS
	//val ALL_TARGETS = listOf("android", "js", "jvm", "metadata") + ALL_NATIVE_TARGETS
	val ALL_TARGETS = listOf("js", "jvm", "metadata") + ALL_NATIVE_TARGETS

	@JvmOverloads
	fun dependencyMulti(group: String, name: String, version: String, targets: List<String> = ALL_TARGETS, suffixCommonRename: Boolean = false, androidIsJvm: Boolean = false): Unit = project {
		project.dependencies {
			//println("dependencyMulti --> $group:$name:$version")
            add("commonMainApi", "$group:$name:$version")
		}
        Unit
	}

	@JvmOverloads
	fun dependencyMulti(dependency: String, targets: List<String> = ALL_TARGETS, registerPlugin: Boolean = true) {
		val location = MavenLocation(dependency)
		if (registerPlugin) plugin(location.full)
		return dependencyMulti(location.group, location.name, location.versionWithClassifier, targets)
	}

	/*
	@JvmOverloads
	fun dependencyNodeModule(name: String, version: String) = project {
		val node = extensions.getByType(NodeExtension::class.java)

		val installNodeModule = tasks.create<NpmTask>("installJs${name.capitalize()}") {
			onlyIf { !File(node.nodeModulesDir, name).exists() }
			setArgs(arrayListOf("install", "$name@$version"))
		}

		tasks.getByName("jsTestNode").dependsOn(installNodeModule)
	}
	*/

	data class CInteropTargets(val name: String, val targets: List<String>)

	val cinterops = arrayListOf<CInteropTargets>()


	fun dependencyCInterops(name: String, targets: List<String>) = project {
		cinterops += CInteropTargets(name, targets)
		for (target in targets) {
			((kotlin.targets[target] as AbstractKotlinTarget).compilations["main"] as KotlinNativeCompilation).apply {
				cinterops.apply {
					maybeCreate(name).apply {
					}
				}
			}
		}
	}

	@JvmOverloads
	fun dependencyCInteropsExternal(dependency: String, cinterop: String, targets: List<String> = ALL_NATIVE_TARGETS) {
		dependencyMulti("$dependency:cinterop-$cinterop@klib", targets)
	}

	fun addDependency(config: String, notation: String) {
		val cfg = project.configurations.findByName(config)
		if (cfg == null) {
			// @TODO: Turkish hack. This doesn't seems right. Probably someone messed something up.
			if (config.endsWith("Implementation")) {
				val config2 = config.removeSuffix("Implementation") + "İmplementation"
				println("Can't find config: $config . Trying: $config2 (Turkish hack)")
				return addDependency(config2, notation)
			}

			for (rcfg in project.configurations) {
				println("CONFIGURATION: ${rcfg.name}")
			}
			error("Can't find configuration '$config'")
		}
		project.dependencies.add(config, notation)
	}

}

// println(project.resolveArtifacts("com.soywiz.korlibs.korge:korge-metadata:1.0.0"))
fun Project.resolveArtifacts(vararg artifacts: String): Set<File> {
    val config = project.configurations.detachedConfiguration(
        *artifacts.map {
            (project.dependencies.create(it) as ExternalModuleDependency).apply {
                targetConfiguration = "default"
            }
        }.toTypedArray()
    ).apply {
        isTransitive = false
    }
    return config.files
}

fun Project.resolveArtifacts(vararg artifacts: MavenLocation): Set<File> =
	resolveArtifacts(*artifacts.map { it.full }.toTypedArray())
