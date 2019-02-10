package com.soywiz.korge.gradle

import com.moowork.gradle.node.*
import com.moowork.gradle.node.npm.*
import com.soywiz.korge.gradle.targets.desktop.DESKTOP_NATIVE_TARGETS
import com.soywiz.korge.gradle.util.*
import org.gradle.api.*
import java.io.*
import groovy.text.*
import org.gradle.api.artifacts.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*


enum class Orientation(val lc: String) { DEFAULT("default"), LANDSCAPE("landscape"), PORTRAIT("portrait") }

data class KorgeCordovaPluginDescriptor(val name: String, val args: Map<String, String>, val version: String?)
data class KorgePluginDescriptor(val artifact: String, val args: Map<String, String>, val xml: QXml) {
	val variables get() = xml["variables"].attributes.keys.toList()
	val androidInit get() = xml["android"]["init"].text?.replaceGroovy(args)
	val androidManifestApplication get() = xml["android"]["manifest-application"].text?.replaceGroovy(args)
	val androidDependencies get() = xml["android"]["dependencies"].children.mapNotNull { it.text?.replaceGroovy(args) }
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

	val plugins = arrayListOf<KorgePluginDescriptor>()

    fun plugin(name: String, args: Map<String, String> = mapOf()) {
		val xml = QXml(xmlParse(project.resolveArtifacts("$name@korge-plugin").first().readText()))
		val plugin = KorgePluginDescriptor(name, args, xml)
		plugins += plugin
		for (variable in plugin.variables) {
			if (variable !in args) {
				error("When configuring Korge plugin '$name': Variable $variable is expected but not found. Expected variables: ${plugin.variables}")
			}
		}
		project.dependencies.add("commonMainApi", name)

		for ((k, v) in args) {
			config(k, v)
		}

		for (pluginXml in xml["cordova"]["plugins"].children) {
			val pluginName = pluginXml.name
			val pluginVars = pluginXml.attributes.toList().associate { it.first to it.second.replaceGroovy(args) }

			cordovaPlugin(pluginName, pluginVars)

		}

		//println("plugin.androidInit=${plugin.androidInit}")
		//println("plugin.variables=${plugin.variables}")
    }

    fun admob(ADMOB_APP_ID: String) {
        plugin("com.soywiz:korge-admob:${project.korgeVersion}", mapOf("ADMOB_APP_ID" to ADMOB_APP_ID))
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
	fun dependencyMulti(group: String, name: String, version: String, targets: List<String> = ALL_TARGETS, suffixCommonRename: Boolean = false, androidIsJvm: Boolean = false) = project {
		dependencies {
			for (target in targets) {
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
		}
	}

	@JvmOverloads
	fun dependencyMulti(dependency: String, targets: List<String> = ALL_TARGETS) {
		val (group, name, version) = dependency.split(":", limit = 3)
		return dependencyMulti(group, name, version, targets)
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
