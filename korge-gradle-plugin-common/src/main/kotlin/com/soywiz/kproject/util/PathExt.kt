package com.soywiz.kproject.util

val String.pathInfo: PathInfo get () = PathInfo(this)

class PathInfo private constructor(val fullPath: String, unit: Unit) {
    override fun toString(): String = "PathInfo($fullPath)"
    override fun equals(other: Any?): Boolean = other is PathInfo && this.fullPath == other.fullPath
    override fun hashCode(): Int = fullPath.hashCode()

    companion object {
        operator fun invoke(str: String): PathInfo = PathInfo(normalizePath(str), Unit)

        fun normalizePath(fullPath: String): String {
            val rfullPath = fullPath.replace('\\', '/')
            val isAbsolute = rfullPath.startsWith('/')
            val components = rfullPath.split('/')
            val parts = ArrayList<String>(components.size)
            for (part in components) {
                when (part) {
                    "" -> if (parts.isEmpty()) parts.add("")
                    "." -> Unit
                    ".." -> parts.removeLastOrNull()
                    else -> parts.add(part)
                }
            }
            //if (parts.size == 1 && fullPath.startsWith("/")) return "/${parts[0]}"
            val prep = if (isAbsolute) "/" else ""
            return prep + parts.joinToString("/").trimStart('/')
        }
    }

    val parent: PathInfo get() = PathInfo(fullPath.substringBeforeLast('/', ""))
    val path: String get() = fullPath.substringBeforeLast('/', "")
    val name: String get() = fullPath.substringAfterLast('/')

    fun access(path: String): PathInfo {
        val absoluteAccess = path.startsWith("/")
        return PathInfo(normalizePath(if (absoluteAccess) path else this.fullPath + "/" + path))
    }

}
