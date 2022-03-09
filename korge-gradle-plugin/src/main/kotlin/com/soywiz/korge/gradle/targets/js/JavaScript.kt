package com.soywiz.korge.gradle.targets.js

import com.soywiz.korge.gradle.*
import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.targets.windows.*
import com.soywiz.korge.gradle.util.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.plugin.*
import java.io.*

private object JavaScriptClass

fun Project.configureJavaScript() {
    if (gkotlin.targets.findByName("js") != null) return

    rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin>().configureEach {
        try {
            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().nodeVersion =
                BuildVersions.NODE_JS
        } catch (e: Throwable) {
            // Ignore failed because already configured
        }
    }

    gkotlin.apply {
		js(KotlinJsCompilerType.IR) {
            browser {
                binaries.executable()
                testTask {
                    useKarma {
                        useChromeHeadless()
                    }
                }
            }

			this.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)

			compilations.all {
				it.kotlinOptions.apply {
					sourceMap = true
					//metaInfo = true
					//moduleKind = "umd"
					suppressWarnings = korge.supressWarnings
				}
			}
		}

        sourceSets.maybeCreate("jsTest").apply {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-js")
            }
        }
	}

    val generatedIndexHtmlDir = File(project.buildDir, "processedResources-www")

    afterEvaluate {
        val jsCreateIndexHtml = project.tasks.create("jsCreateIndexHtml", JsCreateIndexTask::class.java).also { task ->
            val jsMainCompilation = kotlin.js().compilations["main"]!!
            val resourcesFolders: List<File> = jsMainCompilation.allKotlinSourceSets
                .flatMap { it.resources.srcDirs } + listOf(File(rootProject.rootDir, "_template"))
            task.resourcesFolders = resourcesFolders
            task.targetDir = generatedIndexHtmlDir
        }
        (project.tasks.getByName("jsProcessResources") as Copy).apply {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            dependsOn(jsCreateIndexHtml)
            from(generatedIndexHtmlDir) {
                it.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            }
            //println(this.outputs.files.toList())

        }
    }

    configureEsbuild()
    configureWebpackFixes()
    configureJavascriptRun()
    configureClosureCompiler()
}

abstract class JsCreateIndexTask : DefaultTask() {
    @get:InputFiles lateinit var resourcesFolders: List<File>
    //@get:OutputDirectory lateinit var targetDir: File
    @Internal lateinit var targetDir: File
    private val projectName: String = project.name
    private val korgeTitle: String? = project.korge.title
    private val korgeName: String? = project.korge.name

    private val iconProvider: KorgeIconProvider = KorgeIconProvider(project)

    @TaskAction
    fun run() {
        targetDir.mkdirs()
        logger.info("jsCreateIndexHtml.targetDir: $targetDir")
        //val jsFile = File(jsMainCompilation.kotlinOptions.outputFile ?: "dummy.js").name
        // @TODO: How to get the actual .js file generated/served?
        val jsFile = File("${projectName}.js").name
        //println("jsFile: $jsFile")
        //println("resourcesFolders: $resourcesFolders")
        fun readTextFile(name: String): String {
            for (folder in resourcesFolders) {
                val file = File(folder, name)?.takeIf { it.exists() } ?: continue
                return file.readText()
            }
            return JavaScriptClass::class.java.classLoader.getResourceAsStream(name)?.readBytes()?.toString(Charsets.UTF_8)
                ?: error("We cannot find suitable '$name'")
        }

        val indexTemplateHtml = readTextFile("index.v2.template.html")
        val customCss = readTextFile("custom-styles.template.css")
        val customHtmlHead = readTextFile("custom-html-head.template.html")
        val customHtmlBody = readTextFile("custom-html-body.template.html")

        //println(File(targetDir, "index.html"))

        try {
            File(targetDir, "favicon.ico").writeBytes(ICO2.encode(listOf(16, 32).map {
                iconProvider.getIconBytes(it).decodeImage()
            }))
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        File(targetDir, "index.html").writeText(
            groovy.text.SimpleTemplateEngine().createTemplate(indexTemplateHtml).make(
                mapOf(
                    "OUTPUT" to jsFile,
                    "TITLE" to (korgeTitle ?: korgeName ?: "KorGE"),
                    "CUSTOM_CSS" to customCss,
                    "CUSTOM_HTML_HEAD" to customHtmlHead,
                    "CUSTOM_HTML_BODY" to customHtmlBody
                )
            ).toString()
        )
    }
}
