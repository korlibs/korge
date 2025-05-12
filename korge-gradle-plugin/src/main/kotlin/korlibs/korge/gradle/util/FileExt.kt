package korlibs.korge.gradle.util

import java.io.File
import java.nio.charset.*

val File.absolutePathWithSlash: String get() = if (this.isDirectory) "$absolutePath/" else absolutePath

fun File.isDescendantOf(base: File): Boolean {
    return this.absolutePathWithSlash.startsWith(base.absolutePathWithSlash)
}

fun File.takeIfExists() = this.takeIf { it.exists() }
fun File.takeIfNotExists() = this.takeIf { !it.exists() }
fun File.ensureParents() = this.apply { parentFile.mkdirs() }
fun <T> File.conditionally(ifNotExists: Boolean = true, block: File.() -> T): T? = if (!ifNotExists || !this.exists()) block() else null
fun <T> File.always(block: File.() -> T): T = block()
operator fun File.get(name: String) = File(this, name)

fun File.getFirstRegexOrNull(regex: Regex): File? = this
    .listFiles { dir, name -> regex.containsMatchIn(name) }
    ?.firstOrNull()

fun File.getFirstRegexOrFail(regex: Regex) = this.getFirstRegexOrNull(regex)
    ?: error("Can't find file matching '$regex' in folder '$this'")

fun File.writeTextIfChanged(text: String, charset: Charset = Charsets.UTF_8) {
    val originalText = this.takeIf { it.exists() }?.readText(charset)
    if (originalText != text) {
        if (!parentFile.isDirectory) parentFile.mkdirs()
        writeText(text, charset)
    }
}

fun File.writeBytesIfChanged(bytes: ByteArray) {
    val originalBytes = this.takeIf { it.exists() }?.readBytes()
    if (originalBytes == null || !bytes.contentEquals(originalBytes)) {
        writeBytes(bytes)
    }
}

class FileList(val files: List<File>) : Collection<File> by files {
    constructor(vararg files: File) : this(files.toList())
    fun exists() = files.any { it.exists() }
    fun exists(name: String) = files.any { it[name].exists() }
    val firstExistantFile: File? get() = files.firstOrNull { it.exists() }
    fun takeIfExists() = firstExistantFile
}
