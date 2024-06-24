@file:OptIn(ExperimentalBuildToolsApi::class)

package korlibs.korge.kotlincompiler

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.jvm.*
import org.jetbrains.kotlin.daemon.common.*
import org.w3c.dom.*
import java.io.*
import java.net.*
import java.security.*
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
            repeat(10) {
                run {
                    val compiler = KorgeKotlinCompiler()
                    compiler.buildDirectory = File("C:\\temp\\.kotlin")
                    compiler.rootDir = File("C:\\temp")
                    compiler.sourceDirs = setOf(
                        File("C:\\temp\\1"),
                        File("C:\\temp\\1-common"),
                    )
                    compiler.libs = compiler.filesForMaven(
                        MavenArtifact("org.jetbrains.kotlin", "kotlin-stdlib", "2.0.0"),
                        MavenArtifact("com.soywiz.korge", "korge-jvm", "999.0.0.999")
                    )

                    repeat(1) { NN ->
                        println(measureTimeMillis {
                            println(compiler.compileJvm(forceRecompilation = NN == 0))
                            //println(compiler.compileJvm(forceRecompilation = true))
                        })
                    }
                }

                val compiler = KorgeKotlinCompiler()
                compiler.buildDirectory = File("C:\\Users\\soywiz\\projects\\korge-snake\\.kotlin")
                compiler.rootDir = File("C:\\Users\\soywiz\\projects\\korge-snake")
                compiler.sourceDirs = setOf(
                    File("C:\\Users\\soywiz\\projects\\korge-snake\\src"),
                    File("C:\\Users\\soywiz\\projects\\korge-snake\\modules\\korma-tile-matching\\src\\commonMain\\kotlin"),
                )
                compiler.libs = compiler.filesForMaven(
                    MavenArtifact("org.jetbrains.kotlin", "kotlin-stdlib", "2.0.0"),
                    MavenArtifact("com.soywiz.korge", "korge-jvm", "999.0.0.999")
                )

                repeat(4) { NN ->
                    println(measureTimeMillis {
                        println(compiler.compileJvm(forceRecompilation = NN == 0))
                        //println(compiler.compileJvm(forceRecompilation = true))
                    })
                }
            }
        }
    }

    fun filesForMaven(vararg artifacts: MavenArtifact): Set<File> = artifacts.flatMap { filesForMaven(it) }.toSet()
    fun filesForMaven(artifacts: List<MavenArtifact>): Set<File> = artifacts.flatMap { filesForMaven(it) }.toSet()
    fun filesForMaven(artifact: MavenArtifact): Set<File> = MavenTools.getMavenArtifacts(artifact)

    var buildDirectory = File("/temp/build").absoluteFile
    var rootDir: File = File("/temp")
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
        val filesTxt = File(buildDirectory, "files.txt")
        if (forceRecompilation) {
            filesTxt.delete()
        }
        val oldFiles = loadFileToTime(filesTxt.takeIf { it.exists() }?.readText() ?: "")
        val allFiles = getAllFilesToModificationTime()
        filesTxt.writeText(saveFileToTime(allFiles))
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
                "-module-name=korge-snake",
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
                File(buildDirectory, "classes").absolutePath,
                //add("-Xfriend-paths=${friendPaths.joinToString(",")}")
                //"C:\\Users\\soywiz\\projects\\korge-snake\\src",
                //"C:\\Users\\soywiz\\projects\\korge-snake\\modules\\korma-tile-matching\\src\\commonMain\\kotlin",
            )
        )
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
    fun getMavenArtifacts(artifact: MavenArtifact, explored: MutableSet<MavenArtifact> = mutableSetOf()): Set<File> {
        val explore = ArrayDeque<MavenArtifact>()
        explore += artifact
        val out = mutableSetOf<MavenArtifact>()
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
        return out.map { getSingleMavenArtifact(it) }.toSet()
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
