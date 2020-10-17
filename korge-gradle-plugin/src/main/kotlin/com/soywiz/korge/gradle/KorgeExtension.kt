package com.soywiz.korge.gradle

import com.soywiz.korge.gradle.bundle.KorgeBundles
import com.soywiz.korge.gradle.targets.desktop.DESKTOP_NATIVE_TARGETS
import com.soywiz.korge.gradle.util.*
import com.sun.net.httpserver.*
import org.gradle.api.*
import java.io.*
import groovy.text.*
import org.gradle.api.artifacts.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import java.net.*
import java.time.*
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.reflect.*

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
	internal fun init() {
		// Do nothing, but serves to be referenced to be installed
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

	var exeBaseName: String = "app"

	var name: String = "unnamed"
	var description: String = "description"
	var orientation: Orientation = Orientation.DEFAULT

	var copyright: String = "Copyright (c) ${Year.now().getValue()} Unknown"

	var supressWarnings: Boolean = false

	var authorName = "unknown"
	var authorEmail = "unknown@unknown"
	var authorHref = "http://localhost"

	val nativeEnabled = (project.findProperty("disable.kotlin.native") != "true") && (System.getenv("DISABLE_KOTLIN_NATIVE") != "true")

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

	fun supportBox2d() {
		dependencyMulti("com.soywiz.korlibs.korge:korge-box2d:${BuildVersions.KORGE}", registerPlugin = false)
	}

	fun admob(ADMOB_APP_ID: String) {
        plugin("com.soywiz.korlibs.korge:korge-admob:${project.korgeVersion}", mapOf("ADMOB_APP_ID" to ADMOB_APP_ID))
    }

    fun gameServices() {
        plugin("com.soywiz.korlibs.korge:korge-services:${project.korgeVersion}")
    }

    fun billing() {
        plugin("com.soywiz.korlibs.korge:korge-billing:${project.korgeVersion}")
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
