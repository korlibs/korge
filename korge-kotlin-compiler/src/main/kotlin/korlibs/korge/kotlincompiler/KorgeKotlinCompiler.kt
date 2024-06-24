@file:OptIn(ExperimentalBuildToolsApi::class)

package korlibs.korge.kotlincompiler

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.jvm.*
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.*
import org.jetbrains.kotlin.cli.metadata.*
import org.jetbrains.kotlin.daemon.common.*
import org.w3c.dom.*
import java.io.*
import java.net.*
import java.security.MessageDigest
import java.util.*
import javax.xml.*
import javax.xml.parsers.*
import kotlin.collections.ArrayDeque
import kotlin.system.*

// https://github.com/JetBrains/kotlin/tree/master/compiler/build-tools/kotlin-build-tools-api
// https://github.com/JetBrains/kotlin/blob/bc1ddd8205f6107c7aec87a9fb3bd7713e68902d/compiler/build-tools/kotlin-build-tools-api-tests/src/main/kotlin/compilation/model/JvmModule.kt
class KorgeKotlinCompiler {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val libs = listOf(
                *MavenTools.getMavenArtifacts(MavenArtifact("org.jetbrains.kotlin", "kotlin-stdlib", "2.0.0")).toTypedArray(),
                *MavenTools.getMavenArtifacts(MavenArtifact("com.soywiz.korge", "korge-jvm", "999.0.0.999")).toTypedArray(),
            )

            println(libs.joinToString("\n"))

            //return

            val service = CompilationService.loadImplementation(ClassLoader.getSystemClassLoader())
            println(service.getCompilerVersion())
            val executionConfig = service.makeCompilerExecutionStrategyConfiguration()
                .useInProcessStrategy()

            //executionConfig.useDaemonStrategy(emptyList())

            val buildDirectory = File("/temp/build").absoluteFile
            val icWorkingDir = File(buildDirectory, "ic")
            val icCachesDir = File(icWorkingDir, "caches")
            icWorkingDir.mkdirs()
            icCachesDir.mkdirs()

            val snapshots = mutableListOf<File>()

            for (lib in libs) {
                val hexDigest = MessageDigest.getInstance("SHA1").digest(lib.readBytes()).toHexString()
                val file = File(icWorkingDir, "dep-" + lib.name + "-$hexDigest.snapshot").absoluteFile
                if (!file.exists()) {
                    val snapshot = service.calculateClasspathSnapshot(lib, ClassSnapshotGranularity.CLASS_MEMBER_LEVEL)
                    println("Saving... $file")
                    file.parentFile.mkdirs()
                    //println(snapshot.classSnapshots)
                    snapshot.saveSnapshot(file)
                } else {
                    println("Loading... $file")
                }
                snapshots += file
            }

            //val snapshots = libs
            val shrunkClasspathSnapshotFile = File(icWorkingDir, "shrunk-classpath-snapshot.bin")
            //shrunkClasspathSnapshotFile.createNewFile()
            //options.forceNonIncrementalMode(value = true)

            val srcRoots = listOf(
                File("C:\\Users\\soywiz\\projects\\korge-snake\\src"),
                File("C:\\Users\\soywiz\\projects\\korge-snake\\modules\\korma-tile-matching\\src\\commonMain\\kotlin")
            )

            val allFiles = srcRoots.flatMap { it.walkBottomUp() }.filter { it.extension == "kt" }

            println("allFiles=${allFiles.joinToString("\n")}")

            repeat(10) { NN ->
                val time = measureTimeMillis {

                    val result = service.compileJvm(
                        projectId = ProjectId.ProjectUUID(UUID.randomUUID()),
                        strategyConfig = executionConfig,
                        compilationConfig = service.makeJvmCompilationConfiguration().also { compilationConfig ->
                            compilationConfig.useIncrementalCompilation(
                                icCachesDir,
                                //SourcesChanges.ToBeCalculated,
                                //SourcesChanges.Known(listOf(allFiles.first()), emptyList()),
                                SourcesChanges.Known(if (NN == 0) allFiles else listOf(allFiles.first()), emptyList()),
                                //SourcesChanges.Known(listOf(allFiles.first()), emptyList()),
                                ClasspathSnapshotBasedIncrementalCompilationApproachParameters(
                                    snapshots,
                                    //emptyList(),
                                    shrunkClasspathSnapshotFile
                                ),
                                compilationConfig.makeClasspathSnapshotBasedIncrementalCompilationConfiguration().also {
                                    it.setBuildDir(buildDirectory)
                                    it.setRootProjectDir(File("C:\\Users\\soywiz\\projects\\korge-snake"))
                                    //it.forceNonIncrementalMode(true)
                                }
                            )
                        },
                        //listOf(File("/temp/1")),
                        sources = listOf<File>(
                            //File("/temp/1"),
                            //File("/temp/1-common")
                            File("C:\\Users\\soywiz\\projects\\korge-snake\\src"),
                            File("C:\\Users\\soywiz\\projects\\korge-snake\\modules\\korma-tile-matching\\src\\commonMain\\kotlin"),
                        ) + allFiles,
                        //listOf(File("/temp/1-common")),
                        arguments = listOf(
                            "-module-name=korge-snake",
                            "-Xjdk-release=17",
                            "-Xmulti-platform",
                            "-language-version=1.9",
                            "-api-version=1.9",
                            "-no-stdlib",
                            "-no-reflect",
                            "-Xexpect-actual-classes",
                            "-Xenable-incremental-compilation",
                            "-classpath=${libs.joinToString(File.pathSeparator) { it.absolutePath }}",
                            "-d",
                            File(buildDirectory, "classes").absolutePath,
                            //add("-Xfriend-paths=${friendPaths.joinToString(",")}")
                            //"C:\\Users\\soywiz\\projects\\korge-snake\\src",
                            //"C:\\Users\\soywiz\\projects\\korge-snake\\modules\\korma-tile-matching\\src\\commonMain\\kotlin",
                        )
                    )
                    println("result=$result")
                }

                println("[1]")
                println(time)
            }
//
            return


            println("[2]")

            //KorgeKotlinCompiler().doCompile("/temp/1-common.klib", listOf("/temp/1-common"), target = Target.COMMON)
            KorgeKotlinCompiler().doCompile(
                out = "/temp/1.jvm",
                //srcs = listOf("C:/temp/1"),
                //srcs = listOf("/temp/1", "/temp/1-common"),
                srcs = listOf(
                    "C:\\Users\\soywiz\\projects\\korge-snake\\src",
                    "C:\\Users\\soywiz\\projects\\korge-snake\\modules\\korma-tile-matching\\src\\commonMain\\kotlin",
                ),
                //common = listOf("C:/temp/1-common"),
                libs = libs.map { it.absolutePath },
                //klibs = listOf("C:/temp/1-common.klib"),
                target = Target.JVM
            )

            /*
            // Create a message collector to capture compiler messages
            val messageCollector = PrintingMessageCollector(System.err, MessageRenderer.GRADLE_STYLE, true)

            // Set up compiler configuration
            val configuration = CompilerConfiguration()
            configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
            configuration.put(CommonConfigurationKeys.MODULE_NAME, "MultiplatformModule")

            // Specify common, Android, and iOS source directories
            val commonSrcDir = File("src/commonMain/kotlin")
            val androidSrcDir = File("src/androidMain/kotlin")
            val iosSrcDir = File("src/iosMain/kotlin")

            //configuration.addJvmClasspathRoot(PathUtil.getResourcePathForClass(MessageCollector::class.java))

            // Add source directories to the configuration
            configuration.add(CLIConfigurationKeys.CONTENT_ROOTS, KotlinSourceRoot("/temp/1-common", isCommon = true, hmppModuleName = null))
            configuration.add(CLIConfigurationKeys.CONTENT_ROOTS, KotlinSourceRoot("/temp/1", isCommon = false, hmppModuleName = null))
            //configuration.add(CLIConfigurationKeys.CONTENT_ROOTS, iosSrcDir)

            // Add classpath entries
            //val classpath = (Thread.currentThread().contextClassLoader as URLClassLoader).urLs.map { File(it.toURI()) }
            //classpath.forEach { configuration.add(CLIConfigurationKeys.CONTENT_ROOTS, it) }

            // Create the environment
            val environment = KotlinCoreEnvironment.createForProduction(
                {},
                configuration,
                //KotlinCoreEnvironment.ProjectEnvironmentName.Production
                EnvironmentConfigFiles.JVM_CONFIG_FILES
            )

            // Set up the compiler
            val compiler = K2JVMCompiler()
            val arguments = compiler.createArguments().apply {
                destination = "build/classes/kotlin/main"
                freeArgs = listOf(commonSrcDir.path, androidSrcDir.path, iosSrcDir.path)
                classpath = libs.joinToString(File.pathSeparator) { it.absolutePath }
            }

            // Invoke the compiler
            //compiler.exec()
            K2JVMCompiler().
            object MyCompiler : K2JVMCompiler() {

            }
            val result = compiler.exec(messageCollector, Services.EMPTY, arguments)
            if (result == org.jetbrains.kotlin.cli.common.ExitCode.OK) {
                println("Compilation succeeded")
            } else {
                println("Compilation failed")
            }

             */
        }

    }

    val metadataCompiler = K2MetadataCompiler()
    val jvmCompiler = K2JVMCompiler()
    val arguments = jvmCompiler.createArguments().also {
        it.incrementalCompilation = true
        it.reportOutputFiles = true
        it.multiPlatform = true
        it.expectActualClasses = true
        it.klibLibraries
        it.languageVersion
    }

    enum class Target {
        JVM, JS, COMMON
    }

    fun doCompile(
        out: String,
        srcs: List<String> = emptyList(),
        common: List<String> = emptyList(),
        libs: List<String> = emptyList(),
        klibs: List<String> = emptyList(),
        target: Target = Target.JVM
    ) {
        val args = buildList<String> {
            //add("-Xmodule-name=mymodule")
            add("-Xmulti-platform")
            add("-Xjdk-release=17")
            //add("-no-stdlib")
            //add("-help")
            add("-language-version"); add("1.9")
            add("-api-version"); add("1.9")

            //add("-verbose")
            //add("-progressive")
            add("-Xenable-incremental-compilation")
            //for (src in common) {
            //    add(File(src).absolutePath)
            //}
            if (common.isNotEmpty()) {
                add("-Xcommon-sources=${common.joinToString(File.pathSeparator) { File(it).absolutePath }}")
            }
            if (target == Target.JVM) add("-no-stdlib")
            if (libs.isNotEmpty()) {
                add("-classpath")
                add(libs.joinToString(File.pathSeparator) { File(it).absolutePath })
            }
            if (klibs.isNotEmpty()) {
                add("-Xklib=${klibs.joinToString(File.pathSeparator) { File(it).absolutePath }}")
            }
            add("-d")
            add(File(out).absolutePath)
            for (src in srcs) {
                add(File(src).absolutePath)
            }
        }

        println("[3]")

        println("args=$args")

        repeat(20) {
            println(measureTimeMillis {
                (if (target == Target.JVM) jvmCompiler else metadataCompiler).exec(
                    System.err,
                    MessageRenderer.GRADLE_STYLE,
                    *args.toTypedArray()
                )
            })
        }



        //compiler.
        //CLITool.doMain(compiler.createArguments(), arrayOf())
        /*
        K2JVMCompiler.main(arrayOf(""))
        val arguments = createCompilerArguments()
        val buildArguments = buildMetrics.measure(GradleBuildTime.OUT_OF_WORKER_TASK_ACTION) {
            val output = outputFile.get()
            output.parentFile.mkdirs()

            buildFusService.orNull?.reportFusMetrics {
                NativeCompilerOptionMetrics.collectMetrics(compilerOptions, it)
            }

            ArgumentUtils.convertArgumentsToStringList(arguments)
        }

        KotlinNativeCompilerRunner(
            settings = runnerSettings,
            executionContext = KotlinToolRunner.GradleExecutionContext.fromTaskContext(objectFactory, execOperations, logger),
            metricsReporter = buildMetrics
        ).run(buildArguments)

         */
    }
}

data class MavenArtifact(val group: String, val name: String, val version: String, val classifier: String? = null, val extension: String = "jar") {
    val groupSeparator by lazy { group.replace(".", "/") }
    val localPath by lazy { "$groupSeparator/$name/$version/$name-$version.$extension" }
}
data class MavenDependency(val artifact: MavenArtifact, val scope: String)

class Pom(
    val packaging: String? = null,
    val deps: List<MavenDependency> = emptyList(),
) {
    companion object {
        fun parse(file: File): Pom = parse(file.readText())
        fun parse(text: String): Pom {
            val db = DocumentBuilderFactory.newInstance().also { it.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }.newDocumentBuilder()
            val doc = db.parse(text.byteInputStream())
            val out = arrayListOf<MavenDependency>()
            val node = doc.getElementsByTagName("packaging").toList().firstOrNull()
            for (e in doc.getElementsByTagName("dependency").toList()) {
                val groupId = e.findChildByTagName("groupId").firstOrNull()?.textContent?.trim() ?: error("Missing groupId")
                val artifactId = e.findChildByTagName("artifactId").firstOrNull()?.textContent?.trim() ?: error("Missing artifactId")
                val scope = e.findChildByTagName("scope").firstOrNull()?.textContent?.trim()
                if (scope == "test" || scope == null) continue
                val version = e.findChildByTagName("version").firstOrNull()?.textContent?.trim() ?: error("Missing version for $groupId:$artifactId in $text")
                if (version.contains("\$")) continue
                out += MavenDependency(MavenArtifact(groupId, artifactId, version), scope ?: "compile")
                //println("DEP: $groupId:$artifactId:$version  :: $scope")
            }
            return Pom(packaging = node?.textContent, deps = out)
        }

        private fun NodeList.toList(): List<Node> = (0 until length).map { item(it) }
        private fun Node.findChildByTagName(tagName: String): List<Node> = childNodes.toList().filter { it.nodeName.equals(tagName, ignoreCase = true) }
    }
}

object MavenTools {
    fun getMavenArtifacts(artifact: MavenArtifact, explored: MutableSet<MavenArtifact> = mutableSetOf()): List<File> {
        val explore = ArrayDeque<MavenArtifact>()
        explore += artifact
        val out = arrayListOf<MavenArtifact>()
        while (explore.isNotEmpty()) {
            val artifact = explore.removeFirst()
            if (artifact in explored) continue
            explored += artifact
            val pom = Pom.parse(getSingleMavenArtifact(artifact.copy(extension = "pom")))
            if (pom.packaging == null || pom.packaging == "jar") {
                out += artifact
            }
            for (dep in pom.deps) {
                explore += dep.artifact
            }
        }
        return out.map { getSingleMavenArtifact(it) }
    }

    fun getSingleMavenArtifact(artifact: MavenArtifact): File {
        val file = File(System.getProperty("user.home"), ".m2/repository/${artifact.localPath}")
        if (!file.exists()) {
            file.parentFile.mkdirs()
            val url = URL("https://repo1.maven.org/maven2/${artifact.localPath}")
            println("Downloading $url")
            file.writeBytes(url.readBytes())
        }
        return file
    }

}
