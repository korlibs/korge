import korlibs.korge.gradle.*
import korlibs.korge.gradle.targets.jvm.*
import org.jetbrains.kotlin.gradle.tasks.*
import java.io.*
import java.net.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.*
import com.soywiz.korio.dynamic.dyn
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.serialization.json.Json
import kotlinx.coroutines.flow.*
import java.io.File
import kotlin.jvm.*

buildscript {
    val korgePluginVersion: String by project

    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap") }
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/temporary") }
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev") }
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven") }
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }

    }
    dependencies {
        classpath("com.soywiz.korlibs.korim:korim-jvm:3.0.0")
        classpath("com.soywiz.korlibs.korge.plugins:korge-gradle-plugin:$korgePluginVersion")
    }
}

apply<KorgeGradlePlugin>()

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

korge {
    id = "com.sample.demo"

// To enable all targets at once

    //targetAll()

// To enable targets based on properties/environment variables
    //targetDefault()

// To selectively enable targets

    targetJvm()
    targetJs()
    targetIos()
    //targetAndroidIndirect() // targetAndroidDirect()
    targetAndroid()

    jvmMainClassName = "RefMainKt"

    //entrypoint("CheckReferences", "CheckReferences")
    //entrypoint("HelloWorld", "HelloWorld")
}


object CheckReferences {
    @JvmStatic
    fun main(folder: File, update: Boolean = false) = Korio {
        var checkedCount = 0

        val errors = arrayListOf<String>()
        fun registerError(error: String) {
            println("!! ERROR: $error")
            errors += error
        }

        for (kind in listOf("jvm")) {
            val generatedFolder = File(File(folder, "build/screenshots"), kind).absoluteFile
            val referenceFolder = File(folder, "references").absoluteFile
            if (!generatedFolder.exists()) {
                println("ComparisonCase.generateCases[$kind]..do not exists. Skipping")
                continue
            }
            println("ComparisonCase.generateCases[$kind]($generatedFolder, $referenceFolder)")
            val cases = ComparisonCase.generateCases(generatedFolder, referenceFolder)
            for (case in cases) {
                if (case.generatedFile == null) continue
                println(" - CASE: $case")
                when {
                    update -> {
                        val outFile = File(referenceFolder, case.generatedFile.name)
                        case.generatedFile.copyTo(outFile, overwrite = true)
                        println(" - COPYING... ${case.generatedFile} -> $outFile")
                    }
                    else -> {
                        if (case.referenceFiles.isEmpty()) {
                            registerError("'${case.generatedFile.name}' doesn't have matching reference files")
                        } else {
                            if (!case.compareResult()) {
                                registerError("'${case.generatedFile.name}' failed comparison")
                            }
                            checkedCount++
                        }
                    }
                }
            }
        }

        if (!update && checkedCount == 0) {
            registerError("Couldn't find any reference to check")
        }

        if (errors.isNotEmpty()) {
            error("There was errors[${errors.size}] : ${errors.joinToString("\n")}")
        }
    }

    data class ComparisonResult(val similarPixelPerfect: Boolean, val isEquals: Boolean, val psnr: Double) {
        //val similar get() = psnr >= 38.0
        //val similar get() = psnr >= 32.0
        val similar get() = psnr >= 30.0
    }

    data class ComparisonConfig(
        val scale: Double = 1.0,
        val pixelPerfect: Boolean = true,
    ) {
        companion object {
            fun fromString(str: String): ComparisonConfig {
                val info = Json.parse(str).dyn
                return ComparisonConfig(info["scale"].double, info["pixelPerfect"].bool)
            }
        }
    }

    data class ComparisonCase(
        val generatedFile: File?,
        val config: ComparisonConfig?,
        val referenceFiles: List<File>
    ) {
        val realConfig = config ?: ComparisonConfig()

        suspend fun compareResult(): Boolean {
            val results = compareResults()
            println(" --> equals=${results.map { it.isEquals }}, similarPixelPerfect=${results.map { it.similarPixelPerfect }}, similar=${results.map { it.similar }}, psnr=${results.map { it.psnr }} :: config=$realConfig")
            if (!results.any { it.similar }) {
                println(" --> ERROR: NOT SIMILAR")
            }

            return results.all { if (realConfig.pixelPerfect) it.isEquals else it.similar }
        }

        suspend fun compareResults(): List<ComparisonResult>{
            val bitmap1 = generatedFile?.toVfs()?.readBitmap()?.toBMP32()?.premultipliedIfRequired() ?: error("No generated file to compare")
            return referenceFiles
                .map { it.toVfs().readBitmap().toBMP32().premultipliedIfRequired() }
                .map { bitmap2 ->
                    val similarPixelPerfect = Bitmap32.matches(bitmap1, bitmap2, threshold = 32)
                    val equals = bitmap1.contentEquals(bitmap2)
                    val psnr = Bitmap32.computePsnr(bitmap1, bitmap2)
                    ComparisonResult(similarPixelPerfect, equals, psnr)
                }
        }

        companion object{
            private fun File.listFilesSure(): List<File> = this.listFiles()?.toList() ?: emptyList()

            fun generateCases(generatedFolder: File, referenceFolder: File): List<ComparisonCase> {
                val generatedFiles = generatedFolder.listFilesSure()
                val referenceFiles = referenceFolder.listFilesSure()

                val generatedNames = generatedFiles.filter { it.name.endsWith(".png") }.map { it.name.substringBefore('.') }.toSet()
                val referenceNames = referenceFiles.filter { it.name.endsWith(".png") }.map { it.name.substringBefore('.') }.toSet()
                val allNames = generatedNames + referenceNames

                //println("generatedFiles=$generatedFiles")
                //println("referenceFiles=$referenceFiles")
                //println("allNames=$allNames")

                return allNames.map { name ->

                    val generatedFile = generatedFiles
                        .filter { it.name.endsWith(".png") }
                        .firstOrNull { it.name.substringBeforeLast('.') == name }

                    val referenceFiles = referenceFiles
                        .filter { it.name.endsWith(".png") }
                        .filter { it.name.substringBeforeLast('.') == name }

                    val config = generatedFile
                        ?.let { File(it.parentFile, it.name.substringBefore('.') + ".json") }
                        ?.takeIf { it.exists() }
                        ?.let { ComparisonConfig.fromString(it.readText()) }

                    ComparisonCase(
                        generatedFile = generatedFile,
                        config = config,
                        referenceFiles = referenceFiles
                    )
                }
            }
        }
    }
}

//println(BuildVersions.KORGE)

// Use MESA OpenGL 32 since Windows Server doesn't have a proper OpenGL implementation on GitHub Actions
// @see: https://amiralizadeh9480.medium.com/how-to-run-opengl-based-tests-on-github-actions-60f270b1ea2c
tasks {
    val localOpengl32X64ZipFile = file("opengl32-x64.zip")
    val downloadOpenglMesaForWindows by creating(Task::class) {
        onlyIf { !localOpengl32X64ZipFile.exists() }
        doLast {
            val url = URL("https://github.com/korlibs/mesa-dist-win/releases/download/21.2.3/opengl32-x64.zip")
            localOpengl32X64ZipFile.writeBytes(url.readBytes())
        }
    }
    val unzipOpenglX64 by creating(Task::class) {
        dependsOn(downloadOpenglMesaForWindows)
        doLast {
            if (!file("opengl32.dll").exists()) {
                copy {
                    from(zipTree(localOpengl32X64ZipFile))
                    into(".")
                }
            }
        }
    }

    val runJvm = getByName("runJvm") as KorgeJavaExec
    runJvm.dependsOn(unzipOpenglX64)
    runJvm.workingDir = File(buildDir, "bin/jvm").also { it.mkdirs() }
    runJvm.environment("OUTPUT_DIR", File(buildDir, "screenshots/jvm"))

    val checkReferencesJvm by creating(Task::class) {
        doLast {
            CheckReferences.main(project.projectDir)
        }
        dependsOn("runJvm")
    }

    val updateReferencesJvm by creating(Task::class) {
        doLast {
            CheckReferences.main(project.projectDir, update = true)
        }
        dependsOn("runJvm")
    }
}
