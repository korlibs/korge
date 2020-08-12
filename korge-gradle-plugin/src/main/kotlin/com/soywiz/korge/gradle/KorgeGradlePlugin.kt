package com.soywiz.korge.gradle

import org.gradle.api.*
import org.jetbrains.kotlin.gradle.dsl.*
import java.io.*

class KorgeGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.configureRepositories()
        //project.extensions.add("korge", project.korge)
        project.korge // Ensure KorGE extension is here
        System.setProperty("java.awt.headless", "true")
        project.plugins.apply("kotlin-multiplatform")
        // JVM is always enabled
        project.korge.jvm()
        project.afterEvaluate {
            //val korgeVersion = project.findProperty("korgeVersion") ?: "20-SNAPSHOT"
            val korgeVersion = project.findProperty("korgeVersion") ?: "20-alpha-1"
            project.dependencies.add("commonMainApi", "com.soywiz.korlibs.korge:korge:$korgeVersion")
            //project.

            if (project.hasKotlinTarget("js")) {
                project.tasks.create("runJs").apply {
                    group = "run"
                    dependsOn("jsBrowserDevelopmentRun")
                }
            }
            if (project.hasKotlinTarget("jvm")) {
                //project.dependencies.add("jvmMainApi", "com.soywiz.korlibs.korge:korge-jvm:$korgeVersion")
                project.tasks.create("runJvm", KorgeJavaExec::class.java).apply {
                    group = "run"
                    main = "MainKt"
                }
            }

            project.kotlin.apply {
                /*
                tasks.getByName("jsProcessResources", Task::class).apply {
                    //println(this.outputs.files.toList())
                    doLast {
                        val targetDir = this.outputs.files.first()
                        val jsMainCompilation = kotlin.js().compilations["main"]!!
                        val jsFile = File(jsMainCompilation.kotlinOptions.outputFile ?: "dummy.js").name
                        val resourcesFolders = jsMainCompilation.allKotlinSourceSets
                            .flatMap { it.resources.srcDirs } + listOf(File(rootProject.rootDir, "_template"))
                        //println("jsFile: $jsFile")
                        //println("resourcesFolders: $resourcesFolders")
                        fun readTextFile(name: String): String {
                            for (folder in resourcesFolders) {
                                val file = File(folder, name)?.takeIf { it.exists() } ?: continue
                                return file.readText()
                            }
                            return ClassLoader.getSystemResourceAsStream(name)?.readBytes()?.toString(Charsets.UTF_8)
                                ?: error("We cannot find suitable '$name'")
                        }

                        val indexTemplateHtml = readTextFile("index.v2.template.html")
                        val customCss = readTextFile("custom-styles.template.css")
                        val customHtmlHead = readTextFile("custom-html-head.template.html")
                        val customHtmlBody = readTextFile("custom-html-body.template.html")

                        println(File(targetDir, "index.html"))

                        File(targetDir, "index.html").writeText(
                            groovy.text.SimpleTemplateEngine().createTemplate(indexTemplateHtml).make(
                                mapOf(
                                    "OUTPUT" to jsFile,
                                    //"TITLE" to korge.name,
                                    "TITLE" to "TODO",
                                    "CUSTOM_CSS" to customCss,
                                    "CUSTOM_HTML_HEAD" to customHtmlHead,
                                    "CUSTOM_HTML_BODY" to customHtmlBody
                                )
                            ).toString()
                        )
                    }
                }
                */

                /*
                if (doEnableKotlinNative) {
                    for (target in nativeTargets()) {
                        target.apply {
                            binaries {
                                executable {
                                    entryPoint("entrypoint.main")
                                }
                            }
                        }
                    }

                    val nativeDesktopFolder = File(project.buildDir, "platforms/nativeDesktop")
                    //val nativeDesktopEntryPointSourceSet = kotlin.sourceSets.create("nativeDesktopEntryPoint")
                    //nativeDesktopEntryPointSourceSet.kotlin.srcDir(nativeDesktopFolder)
                    sourceSets.getByName("nativeCommonMain") { kotlin.srcDir(nativeDesktopFolder) }

                    val createEntryPointAdaptorNativeDesktop = tasks.create("createEntryPointAdaptorNativeDesktop") {
                        val mainEntrypointFile = File(nativeDesktopFolder, "entrypoint/main.kt")

                        outputs.file(mainEntrypointFile)

                        // @TODO: Determine the package of the main file
                        doLast {
                            mainEntrypointFile.also { it.parentFile.mkdirs() }.writeText("""
                        package entrypoint

                        import kotlinx.coroutines.*
                        import main

                        fun main(args: Array<String>) {
                            runBlocking {
                                main()
                            }
                        }
                    """.trimIndent())
                        }
                    }

                    val nativeDesktopTargets = nativeTargets()
                    val allNativeTargets = nativeDesktopTargets

                    //for (target in nativeDesktopTargets) {
                    //target.compilations["main"].defaultSourceSet.dependsOn(nativeDesktopEntryPointSourceSet)
                    //    target.compilations["main"].defaultSourceSet.kotlin.srcDir(nativeDesktopFolder)
                    //}

                    for (target in allNativeTargets) {
                        for (binary in target.binaries) {
                            val compilation = binary.compilation
                            val copyResourcesTask = tasks.create("copyResources${target.name.capitalize()}${binary.name.capitalize()}", Copy::class) {
                                dependsOn(getKorgeProcessResourcesTaskName(target, compilation))
                                group = "resources"
                                val isDebug = binary.buildType == org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType.DEBUG
                                val isTest = binary.outputKind == org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind.TEST
                                val compilation = if (isTest) target.compilations["test"] else target.compilations["main"]
                                //target.compilations.first().allKotlinSourceSets
                                val sourceSet = compilation.defaultSourceSet
                                from(sourceSet.resources)
                                from(sourceSet.dependsOn.map { it.resources })
                                into(binary.outputDirectory)
                            }

                            //compilation.compileKotlinTask.dependsOn(copyResourcesTask)
                            binary.linkTask.dependsOn(copyResourcesTask)
                            binary.compilation.compileKotlinTask.dependsOn(createEntryPointAdaptorNativeDesktop)
                        }
                    }
                }

                 */
            }

            /*
            project.tasks {
                val runJvm by getting(KorgeJavaExec::class)
                val jvmMainClasses by getting(Task::class)

                //val prepareResourceProcessingClasses = create("prepareResourceProcessingClasses", Copy::class) {
                //    dependsOn(jvmMainClasses)
                //    afterEvaluate {
                //        from(runJvm.korgeClassPath.toList().map { if (it.extension == "jar") zipTree(it) else it })
                //    }
                //    into(File(project.buildDir, "korgeProcessedResources/classes"))
                //}

                for (target in kotlin.targets) {
                    for (compilation in target.compilations) {
                        val processedResourcesFolder = File(project.buildDir, "korgeProcessedResources/${target.name}/${compilation.name}")
                        compilation.defaultSourceSet.resources.srcDir(processedResourcesFolder)
                        val korgeProcessedResources = create(getKorgeProcessResourcesTaskName(target, compilation)) {
                            //dependsOn(prepareResourceProcessingClasses)
                            dependsOn(jvmMainClasses)

                            doLast {
                                processedResourcesFolder.mkdirs()
                                //URLClassLoader(prepareResourceProcessingClasses.outputs.files.toList().map { it.toURL() }.toTypedArray(), ClassLoader.getSystemClassLoader()).use { classLoader ->


                                /*
                                URLClassLoader(runJvm.korgeClassPath.toList().map { it.toURL() }.toTypedArray(), ClassLoader.getSystemClassLoader()).use { classLoader ->
                                    val clazz = classLoader.loadClass("com.soywiz.korge.resources.ResourceProcessorRunner")
                                    val folders = compilation.allKotlinSourceSets.flatMap { it.resources.srcDirs }.filter { it != processedResourcesFolder }.map { it.toString() }
                                    //println(folders)
                                    try {
                                        clazz.methods.first { it.name == "run" }.invoke(null, classLoader, folders, processedResourcesFolder.toString(), compilation.name)
                                    } catch (e: java.lang.reflect.InvocationTargetException) {
                                        val re = (e.targetException ?: e)
                                        re.printStackTrace()
                                        System.err.println(re.toString())
                                    }
                                }
                                System.gc()
                                 */
                            }
                        }
                        //println(compilation.compileKotlinTask.name)
                        //println(compilation.compileKotlinTask.name)
                        //compilation.compileKotlinTask.finalizedBy(processResourcesKorge)
                        //println(compilation.compileKotlinTask)
                        //compilation.compileKotlinTask.dependsOn(processResourcesKorge)
                        if (compilation.compileKotlinTask.name != "compileKotlinJvm") {
                            compilation.compileKotlinTask.dependsOn(korgeProcessedResources)
                        } else {
                            compilation.compileKotlinTask.finalizedBy(korgeProcessedResources)
                            getByName("runJvm").dependsOn(korgeProcessedResources)

                        }
                        //println(compilation.output.allOutputs.toList())
                        //println("$target - $compilation")

                    }
                }
            }
            */
        }
    }
}
