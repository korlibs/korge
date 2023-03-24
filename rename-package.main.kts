#!/usr/bin/env kotlinc -script -J-Xmx2g

import java.io.*
import java.nio.charset.*

val basePackage = "com.soywiz"

val textsToIgnores = listOf(
    "\"com.soywiz.korlibs.korge.plugins\"",
    "\"com.soywiz.korlibs.korge.reloadagent\"",
    "\"com.soywiz.korlibs.korge2\"",
    "\"com.soywiz.korlibs.\$firstComponent\"",
    "\"com.soywiz.korlibs.korge.plugins:korge-gradle-plugin:\$korgePluginVersion\"",
    "com.soywiz.kmem.Arch.ARM64",
    "\"com.soywiz.korlibs.korim:korim-jvm:3.0.0\"",
    "\"com.soywiz.korlibs.kbignum:kbignum\"",
    "\"com.soywiz.korlibs.kds:kds\"",
    "\"com.soywiz.korlibs.klock:klock\"",
    "\"com.soywiz.korlibs.klogger:klogger\"",
    "\"com.soywiz.korlibs.kmem:kmem\"",
    "\"com.soywiz.korlibs.korau:korau\"",
    "\"com.soywiz.korlibs.korge2:korge\"",
    "\"com.soywiz.korlibs.korge.plugins:korge-gradle-plugin\"",
    "\"com.soywiz.korlibs.korge.reloadagent:korge-reload-agent\"",
    "\"com.soywiz.korlibs.korgw:korgw\"",
    "\"com.soywiz.korlibs.korim:korim\"",
    "\"com.soywiz.korlibs.korinject:korinject\"",
    "\"com.soywiz.korlibs.korio:korio\"",
    "\"com.soywiz.korlibs.korma:korma\"",
    "\"com.soywiz.korlibs.korte:korte\"",
    "\"com.soywiz.korlibs.krypto:krypto\"",
    "\"com.soywiz.korlibs.ktruth:ktruth\"",
    "id = \"com.soywiz.korlibs.korge\"",
    "\"com.soywiz.korlibs.korge.reloadagent:korge-reload-agent:\${BuildVersions.KORGE}\"",
    "\"com.soywiz.korlibs.klock:klock:\${klockVersion}\"",
    "\"com.soywiz.korlibs.kmem:kmem:\${kmemVersion}\"",
    "\"com.soywiz.korlibs.kds:kds:\${kdsVersion}\"",
    "\"com.soywiz.korlibs.krypto:krypto:\${kryptoVersion}\"",
    "\"com.soywiz.korlibs.korge2:korge:\${korgeVersion}\"",
    "\"com.soywiz.korlibs.korma:korma:\${kormaVersion}\"",
    "\"com.soywiz.korlibs.korio:korio:\${korioVersion}\"",
    "\"com.soywiz.korlibs.korim:korim:\${korimVersion}\"",
    "\"com.soywiz.korlibs.korau:korau:\${korauVersion}\"",
    "\"com.soywiz.korlibs.korgw:korgw:\${korgwVersion}\""
)

val fileToIgnore = listOf(
    "e2e-test/buildSrc/src/main/kotlin/CheckReferences.kt"
)

val allowedExtensions = listOf(
    ".kt",
    ".kts",
    ".gradle",
    ".md",
    ".xml",
    ".MF",
    ".pro",
    ".AGFactory",
    ".c",
    ".properties"
)

val projectToParse = listOf(
    "kds",
    "ktruth",
    "osx-metal-playground",
    "korinject",
    "korim",
    "klock",
    "klogger",
    "korgw",
    "korau",
    "korge",
    "gradle",
    "korte",
    "krypto",
    "kmem",
    "kbignum",
    "korio",
    "korge-sandbox",
    "e2e-test",
    "korma",
    "korge-benchmarks",
    "korge-gradle-plugin",
    "korge-reload-agent",
    "buildSrc"
)
val packageChanges = listOf(
    "com.soywiz.klock" to "korlibs.time",
    "com.soywiz.kds" to "korlibs.datastructure",
    "com.soywiz.korinject" to "korlibs.inject",
    "com.soywiz.kmem" to "korlibs.memory",
    "com.soywiz.korte" to "korlibs.template",
    "com.soywiz.kbignum" to "korlibs.bignumber",
    "com.soywiz.krypto" to "korlibs.crypto",
    "com.soywiz.ktruth" to "korlibs.test",
    "com.soywiz.korio" to "korlibs.io",
    "com.soywiz.korma" to "korlibs.math",
    "com.soywiz.korim" to "korlibs.image",
    "com.soywiz.korau" to "korlibs.audio",
    "com.soywiz.korgw" to "korlibs.render",
    "com.soywiz.korag" to "korlibs.graphics",
    "com.soywiz.korge" to "korlibs.korge",
    "com.soywiz.klogger" to "korlibs.logger",
    "com.soywiz.kgl" to "korlibs.kgl",
    "com.soywiz.metal" to "korlibs.metal",
    "com.soywiz.korev" to "korlibs.event",
    "com.soywiz.benchmarks" to "korlibs.benchmarks",
    "com.soywiz.korlibs" to "korlibs",
)
val textToReplace = packageChanges + listOf(
    "build.gradle:501" to "build.gradle:478"
) + packageChanges
    .let { it + it.map { (oldPackage, newPackage) -> oldPackage.replace(".", "/") to newPackage.replace(".", "/") } }

val pathChanges = textToReplace
    .map { (oldPackage, newPackage) -> oldPackage.replace(".", "/") to newPackage.replace(".", "/") } + textToReplace

findKotlinSourceFiles()
    .filter(::isKotlinFile)
    .forEach(::gitMoveIfNeeded)

File("build.gradle")
    .also(::renameOccurrence)

findKotlinSourceFiles()
    .filter(::isAllowedExtension)
    .forEach(::renameOccurrence)

println("scanning any occurrence of $basePackage")

findKotlinSourceFiles()
    .filter(::stillContainingOldPackage)
    .forEach(::println)

"git apply ./patch_fix.patch".run {
    println("fail to apply git patch with error:\n$it")
}

/**** Utilities method ****/

fun gitMoveIfNeeded(file: File) {
    pathChanges.forEach { (oldPath, newPath) ->
        if (file.absolutePath.contains(oldPath)) {
            val newAbsolutePath = file.absolutePath.replace(oldPath, newPath)
            val newAbsolutePathDirectory = File(newAbsolutePath).parentFile.absolutePath
            newAbsolutePathDirectory.createPathIfNeeded()
            val command = "git mv ${file.absolutePath} $newAbsolutePathDirectory"
            command.run(
                onError = { error ->
                    println("${file.absolutePath} match $oldPath will use $newPath")
                    println("$command fail with error :\n$error")
                }
            )
            return
        }
    }
    println("no match with ${file.absolutePath}")
}

fun stillContainingOldPackage(file: File): Boolean {
    return file.readLines(Charset.defaultCharset())
        .any { it.contains(basePackage) }

}

fun isKotlinFile(file: File): Boolean {
    return file.name.endsWith(".kt", ignoreCase = true)
}

fun isEligible(file: File): Boolean {
    var found = false
    file.forEachLine(Charset.defaultCharset()) {
        if (it.contains(basePackage)) {
            found = true
            return@forEachLine
        }
    }
    return found
}

fun File.replaceTexts() {

    val fileContent = readLines(Charset.defaultCharset())
        .joinToString("\n", transform = ::replaceTexts)

    FileWriter(this).use {
        it.write(fileContent)
    }
}

fun replaceTexts(line: String): String {
    if (textsToIgnores.firstOrNull { line.contains(it) } != null) {
        return line
    }

    var newLine = line
    textToReplace.forEach { (oldPackage, newPackage) ->
        newLine = newLine.replace(oldPackage, newPackage)
    }

    return newLine
}

fun findKotlinSourceFiles() = projectToParse
    .asSequence()
    .map(::File)
    .flatMap(File::walk)
    .filter(File::isFile)
    .filter(::notInBuild)
    .filter(::isNotAFileToIgnore)

fun notInBuild(file: File) = (file.absolutePath.contains("/build/")).not()

fun notHiddenFiles(it: File) = it.name.startsWith(".").not()

fun isAllowedExtension(file: File): Boolean {
    return allowedExtensions.any { file.name.endsWith(it) }
}

fun renameOccurrence(file: File) {
    file.replaceTexts()
}

fun String.run(onError: (String) -> Unit) {
    val process = ProcessBuilder()
        .command("bash", "-c", this)
        .start()

    val reader = BufferedReader(InputStreamReader(process.errorStream))
    var errors = mutableListOf<String>()
    var line: String?
    while (reader.readLine().also { line = it } != null) {
        errors.add(line ?: "")
    }

    if (errors.isNotEmpty()) {
        onError(errors.joinToString("\n"))
    }
}

fun String.createPathIfNeeded() {
    File(this).mkdirs()
}

fun isNotAFileToIgnore(file: File) =
    fileToIgnore.none { fileName -> file.absolutePath.contains(fileName) }
