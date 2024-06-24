package korlibs.korge.kotlincompiler

import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.*
import org.jetbrains.kotlin.cli.metadata.*
import org.w3c.dom.*
import java.io.*
import java.net.*
import javax.xml.*
import javax.xml.parsers.*
import kotlin.system.*

class KorgeKotlinCompiler {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println("[1]")

            val libs = listOf(
                *MavenTools.getMavenArtifacts(MavenArtifact("org.jetbrains.kotlin", "kotlin-stdlib", "2.0.0")).toTypedArray(),
            )

            println("[2]")

            //KorgeKotlinCompiler().doCompile("/temp/1-common.klib", listOf("/temp/1-common"), target = Target.COMMON)
            KorgeKotlinCompiler().doCompile(
                out = "/temp/1.jvm",
                //srcs = listOf("C:/temp/1"),
                srcs = listOf("/temp/1", "/temp/1-common"),
                //common = listOf("C:/temp/1-common"),
                libs = libs.map { it.absolutePath },
                target = Target.COMMON
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
            add("-Xenable-incremental-compilation")
            add("-Xmulti-platform")
            for (src in srcs) {
                add(File(src).absolutePath)
            }
            //for (src in common) {
            //    add(File(src).absolutePath)
            //}
            if (common.isNotEmpty()) {
                add("-Xcommon-sources=${common.joinToString(File.pathSeparator) { File(it).absolutePath }}")
            }
            if (target == Target.JVM) add("-no-stdlib")
            add("-api-version=2.0")
            add("-language-version=2.0")
            if (libs.isNotEmpty()) {
                add("-cp")
                add(libs.joinToString(File.pathSeparator) { File(it).absolutePath })
            }
            if (klibs.isNotEmpty()) {
                add("-Xklib")
                add(klibs.joinToString(File.pathSeparator) { File(it).absolutePath })
            }
            add("-progressive")
            add("-d")
            add(File(out).absolutePath)
        }

        println("[3]")

        println(measureTimeMillis {
            (if (target == Target.JVM) jvmCompiler else metadataCompiler).exec(
                System.err,
                MessageRenderer.GRADLE_STYLE,
                *args.toTypedArray()
            )
        })



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

object MavenTools {
    fun getPomDependencies(file: File): List<MavenDependency> = getPomDependencies(file.readText())
    fun getPomDependencies(text: String): List<MavenDependency> {
        val db = DocumentBuilderFactory.newInstance().also { it.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }.newDocumentBuilder()
        val doc = db.parse(text.byteInputStream())
        val out = arrayListOf<MavenDependency>()
        for (e in doc.getElementsByTagName("dependency").toList()) {
            val groupId = e.findChildByTagName("groupId").firstOrNull()?.textContent?.trim()
            val artifactId = e.findChildByTagName("artifactId").firstOrNull()?.textContent?.trim()
            val version = e.findChildByTagName("version").firstOrNull()?.textContent?.trim()
            val scope = e.findChildByTagName("scope").firstOrNull()?.textContent?.trim()
            out += MavenDependency(MavenArtifact(groupId!!, artifactId!!, version!!), scope ?: "compile")
            //println("DEP: $groupId:$artifactId:$version  :: $scope")
        }
        return out
    }

    fun getMavenArtifacts(artifact: MavenArtifact, outArtifacts: MutableSet<MavenArtifact> = mutableSetOf()): List<File> {
        val explore = ArrayDeque<MavenArtifact>()
        explore += artifact
        while (explore.isNotEmpty()) {
            val artifact = explore.removeFirst()
            if (artifact in outArtifacts) continue
            outArtifacts += artifact
            val deps = getPomDependencies(getSingleMavenArtifact(artifact.copy(extension = "pom")))
            for (dep in deps) {
                explore += dep.artifact
            }
        }
        return outArtifacts.map { getSingleMavenArtifact(it) }
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

    private fun NodeList.toList(): List<Node> = (0 until length).map { item(it) }
    private fun Node.findChildByTagName(tagName: String): List<Node> = childNodes.toList().filter { it.nodeName.equals(tagName, ignoreCase = true) }
}
