package com.soywiz.kproject.model

import com.soywiz.kproject.util.*
import java.io.*

sealed interface Dependency : Comparable<Dependency> {
    val projectName: String
    val version: Version

    override fun compareTo(other: Dependency): Int = version.compareTo(other.version)

    companion object {
        val MAX_VERSION = Version("999.999.999.999")
    }
}

data class GitDependency(
    val name: String,
    val repo: GitRepository,
    val path: String,
    val ref: String,
    val commit: String? = null,
    val hash: String? = null,
) : Dependency {
    val gitWithPathAndRef by lazy { GitRepositoryWithPathAndRef(repo, path, ref) }
    override val version: Version get() = Version(ref)
    override val projectName: String get() = name

    val file: GitFileRef = GitFileRef(repo, ref, path)

    val commitCount: Int by lazy { gitWithPathAndRef.getContent().commitCount }

    override fun compareTo(other: Dependency): Int {
        if (other is GitDependency) {
            return gitWithPathAndRef.compareTo(other.gitWithPathAndRef)
        }
        return super.compareTo(other)
    }

}

data class MavenDependency(
    val group: String,
    val name: String,
    override val version: Version,
    val target: String = "common",
) : Dependency {
    val hasVersion get() = version.str.isNotBlank()
    val coordinates: String = if (hasVersion) "$group:$name:${version.str}" else "$group:$name"
    override val projectName: String = "$group-$name"

    companion object {
        fun fromCoordinates(coordinates: String, target: String = "common"): MavenDependency {
            val coords = coordinates.split(':')
            val group = coords[0]
            val name = coords[1]
            val version = coords.getOrNull(2) ?: ""
            return MavenDependency(group, name, Version(version), target)
        }
    }
}

data class FileRefDependency(
    val path: FileRef,
) : Dependency {
    override val projectName: String = when {
        path.name.endsWith(".kproject.yml") -> path.name.removeSuffix(".kproject.yml")
        path.name.endsWith("kproject.yml") -> path.parent().name
        else -> path.name
    }
    override val version: Version get() = Dependency.MAX_VERSION
}

private val GITHUB_TREE_REGEX = Regex("(https://github\\.com\\/.*?\\/.*?)\\/tree\\/(.*?)\\/(.*)")

fun Dependency.Companion.parseString(str: String, projectFile: FileRef = MemoryFileRef()): Dependency {
    try {
        val parts = str.split("::")
        val firstPart = parts.first()
        when (firstPart) {
            // - git::adder::korlibs/kproject::/modules/adder::54f73b01cea9cb2e8368176ac45f2fca948e57db
            // - https://github.com/korlibs/korge-parallax/tree/dacd7f4c430c48349565295394f723b05841c54a/korge-parallax
            "git" -> {
                val (_, name, coordinates, path, ref) = if (parts.size == 4) parts.take(1) + "unknown" + parts.drop(1) else parts
                return GitDependency(name, GitRepository("https://github.com/${coordinates.pathInfo.fullPath}.git"), path, ref, commit = parts.getOrNull(5), hash = parts.getOrNull(6))
            }
            // - maven::common::org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4
            "maven" -> {
                val (_, target, coordinates) = parts
                return MavenDependency.fromCoordinates(coordinates, target)
            }
            else -> when {
                // - git@github.com:korlibs/korge-ext.git/korge-tiled#0.0.1::734d96ccc18733064ef9fbda8ac359585011112d
                GITHUB_TREE_REGEX.matches(firstPart) || firstPart.contains(".git") -> {
                    val firstPart2 = GITHUB_TREE_REGEX.matchEntire(firstPart)?.let {
                        val (_, repo, ref, path) = it.groupValues
                        "$repo.git/$path#$ref"
                    } ?: firstPart

                    val repo = firstPart2.substringBefore(".git") + ".git"
                    val path = PathInfo(firstPart2.substringAfter(".git").substringBefore('#').takeIf { it.isNotBlank() } ?: "/").fullPath
                    val ref = PathInfo(firstPart2.substringAfter('#', "").takeIf { it.isNotEmpty() } ?: error("Missing ref as #")).fullPath
                    val name = PathInfo(File(path).name.removeSuffix(".git").takeIf { it.isNotEmpty() } ?: File(repo).name.removeSuffix(".git")).fullPath
                    return GitDependency(name, GitRepository(repo), path, ref, commit = parts.getOrNull(1), hash = parts.getOrNull(2))
                }
                // org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4
                firstPart.contains(':') -> {
                    val (group, name, version) = firstPart.split(":") + listOf("")
                    return MavenDependency(group, name, Version(version), parts.getOrNull(1) ?: "common")
                }
                // ../korge-tiled
                parts.size == 1 -> {
                    val rfirstPart = when {
                        firstPart.endsWith(".kproject.yml") -> firstPart
                        else -> "$firstPart/kproject.yml"
                    }
                    val file = projectFile.parent()[rfirstPart]
                    return FileRefDependency(file)
                }
            }
        }
        error("Don't know how to handle '$str'")
    } catch (e: Throwable) {
        throw IllegalArgumentException("""
            Invalid format for string '$str' : ${e.message}

            Supported formats:
            ## FOLDER:
            - ../korge-tiled
            
            ## GIT:
            - git::adder::korlibs/kproject::/modules/adder::54f73b01cea9cb2e8368176ac45f2fca948e57db
            - git@github.com:korlibs/korge-ext.git/korge-tiled#0.0.1::734d96ccc18733064ef9fbda8ac359585011112d
            - "https://github.com/korlibs/korge-ext.git/korge-tiled#0.0.1::734d96ccc18733064ef9fbda8ac359585011112d"
            - "https://github.com/korlibs/korge-parallax/tree/0.0.1/korge-parallax::dacd7f4c430c48349565295394f723b05841c54a"
            
            ## MAVEN:
            - maven::common::org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4
            - com.soywiz.korlibs.korge2:korge
            - org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4
            - org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4::common
            - org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4::jvm
        """.trimIndent(), e)
    }
}

fun Dependency.Companion.parseObject(any: Any?, projectFile: FileRef = MemoryFileRef()): Dependency {
    return when (any) {
        is String -> parseString(any, projectFile)
        else -> TODO("Unsupported dependency $any")
    }
}
