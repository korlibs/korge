package com.soywiz.korge.gradle.targets.js

import com.soywiz.korge.gradle.*
import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.targets.windows.*
import com.soywiz.korge.gradle.util.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.*
import java.io.*

private object JavaScriptClass

fun Project.configureJavaScript() {
    if (gkotlin.targets.findByName("js") != null) return

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

    val jsCreateIndexHtml = project.tasks.create("jsCreateIndexHtml", Task::class.java).apply {
        doLast {
            val targetDir = generatedIndexHtmlDir
            generatedIndexHtmlDir.mkdirs()
            logger.info("jsCreateIndexHtml.targetDir: $targetDir")
            val jsMainCompilation = kotlin.js().compilations["main"]!!
            //val jsFile = File(jsMainCompilation.kotlinOptions.outputFile ?: "dummy.js").name
            // @TODO: How to get the actual .js file generated/served?
            val jsFile = File("${project.name}.js").name
            val resourcesFolders = jsMainCompilation.allKotlinSourceSets
                .flatMap { it.resources.srcDirs } + listOf(File(rootProject.rootDir, "_template"))
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
                    project.korge.getIconBytes(it).decodeImage()
                }))
            } catch (e: Throwable) {
                e.printStackTrace()
            }

            File(targetDir, "index.html").writeText(
                groovy.text.SimpleTemplateEngine().createTemplate(indexTemplateHtml).make(
                    mapOf(
                        "OUTPUT" to jsFile,
                        "TITLE" to (korge.title ?: korge.name),
                        "CUSTOM_CSS" to customCss,
                        "CUSTOM_HTML_HEAD" to customHtmlHead,
                        "CUSTOM_HTML_BODY" to customHtmlBody
                    )
                ).toString()
            )
        }
    }

    (project.tasks.getByName("jsProcessResources") as Copy).apply {
        dependsOn(jsCreateIndexHtml)
        from(generatedIndexHtmlDir) {
            it.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
        //println(this.outputs.files.toList())

    }
    configureEsbuild()
    configureWebpackFixes()
    configureJavascriptRun()
    configureClosureCompiler()
}
