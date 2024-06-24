@file:OptIn(ExperimentalBuildToolsApi::class)

package korlibs.korge.kotlincompiler

import korlibs.korge.kotlincompiler.maven.*
import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.jvm.*
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.io.*
import java.security.*
import java.util.*

// https://github.com/JetBrains/kotlin/tree/master/compiler/build-tools/kotlin-build-tools-api
// https://github.com/JetBrains/kotlin/blob/bc1ddd8205f6107c7aec87a9fb3bd7713e68902d/compiler/build-tools/kotlin-build-tools-api-tests/src/main/kotlin/compilation/model/JvmModule.kt
class KorgeKotlinCompiler {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            println(javaExecutablePath)

            val libs = filesForMaven(
                MavenArtifact("org.jetbrains.kotlin", "kotlin-stdlib", "2.0.0"),
                MavenArtifact("com.soywiz.korge", "korge-jvm", "999.0.0.999")
            )

            val mod1 = Module(
                path = File("C:\\Users\\soywiz\\projects\\korge-snake\\modules\\korma-tile-matching"),
                libs = libs
            )
            val snakeModule = Module(
                path = File("C:\\Users\\soywiz\\projects\\korge-snake"),
                moduleDeps = setOf(mod1),
                libs = libs
            )

            compileAndRun(snakeModule, mapOf("KORGE_HEADLESS" to "true", "KORGE_IPC" to "C:\\Users\\soywiz\\AppData\\Local\\Temp\\/KORGE_IPC-304208"))
        }

        fun compileAndRun(module: Module, envs: Map<String, String> = mapOf()) {
            compileAllModules(module)
            runModule(module, envs)
        }

        fun compileAllModules(module: Module) {
            module.allModuleDeps.forEach {
                compileModule(it)
            }
        }

        val javaExecutablePath by lazy {
            ProcessHandle.current()
                .info()
                .command()
                .orElseThrow()
        }

        fun compileModule(
            module: Module
        ) {
            val srcDirs = module.srcDirs
            val resourcesDirs = module.resourceDirs
            val libFiles = arrayListOf<File>()
            val root = module.path

            for (dep in module.allModuleDeps.map { it.classesDir }) {
                libFiles += dep
            }

            val compiler = KorgeKotlinCompiler()
            compiler.rootDir = root
            compiler.sourceDirs = srcDirs.toSet()
            compiler.libs = module.libs.toSet() + libFiles.toSet()

            val (time, result) = measureTimeMillisWithResult {
                compiler.compileJvm()
                //println(compiler.compileJvm(forceRecompilation = true))
            }
            if (result != CompilationResult.COMPILATION_SUCCESS) {
                //compiler.filesTxtFile.delete() // Deletes just in case
            }
            println("$result: $time ms")
            //compiler.runJvm()
        }

        fun runModule(module: Module, envs: Map<String, String> = mapOf()) {
            val allClasspaths: Set<File> = module.allModuleDeps.flatMap { setOf(it.classesDir) + it.resourceDirs + it.libs }.toSet()
            runJvm(module.main, allClasspaths, envs)
        }

        fun runJvm(main: String, classPaths: Collection<File>, envs: Map<String, String> = mapOf()): Int {
            val allArgs = listOf(javaExecutablePath, "-cp", classPaths.joinToString(File.pathSeparator), main)
            println(allArgs.joinToString(" "))
            return ProcessBuilder(allArgs)
                .inheritIO()
                .also { it.environment().putAll(envs) }
                .start()
                .also { proc -> Runtime.getRuntime().addShutdownHook(Thread { proc.destroy(); proc.destroyForcibly() }) }
                .waitFor()
        }

        fun filesForMaven(vararg artifacts: MavenArtifact): Set<File> = artifacts.flatMap { filesForMaven(it) }.toSet()
        fun filesForMaven(artifacts: List<MavenArtifact>): Set<File> = artifacts.flatMap { filesForMaven(it) }.toSet()
        fun filesForMaven(artifact: MavenArtifact): Set<File> = MavenTools.getMavenArtifacts(artifact)
    }

    data class Module(
        val path: File,
        val moduleDeps: Set<Module> = setOf(),
        val libs: Set<File> = setOf(),
        val main: String = "MainKt",
    ) {
        val allModuleDeps: Set<Module> by lazy {
            (moduleDeps.flatMap { it.allModuleDeps + it } + this).toSet()
        }
        val allTransitiveLibs by lazy { allModuleDeps.flatMap { it.libs }.toSet() }

        val buildDir = File(path, ".korge")
        val classesDir = File(buildDir, "classes")
        val srcDirs by lazy {
            val dirs = arrayListOf<File>()
            if (File(path, "src/commonMain/kotlin").isDirectory) {
                dirs += File(path, "src/commonMain/kotlin")
                dirs += File(path, "src/jvmMain/kotlin")
            } else {
                dirs += File(path, "src")
                dirs += File(path, "src@jvm")
            }
            dirs
        }
        val resourceDirs by lazy {
            val dirs = arrayListOf<File>()
            if (File(path, "src/commonMain/kotlin").isDirectory) {
                dirs += File(path, "src/commonMain/resources")
                dirs += File(path, "src/jvmMain/resources")
            } else {
                dirs += File(path, "resources")
                dirs += File(path, "resources@jvm")
            }
            dirs
        }
    }

    var rootDir: File = File("/temp")
    val buildDirectory get() = File(rootDir, ".korge").absoluteFile
    val filesTxtFile get() = File(buildDirectory, "files.txt")
    val classesDir get() = File(buildDirectory, "classes").absolutePath
    var sourceDirs: Set<File> = emptySet()
    var libs: Set<File> = emptySet()
        set(value) {
            if (field != value) {
                field = value
                snapshot = null
            }
        }

    private var snapshot: ClasspathSnapshotBasedIncrementalCompilationApproachParameters? = null
    private val service = CompilationService.loadImplementation(ClassLoader.getSystemClassLoader())
    private val executionConfig = service.makeCompilerExecutionStrategyConfiguration()
        .useInProcessStrategy()

    private val icWorkingDir by lazy { File(buildDirectory, "ic").also { it.mkdirs() } }
    private val icCachesDir by lazy { File(icWorkingDir, "caches").also { it.mkdirs() } }

    private fun createSnapshots(): ClasspathSnapshotBasedIncrementalCompilationApproachParameters {
        val snapshots = mutableListOf<File>()

        for (lib in libs) {
            if (lib.isFile) {
                val hexDigest = MessageDigest.getInstance("SHA1").digest(lib.readBytes()).toHexString()
                val file = File(icWorkingDir, "dep-" + lib.name + "-$hexDigest.snapshot").absoluteFile
                if (!file.exists()) {
                    val snapshot = service.calculateClasspathSnapshot(lib, ClassSnapshotGranularity.CLASS_MEMBER_LEVEL)
                    println("Saving... $file")
                    file.parentFile.mkdirs()
                    //println(snapshot.classSnapshots)
                    snapshot.saveSnapshot(file)
                } else {
                    //println("Loading... $file")
                }
                snapshots += file
            } else {
                val snapshot = service.calculateClasspathSnapshot(lib, ClassSnapshotGranularity.CLASS_MEMBER_LEVEL)
                val hash = snapshot.classSnapshots.values
                    .filterIsInstance<AccessibleClassSnapshot>()
                    .withIndex()
                    .sumOf { (index, snapshot) -> index * 31 + snapshot.classAbiHash }
                val file = File(icWorkingDir, "dep-$hash.snapshot")
                file.parentFile.mkdirs()
                snapshot.saveSnapshot(file)
                snapshots += file
            }
        }

        val shrunkClasspathSnapshotFile = File(icWorkingDir, "shrunk-classpath-snapshot.bin")
        return ClasspathSnapshotBasedIncrementalCompilationApproachParameters(
            snapshots,
            //emptyList(),
            shrunkClasspathSnapshotFile
        )
    }

    fun getAllFiles(): List<File> {
        return sourceDirs.flatMap { it.walkBottomUp() }.filter { it.extension == "kt" }.map { it.absoluteFile }
    }

    fun getAllFilesToModificationTime(): Map<File, Long> {
        return getAllFiles().associateWith { it.lastModified() }
    }

    private fun saveFileToTime(files: Map<File, Long>): String {
        return files.entries.joinToString("\n") { "${it.key}:::${it.value}" }
    }

    private fun loadFileToTime(text: String): Map<File, Long> {
        return text.split("\n").filter { it.contains(":::") }.map { val (file, time) = it.split(":::"); File(file) to time.toLong() }.toMap()
    }

    fun compileJvm(forceRecompilation: Boolean = false): CompilationResult {
        buildDirectory.mkdirs()
        if (forceRecompilation) {
            filesTxtFile.delete()
        }
        val oldFiles = loadFileToTime(filesTxtFile.takeIf { it.exists() }?.readText() ?: "")
        val allFiles = getAllFilesToModificationTime()
        filesTxtFile.writeText(saveFileToTime(allFiles))
        val sourcesChanges = getModifiedFiles(oldFiles, allFiles)

        if (snapshot == null) {
            snapshot = createSnapshots()
        }

        return service.compileJvm(
            projectId = ProjectId.ProjectUUID(UUID.randomUUID()),
            strategyConfig = executionConfig,
            compilationConfig = service.makeJvmCompilationConfiguration().also { compilationConfig ->
                compilationConfig.useIncrementalCompilation(
                    icCachesDir,
                    sourcesChanges,
                    snapshot!!,
                    compilationConfig.makeClasspathSnapshotBasedIncrementalCompilationConfiguration().also {
                        it.setBuildDir(buildDirectory)
                        it.setRootProjectDir(rootDir)
                        //it.forceNonIncrementalMode(true)
                    }
                )
            },
            //listOf(File("/temp/1")),
            sources = listOf<File>(
                //File("/temp/1"),
                //File("/temp/1-common")
                //File("C:\\Users\\soywiz\\projects\\korge-snake\\src"),
                //File("C:\\Users\\soywiz\\projects\\korge-snake\\modules\\korma-tile-matching\\src\\commonMain\\kotlin"),
            ) + allFiles.map { it.key },
            //listOf(File("/temp/1-common")),
            arguments = listOf(
                "-module-name=${rootDir.name}",
                "-Xjdk-release=17",
                //"-Xuse-fast-jar-file-system",
                "-jvm-target=17",
                "-Xmulti-platform",
                //"-progressive",
                "-language-version=1.9",
                "-api-version=1.9",
                "-no-stdlib",
                "-no-reflect",
                "-Xexpect-actual-classes",
                "-Xenable-incremental-compilation",
                "-classpath=${libs.joinToString(File.pathSeparator) { it.absolutePath }}",
                "-d",
                classesDir,
                //add("-Xfriend-paths=${friendPaths.joinToString(",")}")
                //"C:\\Users\\soywiz\\projects\\korge-snake\\src",
                //"C:\\Users\\soywiz\\projects\\korge-snake\\modules\\korma-tile-matching\\src\\commonMain\\kotlin",
            )
        )
    }

    fun getJavaCommandLine() {

    }

    fun getModifiedFiles(old: Map<File, Long>, new: Map<File, Long>): SourcesChanges.Known {
        val modified = arrayListOf<File>()
        val removed = arrayListOf<File>()
        for ((file, newTime) in new) {
            if (file !in old) {
                removed += file
            } else if (old[file] != newTime) {
                modified += file
            }
        }
        return SourcesChanges.Known(modified, removed)
    }
}


