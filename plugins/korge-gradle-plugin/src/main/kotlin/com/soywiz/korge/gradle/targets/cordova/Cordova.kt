package com.soywiz.korge.gradle.targets.cordova

import com.moowork.gradle.node.npm.*
import com.moowork.gradle.node.task.*
import com.soywiz.korge.gradle.*
import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.targets.js.*
import com.soywiz.korge.gradle.util.*
import com.soywiz.korge.gradle.util.QXml
import com.soywiz.korge.gradle.util.get
import org.gradle.api.*
import org.gradle.api.tasks.*
import java.io.*

private val Project.cordovaFolder get() = buildDir["cordova"]
private val Project.cordovaConfigXmlFile get() = cordovaFolder["config.xml"].ensureParents()
private val Project.cordova_bin get() = node_modules["cordova/bin/cordova"]

fun Project.configureCordova() {
	val jsInstallCordova = project.addTask<NpmTask>("jsInstallCordova") { task ->
		task.onlyIf { !node_modules["/cordova"].exists() }
		task.setArgs(listOf("install", "cordova@8.1.2"))
	}

	fun synchronizeCordovaXmlAndIcons() {
		cordovaFolder.mkdirs()
		val md5File = cordovaFolder["icon.png.md5"]
		val bytes = korge.getIconBytes()
		val md5Actual = bytes.md5String()
		val md5Old = md5File.takeIf { it.exists() }?.readText()
		if (md5Old != md5Actual) {
			project.logger.info("Cordova ICONS md5 not matching, regenerating...")
			cordovaFolder["icon.png"].writeBytes(bytes)
			for (size in com.soywiz.korge.gradle.targets.ICON_SIZES) {
				cordovaFolder["icon-$size.png"].writeBytes(korge.getIconBytes(size))
			}
			md5File.writeText(md5Actual)
		} else {
			project.logger.info("Cordova ICONS already up-to-date")
		}
		korge.updateCordovaXmlFile(cordovaConfigXmlFile)
	}

	fun NodeTask.setCordova(vararg args: String) {
		setWorkingDir(cordovaFolder)
		setScript(cordova_bin)
		setArgs(listOf(*args))
	}

	val cordovaCreate = project.addTask<NodeTask>("cordovaCreate", dependsOn = listOf("jsInstallCordova")) { task ->
		task.onlyIf { !cordovaFolder.exists() }
		task.doFirst {
			buildDir.mkdirs()
		}
		task.setCordova("create", cordovaFolder.absolutePath, "com.soywiz.sample1", "sample1")
		task.setWorkingDir(project.projectDir)
	}

	val cordovaUpdateIcon = project.addTask<Task>("cordovaUpdateIcon", dependsOn = listOf(cordovaCreate)) { task ->
		task.doLast {
			synchronizeCordovaXmlAndIcons()
		}
	}

	val cordovaPluginsList =
		project.addTask<DefaultTask>("cordovaPluginsList", dependsOn = listOf(cordovaCreate)) { task ->
			task.doLast {
				println("name: ${korge.name}")
				println("description: ${korge.description}")
				println("orientation: ${korge.orientation}")
				println("plugins: ${korge.cordovaPlugins}")
			}
		}

	val cordovaSynchronizeConfigXml = project.addTask<DefaultTask>(
		"cordovaSynchronizeConfigXml",
		dependsOn = listOf(cordovaCreate, cordovaUpdateIcon)
	) { task ->
		task.doLast {
			synchronizeCordovaXmlAndIcons()
		}
	}

	val cordovaPluginsInstall =
		project.addTask<Task>("cordovaPluginsInstall", dependsOn = listOf(cordovaCreate)) { task ->
			task.doLast {
				println("korge.plugins: ${korge.cordovaPlugins}")
				for (plugin in korge.cordovaPlugins) {
					val list = plugin.args.flatMap {
						listOf(
							"--variable",
							"${it.key}=${it.value}"
						)
					}.toTypedArray()
					nodeExec(
						cordova_bin, "plugin", "add", plugin.name, "--save", *list,
						workingDir = cordovaFolder
					)
				}
			}
		}

	val cordovaPackageJsWeb = project.addTask<Copy>(
		"packageCordovaJsWeb",
		group = GROUP_KORGE_PACKAGE,
		dependsOn = listOf("jsWebMinWebpack", cordovaCreate, cordovaPluginsInstall, cordovaSynchronizeConfigXml)
	) { task ->
		//afterEvaluate {
		//task.from(project.closure { jsWeb.targetDir })
		task.from(project.closure { webMinWebpackFolder })
		task.into(cordovaFolder["www"])
		//}
		task.doLast {
			val f = cordovaFolder["www/index.html"]
			f.writeText(
				f.readText().replace(
					"</head>",
					"<script type=\"text/javascript\" src=\"cordova.js\"></script></head>"
				)
			)
		}
	}

	val cordovaPackageJsWebNoMinimized = project.addTask<Copy>(
		"packageCordovaJsWebNoMinimized",
		group = GROUP_KORGE_PACKAGE,
		dependsOn = listOf("jsWeb", cordovaCreate, cordovaPluginsInstall, cordovaSynchronizeConfigXml)
	) { task ->
		task.from(project.closure { webFolder })
		task.into(cordovaFolder["www"])
		//}
		task.doLast {
			val f = cordovaFolder["www/index.html"]
			f.writeText(
				f.readText().replace(
					"</head>",
					"<script type=\"text/javascript\" src=\"cordova.js\"></script></head>"
				)
			)
		}
	}

	val cordovaPrepareTargets =
		project.addTask<Task>("cordovaPrepareTargets", dependsOn = listOf(cordovaCreate)) { task ->
			task.doLast {
				if (korge._androidAppendBuildGradle != null) {
					// https://cordova.apache.org/docs/en/8.x/guide/platforms/android/index.html
					val androidFolder = cordovaFolder["platforms/android"]
					if (androidFolder.exists()) {
						androidFolder["build-extras.gradle"].writeText(korge._androidAppendBuildGradle!!)
					}
				}
			}
		}


	for (target in listOf("ios", "android", "browser", "osx", "windows")) {
		val Target = target.capitalize()

		val cordovaTargetInstall =
			project.addTask<NodeTask>("installCordova${Target}", dependsOn = listOf(cordovaCreate)) { task ->
				task.group = GROUP_KORGE_INSTALL
				task.onlyIf { !cordovaFolder["platforms/$target"].exists() }
				doFirst {
					synchronizeCordovaXmlAndIcons()
				}
				task.setCordova("platform", "add", target)
			}

		val compileTarget = project.addTask<NodeTask>(
			"compileCordova$Target",
			dependsOn = listOf(cordovaTargetInstall, cordovaPackageJsWeb, cordovaPrepareTargets)
		) { task ->
			task.setCordova("build", target) // prepare + compile
		}

		val compileTargetRelease = project.addTask<NodeTask>(
			"compileCordova${Target}Release",
			dependsOn = listOf(cordovaTargetInstall, cordovaPackageJsWeb, cordovaPrepareTargets)
		) { task ->
			task.setCordova("build", target, "--release") // prepare + compile
		}

		for (noMinimized in listOf(false, true)) {
			val NoMinimizedText = if (noMinimized) "NoMinimized" else ""

			for (emulator in listOf(false, true)) {
				val EmulatorText = if (emulator) "Emulator" else ""
				val runTarget = project.addTask<NodeTask>("runCordova$Target$EmulatorText$NoMinimizedText",
					group = GROUP_KORGE_RUN,
					dependsOn = listOf(
						cordovaTargetInstall,
						if (noMinimized) cordovaPackageJsWebNoMinimized else cordovaPackageJsWeb,
						cordovaPrepareTargets
					)
				) { task ->
					task.setCordova("run", target, if (emulator) "--emulator" else "--device")
				}
			}
		}
	}
}

private fun KorgeExtension.updateCordovaXml(cordovaConfig: QXml) {
	val korge = this
	cordovaConfig["name"].setValue(korge.name)
	cordovaConfig["description"].setValue(korge.description)

	cordovaConfig.setAttribute("id", korge.id)
	cordovaConfig.setAttribute("version", korge.version)

	cordovaConfig["author"].apply {
		setAttribute("email", korge.authorEmail)
		setAttribute("href", korge.authorHref)
		setValue(korge.authorName)
	}

	fun replaceCordovaPreference(name: String, value: String) {
		// Remove Orientation node and set a new node
		cordovaConfig.getOrAppendNode("preference", "name" to name).setAttribute("value", value)
		cordovaConfig["preference"].list.filter { it.attributes["name"] == name }.forEach { it.remove() }
		cordovaConfig.appendNode("preference", "name" to name, "value" to value)
	}

	// https://cordova.apache.org/docs/es/latest/config_ref/
	replaceCordovaPreference("Orientation", korge.orientation.lc)
	replaceCordovaPreference("Fullscreen", korge.fullscreen.toString())
	replaceCordovaPreference("BackgroundColor", "0x%08x".format(korge.backgroundColor))

	replaceCordovaPreference("DisallowOverscroll", "true")

	if (korge.androidMinSdk != null) {
		val android = cordovaConfig.getOrAppendNode("platform", "name" to "android")
		android.getOrAppendNode("preference", "name" to "android-minSdkVersion")
			.setAttribute("value", korge.androidMinSdk!!)
	}

	cordovaConfig["icon"].remove()
	cordovaConfig.appendNode("icon", "src" to "icon.png")

	val platformIos = cordovaConfig.getOrAppendNode("platform", "name" to "ios")
	for (iconSize in com.soywiz.korge.gradle.targets.ICON_SIZES) {
		platformIos.getOrAppendNode("icon", "width" to "$iconSize", "height" to "$iconSize")
			.setAttribute("src", "icon-$iconSize.png")
	}
}

private fun KorgeExtension.updateCordovaXmlString(cordovaConfig: String): String {
	return updateXml(cordovaConfig) { updateCordovaXml(this) }
}

private fun KorgeExtension.updateCordovaXmlFile(cordovaConfigXmlFile: File) {
	val cordovaConfigXml = if (cordovaConfigXmlFile.exists()) cordovaConfigXmlFile.readText() else """
    	<?xml version='1.0' encoding='utf-8'?>
		<widget id="com.soywiz.sample1" version="1.0.0" xmlns="http://www.w3.org/ns/widgets">
			<name>sample</name>
			<description>sample</description>
			<author email="dev@cordova.apache.org" href="http://cordova.io">Apache Cordova Team</author>
			<content src="index.html" />
			<preference name="Orientation" value="landscape" />
			<preference name="BackgroundColor" value="0xff000000" />
		</widget>
	""".trimIndent().trim()
	val cordovaConfig = QXml(xmlParse(cordovaConfigXml))
	this.updateCordovaXml(cordovaConfig)

	cordovaConfigXmlFile.writeText(cordovaConfig.serialize())

}
