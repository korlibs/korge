package com.soywiz.korge.gradle

import com.moowork.gradle.node.*
import com.moowork.gradle.node.npm.*
import com.soywiz.kds.*
import com.soywiz.korge.gradle.targets.desktop.DESKTOP_NATIVE_TARGETS
import com.soywiz.korge.gradle.util.*
import com.soywiz.korge.plugin.*
import com.soywiz.korio.util.*
import org.gradle.api.*
import java.io.*
import groovy.text.*
import org.gradle.api.artifacts.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import java.net.*
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.reflect.*


enum class Orientation(val lc: String) { DEFAULT("default"), LANDSCAPE("landscape"), PORTRAIT("portrait") }

data class KorgeCordovaPluginDescriptor(val name: String, val args: Map<String, String>, val version: String?)

class KorgePluginsContainer(val project: Project, val parentClassLoader: ClassLoader = KorgePluginsContainer::class.java.classLoader) {
    val globalParams = LinkedHashMap<String, String>()
	val plugins = LinkedHashMap<MavenLocation, KorgePluginDescriptor>()

    val files by lazy { project.resolveArtifacts(*plugins.values.map { it.jvmArtifact }.toTypedArray()) }
    val urls by lazy { files.map { it.toURI().toURL() } }
    val classLoader by lazy {
		//println("KorgePluginsContainer.classLoader: $urls")
		URLClassLoader(urls.toTypedArray(), parentClassLoader)
	}

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

	fun addPlugin(artifact: MavenLocation): KorgePluginDescriptor {
		return plugins.getOrPut(artifact) { KorgePluginDescriptor(this, artifact) }
	}
}

data class KorgePluginDescriptor(val container: KorgePluginsContainer, val artifact: MavenLocation, val args: LinkedHashMap<String, String> = linkedHashMapOf()) {
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

	var id: String = "com.unknown.unknownapp"
	var version: String = "0.0.1"

	var exeBaseName: String = "app"

	var name: String = "unnamed"
	var description: String = "description"
	var orientation: Orientation = Orientation.DEFAULT
	val cordovaPlugins = arrayListOf<KorgeCordovaPluginDescriptor>()

	var copyright: String = "Copyright (c) 2019 Unknown"

	var authorName = "unknown"
	var authorEmail = "unknown@unknown"
	var authorHref = "http://localhost"

	var icon: File? = project.projectDir["icon.png"]

	var gameCategory: GameCategory? = null

	var fullscreen = true

	var backgroundColor: Int = 0xff000000.toInt()

	var appleDevelopmentTeamId: String? = java.lang.System.getenv("DEVELOPMENT_TEAM")
		?: java.lang.System.getProperty("appleDevelopmentTeamId")?.toString()
		?: project.findProperty("appleDevelopmentTeamId")?.toString()

	var appleOrganizationName = "User Name Name"

	var entryPoint: String = "main"
	var jvmMainClassName: String = "MainKt"

	var androidMinSdk: String? = null
	internal var _androidAppendBuildGradle: String? = null

	@JvmOverloads
	fun cordovaPlugin(name: CharSequence, args: Map<String, String> = mapOf(), version: CharSequence? = null) {
		project.logger.info("Korge.cordovaPlugin(name=$name, args=$args, version=$version)")
		cordovaPlugins += KorgeCordovaPluginDescriptor(name.toString(), args, version?.toString())
		//println("cordovaPlugin($name, $args, $version)")
	}

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

    fun plugin(name: String, args: Map<String, String> = mapOf()) {
		dependencyMulti(name, registerPlugin = false)
        plugins.addPlugin(MavenLocation(name)).addArgs(args)
    }

	internal val defaultPluginsClassLoader by lazy { plugins.classLoader }

	fun supportExperimental3d() {
		dependencyMulti("com.soywiz.korlibs.korge:korge-3d:${BuildVersions.KORGE}")
	}

	fun supportSwf() {
		dependencyMulti("com.soywiz.korlibs.korge:korge-swf:${BuildVersions.KORGE}")
	}

    fun supportShape() {
        dependencyMulti("com.soywiz.korlibs.korma:korma-shape:${BuildVersions.KORMA}")
    }

    fun supportShapeOps() = supportShape()
	fun supportTriangulation() = supportShape()

	fun supportDragonbones() {
		dependencyMulti("com.soywiz.korlibs.korge:korge-dragonbones:${BuildVersions.KORGE}")
	}

	fun supportBox2d() {
		dependencyMulti("com.soywiz.korlibs.korge:korge-box2d:${BuildVersions.KORGE}")
	}

	fun supportMp3() = Unit
	fun supportOggVorbis() = Unit

	fun supportQr() {
		dependencyMulti("com.soywiz.korlibs.korim:korim-qr:${BuildVersions.KORIM}")
	}

	fun supportJpeg() {
		dependencyMulti("com.soywiz.korlibs.korim:korim-jpeg:${BuildVersions.KORIM}")
	}

	fun admob(ADMOB_APP_ID: String) {
        plugin("com.soywiz.korlibs.korge:korge-admob:${project.korgeVersion}", mapOf("ADMOB_APP_ID" to ADMOB_APP_ID))
    }

	fun cordovaUseCrosswalk() {
		// Required to have webgl on android emulator?
		// https://crosswalk-project.org/documentation/cordova.html
		// https://github.com/crosswalk-project/cordova-plugin-crosswalk-webview/issues/205#issuecomment-371669478
		if (androidMinSdk == null) androidMinSdk = "20"
		cordovaPlugin("cordova-plugin-crosswalk-webview", version = "2.4.0")
		androidAppendBuildGradle("""
        	configurations.all {
        		resolutionStrategy {
        			force 'com.android.support:support-v4:27.1.0'
        		}
        	}
        """)
	}

	@JvmOverloads
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

	val ALL_NATIVE_TARGETS = listOf("iosArm64", "iosArm32", "iosX64") + DESKTOP_NATIVE_TARGETS
	//val ALL_TARGETS = listOf("android", "js", "jvm", "metadata") + ALL_NATIVE_TARGETS
	val ALL_TARGETS = listOf("js", "jvm", "metadata") + ALL_NATIVE_TARGETS

	@JvmOverloads
	fun dependencyMulti(group: String, name: String, version: String, targets: List<String> = ALL_TARGETS, suffixCommonRename: Boolean = false, androidIsJvm: Boolean = false): Unit = project {
		dependencies {
            /*
			loop@for (target in targets) {
				if (!OS.isMac){
					when (target) {
						"iosArm64", "iosArm32", "iosX64", "macosX64" -> continue@loop
					}
				}
				if (!OS.isLinux){
					when (target) {
						"linuxX64" -> continue@loop
					}
				}

				val base = when (target) {
					"metadata" -> "common"
					else -> target
				}
				val suffix = when {
					target == "android" && androidIsJvm -> "-jvm"
					target == "metadata" && suffixCommonRename -> "-common"
					else -> "-${target.toLowerCase()}"
				}

				val packed = "$group:$name$suffix:$version"
				add("${base}MainApi", packed)
				add("${base}TestImplementation", packed)
			}
             */
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

	@JvmOverloads
	fun dependencyNodeModule(name: String, version: String) = project {
		val node = extensions.getByType(NodeExtension::class.java)

		val installNodeModule = tasks.create<NpmTask>("installJs${name.capitalize()}") {
			onlyIf { !File(node.nodeModulesDir, name).exists() }
			setArgs(arrayListOf("install", "$name@$version"))
		}

		tasks.getByName("jsTestNode").dependsOn(installNodeModule)
	}

	data class CInteropTargets(val name: String, val targets: List<String>)

	val cinterops = arrayListOf<CInteropTargets>()


	@JvmOverloads
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

}

// println(project.resolveArtifacts("com.soywiz:korge-metadata:1.0.0"))
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
