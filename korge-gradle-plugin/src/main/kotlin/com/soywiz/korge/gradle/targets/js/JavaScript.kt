package com.soywiz.korge.gradle.targets.js

import com.soywiz.korge.gradle.*
import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.util.*
import groovy.text.*
import org.gradle.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.tasks.*
import java.io.*
import java.lang.management.*

val Project.node_modules get() = korgeCacheDir["node_modules"]
val Project.webFolder get() = buildDir["web"]
val Project.webMinWebpackFolder get() = buildDir["web-min-webpack"]

internal var _webServer: DecoratedHttpServer? = null

fun Project.configureJavaScript() {
	plugins.apply("kotlin-dce-js")

	val kotlinTargets by lazy { project["kotlin"]["targets"] }

	gkotlin.apply {
		js {
			this.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
			compilations.all {
				it.kotlinOptions.apply {
					languageVersion = "1.3"
					sourceMap = true
					metaInfo = true
					moduleKind = "umd"
					suppressWarnings = korge.supressWarnings
				}
			}

			browser {
				testTask {
					useKarma {
						useChromeHeadless()
					}
				}
			}
		}
	}

	project.korge.addDependency("jsMainImplementation", "org.jetbrains.kotlin:kotlin-stdlib-js")
	project.korge.addDependency("jsTestImplementation", "org.jetbrains.kotlin:kotlin-test-js")

	val compileKotlinJs = project.tasks.getByName("compileKotlinJs") as Kotlin2JsCompile

	//println(compileKotlinJs.outputFile)

	for (minimized in listOf(false, true)) {
		val Min = if (minimized) "Min" else ""
		// jsWeb, jsWebMin

		project.addTask<JsWebCopy>(name = "jsWeb$Min", dependsOn = listOf(compileKotlinJs, "genResources")) { task ->
			task.group = GROUP_KORGE_PACKAGE
			fun CopySpec.configureWeb() {
				if (minimized) {
					//include("**/require.min.js")
					exclude(*excludesAll)
				} else {
					exclude(*excludesNormal)
				}
			}

			task.targetDir = project.buildDir[if (minimized) "web-min" else "web"]

			val kotlinTargets by lazy { project["kotlin"]["targets"] }
			val jsCompilations by lazy { kotlinTargets["js"]["compilations"] }
			val mainJsCompilation = (jsCompilations["main"] as KotlinJsCompilation)

			//println(mainJsCompilation.output.allOutputs.toList())

			//project.afterEvaluate {
			//task.exclude(*excludesNormal)

			inputs.files(compileKotlinJs.outputs)

			task.apply {
				includeEmptyDirs = false

				if (minimized) {
					from((project["runDceJsKotlin"] as KotlinJsDce).destinationDir) { copy -> copy.exclude(*excludesNormal) }
				}
				//from(compileKotlinJs.outputFile)
				//from("${project.buildDir}/npm/node_modules") { copy -> copy.configureWeb() }
				//println("compileKotlinJs.outputFile: ${compileKotlinJs.outputFile}")
				project.afterEvaluate {
					for (file in (jsCompilations["test"]["runtimeDependencyFiles"] as FileCollection).toList()) {
						//println("file: $file")
						if (file.exists() && !file.isDirectory) {
							from(project.zipTree(file.absolutePath)) { copy -> copy.configureWeb() }
							from(project.zipTree(file.absolutePath)) { copy -> copy.include("**/*.min.js") }
						} else {
							from(file) { copy -> copy.configureWeb() }
							from(file) { copy -> copy.include("**/*.min.js") }
						}
					}
				}

				for (target in listOf(kotlinTargets["js"], kotlinTargets["metadata"])) {
					val main = (target["compilations"]["main"] as KotlinCompilation<*>)
					for (sourceSet in main.kotlinSourceSets) {
						//println("sourceSet.resources: ${sourceSet.resources.toList()}")
						from(sourceSet.resources) { copy -> copy.configureWeb() }
					}
				}
				into({ task.targetDir })
			}

			task.doLast {
				val requireMinJsTemplateFile = "require.min.v2.template.js"
				// If a custom version of require.min.js
				val requireMinTemplateJs = when {
					task.targetDir[requireMinJsTemplateFile].exists() -> task.targetDir[requireMinJsTemplateFile].readText()
					else -> getResourceBytes(requireMinJsTemplateFile).toString(Charsets.UTF_8)
				}
				task.targetDir["kotlin.js"].takeIf { it.exists() }?.also { kotlinJsFile ->
					kotlinJsFile.writeText(applyPatchesToKotlinRuntime(kotlinJsFile.readText()))
				}
				task.targetDir["require.min.js"].writeText(requireMinTemplateJs)
				writeTemplateIndexHtml(task.targetDir)
			}
		}
	}

	val jsStopWeb = project.addTask<Task>(name = "jsStopWeb") { task ->
		task.doLast {
			println("jsStopWeb: ${ManagementFactory.getRuntimeMXBean().name}-${Thread.currentThread()}")
			_webServer?.server?.stop(0)
			_webServer = null
		}
	}

	fun runServer(blocking: Boolean) {
		if (_webServer == null) {
			val address = korge.webBindAddress
			val port = korge.webBindPort
			val server = staticHttpServer(project.buildDir["web"], address = address, port = port)
			_webServer = server
			try {
				val openAddress = when (address) {
					"0.0.0.0" -> "127.0.0.1"
					else -> address
				}
				openBrowser("http://$openAddress:${server.port}/index.html")
				if (blocking) {
					while (true) {
						Thread.sleep(1000L)
					}
				}
			} finally {
				if (blocking) {
					server.server.stop(0)
					_webServer = null
				}
			}
		}
		_webServer?.updateVersion?.incrementAndGet()
	}

	val jsWebRun = project.tasks.create<Task>("jsWebRun") {
		dependsOn("jsWeb")
		doLast {
			runServer(!project.gradle.startParameter.isContinuous)
		}
	}

	val jsWebRunNonBlocking = project.tasks.create<Task>("jsWebRunNonBlocking") {
		dependsOn("jsWeb")
		doLast {
			runServer(false)
		}
	}

	project.gradle.addBuildListener(object : BuildAdapter() {
	})
	// In continuous mode this is executed everytime. We need to detect the "Build cancelled." event.
	project.gradle.buildFinished {
		//println("project.gradle.buildFinished!!")
		//_webServer?.server?.stop(0)
		//_webServer = null
	}

	val runJs = project.tasks.create<Task>("runJs") {
		group = GROUP_KORGE_RUN
		dependsOn(jsWebRun)
	}

	val runJsNonBlocking = project.tasks.create<Task>("runJsNonBlocking") {
		group = GROUP_KORGE_RUN
		dependsOn(jsWebRunNonBlocking)
	}

	val jsWebMinWebpack = project.addTask<DefaultTask>("jsWebMinWebpack", dependsOn = listOf("jsBrowserWebpack", "genResources")) { task ->
		group = GROUP_KORGE_PACKAGE
		val jsFileName = "${project.name}.js"
		val jsFile = buildDir["distributions"][jsFileName]
		val jsFileMap = buildDir["distributions"]["$jsFileName.map"]
		inputs.files(jsFile, jsFileMap)
		outputs.dirs(webMinWebpackFolder)
		task.doLast {
			//webMinWebpackFolder2.mkdirs()
			copy { copy ->
				copy.from(jsFile)
				copy.from(jsFileMap)
				for (target in listOf(kotlinTargets["js"], kotlinTargets["metadata"])) {
					val main = (target["compilations"]["main"] as KotlinCompilation<*>)
					for (sourceSet in main.kotlinSourceSets) {
						copy.from(sourceSet.resources) { copy -> copy.exclude(*excludesNormal) }
					}
				}
				copy.into(webMinWebpackFolder)
				//copy.exclude("**/*.js", "**/index.template.html", "**/index.html")
			}
			writeTemplateIndexHtml(webMinWebpackFolder, jsFile.name)
			//println(jsFile)
			//println(jsBrowserWebpack.outputs.files.toList())
		}
	}

	val jsDist = project.addTask<DefaultTask>("jsDist", dependsOn = listOf(jsWebMinWebpack)) { task ->
	}

	//println(compileKotlinJs.outputs.files.toList())
}

private val excludesNormal = arrayOf(
	"**/*.kotlin_metadata", "**/*.kotlin_module", "**/*.kotlin_builtins",
	"META-INF/**",
	"**/linkdata/module",
	"**/*.MF",
	"**/*.meta.js", "**/*.class",
	"**/DebugProbesKt.bin",
	"**/*.kjsm", "**/*.knb", "**/*.knf", "**/*.knm", "**/*.knd", "**/*.knt"
)
private val excludesJs = arrayOf("**/*.js")
private val excludesAll = excludesNormal + excludesJs

private fun Project.writeTemplateIndexHtml(targetDir: File, webpackFile: String? = null) {
	val kotlinJsCompile = tasks.getByName("compileKotlinJs") as Kotlin2JsCompile

	fun readTextFile(name: String): String {
		return when {
			targetDir[name].exists() -> targetDir[name].readText()
			else -> getResourceBytes(name).toString(Charsets.UTF_8)
		}
	}

	val indexTemplateHtml = readTextFile("index.v2.template.html")
	val customCss = readTextFile("custom-styles.template.css")
	val customHtmlHead = readTextFile("custom-html-head.template.html")
	val customHtmlBody = readTextFile("custom-html-body.template.html")

	targetDir["index.html"].writeText(
		SimpleTemplateEngine().createTemplate(indexTemplateHtml).make(
			mapOf(
				"OUTPUT" to kotlinJsCompile.outputFile.nameWithoutExtension,
				"TITLE" to korge.name,
				"CUSTOM_CSS" to customCss,
				"CUSTOM_HTML_HEAD" to customHtmlHead,
				"CUSTOM_HTML_BODY" to customHtmlBody
			)
		).toString().let {
			if (webpackFile != null) it.fixIndexHtmlWebpack(webpackFile) else it
		}
	)

	targetDir["favicon.png"].writeBytes(korge.getIconBytes(16))
	targetDir["appicon.png"].writeBytes(korge.getIconBytes(180)) // https://developer.apple.com/library/archive/documentation/AppleApplications/Reference/SafariWebContent/ConfiguringWebApplications/ConfiguringWebApplications.html
}

private fun String.fixIndexHtmlWebpack(bundleJs: String = "bundle.js"): String {
	val PREFIX = "<!-- GAME_LOAD -->"
	val SUFFIX = "<!-- /GAME_LOAD -->"
	return replace(
		Regex("$PREFIX.*$SUFFIX", RegexOption.DOT_MATCHES_ALL),
		"$PREFIX<script type='text/javascript'>document.onreadystatechange = () => { if (document.readyState === 'complete') { preloader_complete() } };</script><script src='$bundleJs' type='text/javascript'></script>$SUFFIX"
	)
}

fun applyPatchToKotlinRuntime(runtime: String, patchSrc: String, patchDst: String): String {
	val srcRegex = Regex(
		patchSrc
			.trim()
			.replace("\\", "\\\\")
			.replace(".", "\\.")
			.replace("?", "\\?")
			.replace("[", "\\[")
			.replace("(", "\\(")
			.replace("{", "\\{")
			.replace(")", "\\)")
			.replace("}", "\\}")
			.replace("]", "\\]")
			.replace("+", "\\+")
			.replace("*", "\\*")
			.replace("\$", "\\\$")
			.replace("^", "\\^")
			.replace(Regex("\\s+", RegexOption.MULTILINE), "\\\\s*")
		, setOf(
			RegexOption.MULTILINE, RegexOption.IGNORE_CASE
		)
	)
	//println(srcRegex.pattern)
	return runtime.replace(srcRegex) { result ->
		val srcLines = result.value.lines().size
		val dstLines = patchDst.lines().size
		if (srcLines > dstLines) {
			patchDst + "\n".repeat(srcLines - dstLines)
		} else {
			println("WARNING: More lines in patch than expected")
			patchDst
		}
	}
}

fun applyPatchesToKotlinRuntime(runtime: String): String {
	val (src, dst) = getResourceString("/patches/isInheritanceFromInterface.kotlin.js.patch").split("--------------------------------")
	return applyPatchToKotlinRuntime(runtime, src, dst)
}
