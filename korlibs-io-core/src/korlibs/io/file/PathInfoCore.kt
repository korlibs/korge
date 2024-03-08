package korlibs.io.file

import korlibs.datastructure.*
import korlibs.io.lang.*
import kotlin.math.*

// @TODO: inline classes. Once done PathInfoExt won't be required to do clean allocation-free stuff.
inline class PathInfo(val fullPath: String)

fun PathInfo.relativePathTo(relative: PathInfo): String? {
    val thisParts = this.parts().toMutableList()
    val relativeParts = relative.parts().toMutableList()
    val maxNumParts = min(thisParts.size, relativeParts.size)
    val outputParts = arrayListOf<String>()
    val commonCount = count { it < maxNumParts && thisParts[it] == relativeParts[it] }
    while (relativeParts.size > commonCount) {
        relativeParts.removeLast()
        outputParts += ".."
    }
    outputParts += thisParts.slice(commonCount until thisParts.size)
    return outputParts.joinToString("/")
}

val String.pathInfo: PathInfo get() = PathInfo(this)

/**
 * /path\to/file.ext -> /path/to/file.ext
 */
val PathInfo.fullPathNormalized: String get() = fullPath.replace('\\', '/')

/**
 * /path\to/file.ext -> /path\to
 */
val PathInfo.folder: String get() = fullPath.substring(0, fullPathNormalized.lastIndexOfOrNull('/') ?: 0)

/**
 * /path\to/file.ext -> /path/to/
 */
val PathInfo.folderWithSlash: String
    get() = fullPath.substring(0, fullPathNormalized.lastIndexOfOrNull('/')?.plus(1) ?: 0)

/**
 * /path\to/file.ext -> file.ext
 */
val PathInfo.baseName: String get() = fullPathNormalized.substringAfterLast('/')

/**
 * /path\to/file.ext -> /path\to
 */
val PathInfo.parent: PathInfo get() = PathInfo(folder)

/**
 * /path\to/file.ext -> /path\to/file
 */
val PathInfo.fullPathWithoutExtension: String
    get() {
        val startIndex = fullPathNormalized.lastIndexOfOrNull('/')?.plus(1) ?: 0
        return fullPath.substring(0, fullPathNormalized.indexOfOrNull('.', startIndex) ?: fullPathNormalized.length)
    }

/**
 * /path\to/file.ext -> /path\to/file.newext
 */
fun PathInfo.fullPathWithExtension(ext: String): String =
    if (ext.isEmpty()) fullPathWithoutExtension else "$fullPathWithoutExtension.$ext"

/**
 * /path\to/file.1.ext -> file.1
 */
val PathInfo.baseNameWithoutExtension: String get() = baseName.substringBeforeLast('.',
    baseName
)

/**
 * /path\to/file.1.ext -> file
 */
val PathInfo.baseNameWithoutCompoundExtension: String get() = baseName.substringBefore('.',
    baseName
)

/**
 * /path\to/file.1.ext -> /path\to/file.1
 */
val PathInfo.fullNameWithoutExtension: String get() = "$folderWithSlash$baseNameWithoutExtension"

/**
 * /path\to/file.1.ext -> file
 */
val PathInfo.fullNameWithoutCompoundExtension: String get() = "$folderWithSlash$baseNameWithoutCompoundExtension"

/**
 * /path\to/file.1.ext -> file.1.newext
 */
fun PathInfo.baseNameWithExtension(ext: String): String =
    if (ext.isEmpty()) baseNameWithoutExtension else "$baseNameWithoutExtension.$ext"

/**
 * /path\to/file.1.ext -> file.newext
 */
fun PathInfo.baseNameWithCompoundExtension(ext: String): String =
    if (ext.isEmpty()) baseNameWithoutCompoundExtension else "$baseNameWithoutCompoundExtension.$ext"

/**
 * /path\to/file.1.EXT -> EXT
 */
val PathInfo.extension: String get() = baseName.substringAfterLast('.', "")

/**
 * /path\to/file.1.EXT -> ext
 */
val PathInfo.extensionLC: String get() = extension.toLowerCase()

/**
 * /path\to/file.1.EXT -> 1.EXT
 */
val PathInfo.compoundExtension: String get() = baseName.substringAfter('.', "")

/**
 * /path\to/file.1.EXT -> 1.ext
 */
val PathInfo.compoundExtensionLC: String get() = compoundExtension.toLowerCase()

/**
 * /path\to/file.1.ext -> listOf("", "path", "to", "file.1.ext")
 */
fun PathInfo.getPathComponents(): List<String> = fullPathNormalized.split('/')

/**
 * /path\to/file.1.ext -> listOf("/path", "/path/to", "/path/to/file.1.ext")
 */
fun PathInfo.getPathFullComponents(): List<String> {
    val out = arrayListOf<String>()
    for (n in 0 until fullPathNormalized.length) {
        when (fullPathNormalized[n]) {
            '/', '\\' -> {
                out += fullPathNormalized.substring(0, n)
            }
        }
    }
    out += fullPathNormalized
    return out
}

/**
 * /path\to/file.1.ext -> /path\to/file.1.ext
 */
val PathInfo.fullName: String get() = fullPath

fun PathInfo.parts(): List<String> = fullPath.split('/')
fun PathInfo.normalize(removeEndSlash: Boolean = true): String {
    val path = this.fullPath
    val schemeIndex = path.indexOf(":")
    return if (schemeIndex >= 0) {
        val take = if (path.substring(schemeIndex).startsWith("://")) 3 else 1
        path.substring(0, schemeIndex + take) + path.substring(schemeIndex + take).pathInfo.normalize(removeEndSlash = removeEndSlash)
    } else {
        val path2 = path.replace('\\', '/')
        val out = ArrayList<String>()
        val path2PathLength: Int
        for ((index, part) in path2.split("/").also { path2PathLength = it.size }.withIndex()) {
            when (part) {
                "" -> if (out.isEmpty() || !removeEndSlash) out += ""
                "." -> if (index == path2PathLength - 1 && !removeEndSlash) out += ""
                ".." -> if (out.isNotEmpty() && index != 1) out.removeAt(out.size - 1)
                else -> out += part
            }
        }
        out.joinToString("/")
    }
}

fun PathInfo.combine(access: PathInfo): PathInfo {
    val base = this.fullPath
    val access = access.fullPath
    return (if (access.pathInfo.isAbsolute()) access.pathInfo.normalize() else "$base/$access"
        .pathInfo.normalize()).pathInfo
}

fun PathInfo.lightCombine(access: PathInfo): PathInfo {
    val base = this.fullPath
    val access = access.fullPath
    val res = if (base.isNotEmpty()) base.trimEnd('/') + "/" + access.trim('/') else access
    return res.pathInfo
}

fun PathInfo.isAbsolute(): Boolean {
    val base = this.fullPath
    if (base.isEmpty()) return false
    val b = base.replace('\\', '/').substringBefore('/')
    if (b.isEmpty()) return true
    if (b.contains(':')) return true
    return false
}
