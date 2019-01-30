package com.soywiz.korge.gradle

import org.gradle.api.*
import java.io.*

enum class Orientation(val lc: String) { DEFAULT("default"), LANDSCAPE("landscape"), PORTRAIT("portrait") }

data class KorgePluginDescriptor(val name: String, val args: Map<String, String>, val version: String?)

enum class GameCategory {
	ACTION, ADVENTURE, ARCADE, BOARD, CARD,
	CASINO, DICE, EDUCATIONAL, FAMILY, KIDS,
	MUSIC, PUZZLE, RACING, ROLE_PLAYING, SIMULATION,
	SPORTS, STRATEGY, TRIVIA, WORD
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
	val plugins = arrayListOf<KorgePluginDescriptor>()

	var copyright: String = "Copyright (c) 2019 Unknown"

	var authorName = "unknown"
	var authorEmail = "unknown@unknown"
	var authorHref = "http://localhost"

	val icon: File? = File("icon.png")

	var gameCategory: GameCategory? = null

	var fullscreen = true

	var backgroundColor: Int = 0xff000000.toInt()

	var appleDevelopmentTeamId: String? = java.lang.System.getenv("DEVELOPMENT_TEAM")
		?: java.lang.System.getProperty("appleDevelopmentTeamId")?.toString()
		?: project.findProperty("appleDevelopmentTeamId")?.toString()

	var appleOrganizationName = "User Name Name"

	var entryPoint: String = "main"

	var androidMinSdk: String? = null
	internal var _androidAppendBuildGradle: String? = null

	@JvmOverloads
	fun cordovaPlugin(name: CharSequence, args: Map<String, String> = mapOf(), version: CharSequence? = null) {
		plugins += KorgePluginDescriptor(name.toString(), args, version?.toString())
		//println("cordovaPlugin($name, $args, $version)")
	}

	fun androidAppendBuildGradle(str: String) {
		if (_androidAppendBuildGradle == null) {
			_androidAppendBuildGradle = ""
		}
		_androidAppendBuildGradle += str
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
}
