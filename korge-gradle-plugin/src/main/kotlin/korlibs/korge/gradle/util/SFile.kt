package korlibs.korge.gradle.util

import java.io.File

interface SFile {
    val path: String
    val nameWithoutExtension: String get() = File(path).nameWithoutExtension
    val name: String
    val parent: SFile?
    fun mkdirs()
    fun isDirectory(): Boolean
    fun exists(): Boolean
    fun write(text: String)
    fun read(): String

    fun writeBytes(bytes: ByteArray)
    fun readBytes(): ByteArray

    fun list(): List<SFile>
    fun child(name: String): SFile
}

operator fun SFile.get(path: String): SFile? {
    var current: SFile? = this
    for (chunk in path.split('/')) {
        when (chunk) {
            "." -> Unit
            ".." -> current = current?.parent
            else -> current = current?.child(chunk)
        }
    }
    return current
}

class LocalSFile(val file: File, val base: File) : SFile {
    override fun toString(): String = "LocalSFile($file)"

    constructor(file: File) : this(file, file)
    override val path: String by lazy { file.relativeTo(base).toString().replace('\\', '/') }
    override val name: String get() = file.name
    override val parent: LocalSFile? get() = LocalSFile(file.parentFile, base)
    override fun mkdirs() { file.mkdirs() }
    override fun isDirectory(): Boolean = file.isDirectory
    override fun exists(): Boolean = file.exists()

    override fun write(text: String) = file.writeText(text)
    override fun read(): String = file.readText()

    override fun writeBytes(bytes: ByteArray) = file.writeBytes(bytes)
    override fun readBytes(): ByteArray = file.readBytes()

    override fun child(name: String): SFile = LocalSFile(File(file, name), base)
    override fun list(): List<SFile> = (file.listFiles() ?: emptyArray()).map { LocalSFile(it, base) }
}

class MemorySFile(override val name: String, override val parent: MemorySFile? = null) : SFile {
    override val path: String by lazy {
        when {
            parent != null -> "${parent.path}/$name".trim('/')
            else -> name
        }
    }

    var _isDirectory: Boolean = false
    var text: String? = null
    var bytes: ByteArray? = null

    override fun mkdirs() {
        _isDirectory = true
        parent?.mkdirs()
    }

    override fun isDirectory(): Boolean {
        return _isDirectory
    }

    override fun exists(): Boolean {
        return text != null
    }

    override fun write(text: String) {
        this.text = text
        this.bytes = text.toByteArray()
    }

    override fun read(): String {
        return text ?: error("File $path doesn't exist")
    }

    override fun writeBytes(bytes: ByteArray) {
        this.text = ""
        this.bytes = bytes
    }

    override fun readBytes(): ByteArray = bytes ?: error("File $path doesn't exist")

    private val children: ArrayList<SFile> = arrayListOf()
    private val childrenByName: LinkedHashMap<String, SFile> = LinkedHashMap()
    override fun child(name: String): SFile {
        return childrenByName[name] ?: MemorySFile(name, this).also {
            children += it
            childrenByName[name] = it
        }
    }

    override fun list(): List<SFile> = children.toList()
}

fun MemorySFile(files: Map<String, String>): MemorySFile {
    val root = MemorySFile("")
    for (file in files) {
        val rfile = root[file.key]
        rfile?.parent?.mkdirs()
        rfile?.write(file.value)
    }
    return root
}
fun MemorySFile(vararg files: Pair<String, String>) = MemorySFile(files.toMap())
