package korlibs.korge.gradle.targets.js

import korlibs.korge.gradle.*
import korlibs.korge.gradle.gkotlin
import korlibs.korge.gradle.kotlin
import korlibs.korge.gradle.targets.*
import korlibs.korge.gradle.targets.windows.*
import korlibs.korge.gradle.util.*
import korlibs.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.targets.js.dsl.*
import java.io.*

private object JavaScriptClass

fun Project.configureJavaScript(projectType: ProjectType) {
    if (gkotlin.targets.findByName("js") != null) return

    rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java).allThis {
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
            }

            this.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)

			compilations.allThis {
				kotlinOptions.apply {
					sourceMap = korge.sourceMaps
					//metaInfo = true
					//moduleKind = "umd"
					suppressWarnings = korge.supressWarnings
				}
			}
            configureJsTargetOnce()
            configureJSTestsOnce()
		}

        sourceSets.maybeCreate("jsTest").apply {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-js")
            }
        }
	}

    // https://youtrack.jetbrains.com/issue/KT-58187/KJS-IR-Huge-performance-bottleneck-while-generating-sourceMaps-getCannonicalFile#focus=Comments-27-7301819.0-0
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile> {
        kotlinOptions.sourceMap = korge.sourceMaps
    }

    val generatedIndexHtmlDir = File(project.buildDir, "processedResources-www")

    afterEvaluate {
        val jsCreateIndexHtml = project.tasks.createThis<JsCreateIndexTask>("jsCreateIndexHtml").also { task ->
            val jsMainCompilation = kotlin.js().compilations["main"]!!
            val resourcesFolders: List<File> = jsMainCompilation.allKotlinSourceSets
                .flatMap { it.resources.srcDirs } + listOf(
                    File(rootProject.rootDir, "_template"),
                    File(rootProject.rootDir, "buildSrc/src/main/resources"),
                )
            task.resourcesFolders = resourcesFolders
            task.targetDir = generatedIndexHtmlDir
        }
        (project.tasks.getByName("jsProcessResources") as Copy).apply {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            dependsOn(jsCreateIndexHtml)
            from(generatedIndexHtmlDir) {
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            }
            //println(this.outputs.files.toList())

        }
    }

    configureEsbuild()
    configureWebpackFixes()
    if (projectType.isExecutable) {
        configureJavascriptRun()
    }
    configureClosureCompiler()
}

fun KotlinJsTargetDsl.configureJsTargetOnce() {
    this.compilerOptions {
        //target.set("es2015")
    }
}

fun KotlinJsTargetDsl.configureJSTestsOnce() {
    browser {
        //testTask { useKarma { useChromeHeadless() } }
        testRuns.getByName(KotlinTargetWithTests.DEFAULT_TEST_RUN_NAME).executionTask.configure {
            it.useKarma {
                useChromeHeadless()
                File(project.rootProject.rootDir, "karma.config.d").takeIfExists()?.let {
                    useConfigDirectory(it)
                }
            }
        }
    }

    // Kotlin 1.8.10:
    //   compileSync: task ':korio:jsTestTestDevelopmentExecutableCompileSync' : [/Users/soywiz/projects/korge/build/js/packages/korge-root-korio-test/kotlin]
    // Kotlin 1.8.20-RC:
    //   compileSync: task ':korio:jsTestTestDevelopmentExecutableCompileSync' : [/Users/soywiz/projects/korge/build/js/packages/korge-root-korio-test/kotlin]
    //for (kind in listOf("Development", "Production")) {
    //    val compileSync = project.tasks.findByName("jsTestTest${kind}ExecutableCompileSync") as Copy
    //    println("compileSync: $compileSync : ${compileSync.outputs.files.files}")
    //}
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
