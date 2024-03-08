package korlibs.io.file

import korlibs.io.net.*

expect val File_separatorChar: Char

fun PathInfo.normalizeAbsolute(): PathInfo {
    val path = this.fullPath
    //val res = path.replace('/', File.separatorChar).trim(File.separatorChar)
    //return if (OS.isUnix) "/$res" else res
    return PathInfo(path.replace('/', File_separatorChar))
}

/**
 * /path\to/file.1.jpg -> MimeType("image/jpeg", listOf("jpg", "jpeg"))
 */
val PathInfo.mimeTypeByExtension: MimeType get() = MimeType.getByExtension(extensionLC)

interface Path {
	val pathInfo: PathInfo
}

val Path.fullPathNormalized: String get() = pathInfo.fullPathNormalized
val Path.folder: String get() = pathInfo.folder
val Path.folderWithSlash: String get() = pathInfo.folderWithSlash
val Path.baseName: String get() = pathInfo.baseName
val Path.fullPathWithoutExtension: String get() = pathInfo.fullPathWithoutExtension
fun Path.fullPathWithExtension(ext: String): String = pathInfo.fullPathWithExtension(ext)
val Path.fullNameWithoutExtension: String get() = pathInfo.fullNameWithoutExtension
val Path.baseNameWithoutExtension: String get() = pathInfo.baseNameWithoutExtension
val Path.fullNameWithoutCompoundExtension: String get() = pathInfo.fullNameWithoutCompoundExtension
val Path.baseNameWithoutCompoundExtension: String get() = pathInfo.baseNameWithoutCompoundExtension
fun Path.baseNameWithExtension(ext: String): String = pathInfo.baseNameWithExtension(ext)
fun Path.baseNameWithCompoundExtension(ext: String): String = pathInfo.baseNameWithCompoundExtension(ext)
val Path.extension: String get() = pathInfo.extension
val Path.extensionLC: String get() = pathInfo.extensionLC
val Path.compoundExtension: String get() = pathInfo.compoundExtension
val Path.compoundExtensionLC: String get() = pathInfo.compoundExtensionLC
val Path.mimeTypeByExtension: MimeType get() = pathInfo.mimeTypeByExtension
fun Path.getPathComponents(): List<String> = pathInfo.getPathComponents()
fun Path.getPathFullComponents(): List<String> = pathInfo.getPathFullComponents()
val Path.fullName: String get() = pathInfo.fullPath

open class VfsNamed(override val pathInfo: PathInfo) : Path
