@file:Suppress("DEPRECATION_ERROR", "DEPRECATION")

package korlibs.korge.gradle.targets.js

import java.io.File
import korlibs.allThis
import korlibs.korge.gradle.gkotlin
import korlibs.korge.gradle.korge
import korlibs.korge.gradle.kotlin
import korlibs.korge.gradle.targets.KorgeIconProvider
import korlibs.korge.gradle.targets.ProjectType
import korlibs.korge.gradle.targets.jvm.ensureSourceSetsConfigure
import korlibs.korge.gradle.targets.windows.ICO2
import korlibs.korge.gradle.util.createThis
import korlibs.korge.gradle.util.decodeImage
import korlibs.korge.gradle.util.get
import korlibs.korge.gradle.util.takeIfExists
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetWithTests
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

private object JavaScriptClass

fun Project.configureJavaScript(projectType: ProjectType) {
    if (gkotlin.targets.findByName("js") != null) return

    gkotlin.apply {
		js(KotlinJsCompilerType.IR) {
            browser {
                binaries.executable()
            }

            this.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)

			compilations.allThis {
        // Handled below for all JS compile tasks
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
    tasks.withType(Kotlin2JsCompile::class.java).allThis {
        kotlinOptions.sourceMap = korge.sourceMaps
        kotlinOptions.suppressWarnings = korge.supressWarnings
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
        }
    }

    configureEsbuild()
    configureDenoTest()
    if (projectType.isExecutable) {
        configureDenoRun()
        configureJavascriptRun()
    }
    configureWebpack()

    ensureSourceSetsConfigure("common", "js")
}

fun KotlinJsTargetDsl.configureJsTargetOnce() {
    this.compilerOptions {
        target.set("es2015")
    }
}

fun KotlinJsTargetDsl.configureJSTestsOnce() {
    browser {
        testRuns.getByName(KotlinTargetWithTests.DEFAULT_TEST_RUN_NAME).executionTask.configure {
            useKarma {
                useChromeHeadless()
                File(project.rootProject.rootDir, "karma.config.d").takeIfExists()?.let {
                    useConfigDirectory(it)
                }
            }
        }
    }
}

@DisableCachingByDefault
abstract class JsCreateIndexTask : DefaultTask() {
    @get:InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    lateinit var resourcesFolders: List<File>
    @Internal lateinit var targetDir: File
    private val projectName: String = project.name
    private val korgeTitle: String? = project.korge.title
    private val korgeName: String? = project.korge.name

    private val iconProvider: KorgeIconProvider = KorgeIconProvider(project)

    @TaskAction
    fun run() {
        targetDir.mkdirs()
        logger.info("jsCreateIndexHtml.targetDir: $targetDir")
        // @TODO: How to get the actual .js file generated/served?
        val jsFile = File("${projectName}.js").name
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
