package com.soywiz.kproject.model

import com.soywiz.kproject.internal.*
import com.soywiz.kproject.util.*
import com.soywiz.kproject.version.*

class NewKProjectGradleGenerator(val projectRootFolder: FileRef) {
    val resolver = NewKProjectResolver()
    var rootPath: String? = null
    var rootDepWithProj: NewKProjectResolver.DependencyWithProject? = null

    data class ResolveDep(val file: FileRef, val folder: FileRef = file.parent())
    data class ProjectRef(val projectName: String, val projectDir: FileRef)

    fun generate(path: String) : List<ProjectRef> {
        val depWithProj = resolver.load(projectRootFolder[path])
        if (rootPath == null) {
            rootPath = path
            rootDepWithProj = depWithProj
        }

        val outProjects = arrayListOf<ProjectRef>()
        for (project in resolver.getAllProjects().values) {
            val file = resolveAndGetProjectFileRefFromDependency(project.dep, project.name)

            val buildGradleFile = file.folder[
                when (file.file.name) {
                    "kproject.yml" -> "build.gradle"
                    else -> file.file.name.removeSuffix(".kproject.yml") + "/build.gradle"
                }
            ]

            val proj = project.project
            if (proj != null) {
                val projSrc = proj.src
                if (projSrc != null) {
                    val srcFileRef = buildGradleFile.parent()["src"]
                    when (projSrc) {
                        is GitDependency -> {
                            val content = projSrc.getCachedContentWithLockCheck()
                            unzipTo(srcFileRef, content.zipFile)
                        }
                        is FileRefDependency -> {
                            (projSrc.path as LocalFileRef).file.copyRecursively((srcFileRef as LocalFileRef).file)
                        }
                        else -> TODO("Unsupported dependency")
                    }
                }
                //println("${proj.src}")

                outProjects += ProjectRef(project.name, buildGradleFile.parent())
                if (!buildGradleFile.parent()[".gitignore"].exists()) {
                    buildGradleFile.parent()[".gitignore"] = buildString {
                        if (projSrc != null) appendLine("/src")
                        appendLine("/.idea")
                        appendLine("/.gradle")
                        appendLine("/build")
                        appendLine("/build.gradle")
                    }
                }
                buildGradleFile.writeText(buildString {
                    appendLine("buildscript {")
                    appendLine("    repositories {")
                    appendLine("        mavenLocal(); mavenCentral(); google(); gradlePluginPortal()")
                    appendLine("        maven { url = uri(\"https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev\") }")
                    appendLine("        maven { url = uri(\"https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental\") }")
                    appendLine("    }")
                    appendLine("}")

                    appendLine("plugins {")
                    //appendLine("  id(\"com.soywiz.kproject\") version \"${KProjectVersion.VERSION}\"")
                    appendLine("  id(\"com.soywiz.kproject\")")

                    val gradlePlugins = proj.plugins.filterIsInstance<GradlePlugin>().map { it.name }

                    for (plugin in gradlePlugins) {
                        when (plugin) {
                            "serialization" -> {
                                appendLine("  id(\"org.jetbrains.kotlin.plugin.serialization\")")
                            }

                            else -> {
                                val parts = plugin.split(":", limit = 2)
                                appendLine(buildString {
                                    append("  id(\"${parts.first()}\")")
                                    if (parts.size >= 2) {
                                        append("  version \"${parts.last()}\"")
                                    }
                                })
                            }
                        }
                    }
                    appendLine("}")
                    //for (gradle in this@KProject.gradleNotNull) {
                    //    appendLine(gradle)
                    //}

                    appendLine("dependencies {")
                    for (plugin in gradlePlugins) {
                        when (plugin) {
                            "serialization" -> {
                                appendLine("  add(\"commonMainApi\", \"org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0\")")
                            }
                        }
                    }
                    for ((depName, deps) in listOf(
                        "Main" to project.dependencies,
                        "Test" to project.testDependencies,
                    )) {
                        for (dep in deps) {
                            val ddep = dep.dep
                            when (ddep) {
                                is MavenDependency -> {
                                    val versionRef = rootDepWithProj?.project?.versions?.get(ddep.coordinates)
                                    //println("ddep.coordinates=${ddep.coordinates}, versionRef=$versionRef :: ${rootDepWithProj?.project?.versions}")
                                    val rddep = when {
                                        ddep.hasVersion -> ddep
                                        else -> ddep.copy(version = Version(rootDepWithProj?.project?.versions?.get(ddep.coordinates) ?: ""))
                                    }

                                    appendLine("  add(\"${ddep.target}${depName}Api\", ${rddep.coordinates.quoted})")
                                }
                                else -> {
                                    appendLine("  add(\"common${depName}Api\", project(${":${dep.name}".quoted}))")
                                }
                            }
                        }
                    }
                    appendLine("}")

                    appendLine("[file(\"build.extra.gradle\"), file(\"build.extra.gradle.kts\")].each { extraGradle ->")
                    appendLine("  if (extraGradle.exists()) apply from: extraGradle")
                    appendLine("}")
                })
            }
            //println("file=$file")
            //println("buildGradleFile=$buildGradleFile")
        }

        lockWrite()

        return outProjects
    }

    fun resolveAndGetProjectFileRefFromDependency(dependency: Dependency, projectName: String = dependency.projectName): ResolveDep {
        return when (dependency) {
            is FileRefDependency -> {
                if (dependency.path is GitFileRef) {
                    val path = dependency.path
                    return resolveAndGetProjectFileRefFromDependency(GitDependency(projectName, path.git, path.path.pathInfo.fullPath, path.ref), projectName)
                }
                ResolveDep(dependency.path)
            }
            is GitDependency -> {
                //println("projectName=$projectName, dependency=$dependency")
                val targetFolder = projectRootFolder["modules/${projectName}"]
                val pathInfo = dependency.path.pathInfo
                val content = dependency.getCachedContentWithLockCheck()
                val gitArchiveFileRef = targetFolder[".gitarchive"]

                if (!targetFolder.exists() || !gitArchiveFileRef.exists() || (gitArchiveFileRef.exists() && gitArchiveFileRef.readText() != content.ref)) {
                    targetFolder.deleteTree()
                    unzipTo(targetFolder, content.zipFile)
                    gitArchiveFileRef.writeText(content.ref)
                }
                ResolveDep(when {
                    pathInfo.isFinalFile -> targetFolder[pathInfo.name]
                    else -> targetFolder["kproject.yml"]
                })
            }
            else -> TODO()
        }
    }

    val PathInfo.isFinalFile: Boolean get() = name.endsWith("kproject.yml")

    fun GitDependency.getCachedContentWithLockCheck(): GitRepositoryWithPathAndRef.Content {
        val dependency = this
        val pathInfo = dependency.path.pathInfo
        val folder = when {
            pathInfo.isFinalFile -> pathInfo.parent
            else -> pathInfo
        }
        val gitWithPathAndRef = dependency.gitWithPathAndRef.copy(path = folder.fullPath)
        //println(gitWithPathAndRef.path)
        val content = gitWithPathAndRef.getContent()

        // Lock checking
        val refString = "${dependency.repo.httpsRepo}/${dependency.path.trim('/')}#${dependency.ref}"
        val checkString = "${content.commitId}:${content.hash}"
        lockCheck(refString, checkString)
        return content
    }

    private var lockMapLoaded = false
    private val lockMap: LinkedHashMap<String, String> = LinkedHashMap()
    private val kprojectLockFile = projectRootFolder["kproject.lock"]
    fun lockLoad() {
        if (!lockMapLoaded) {
            lockMapLoaded = true
            if (kprojectLockFile.exists()) {
                for (line in kprojectLockFile.readText().split("\n")) {
                    if (!line.contains(":::")) continue
                    val (key, value) = line.trim().split(":::")
                    lockMap[key.trim()] = value.trim()
                }
            }
        }
    }

    fun lockCheck(refString: String, checkString: String) {
        lockLoad()
        val usedCheckString = lockMap[refString]
        //println("lockCheck: $refString : $checkString : $usedCheckString")

        if (usedCheckString != null && usedCheckString != checkString) {
            error("Failed check for $refString:\nactual=$usedCheckString\nexpect=$checkString")
        }
        lockMap[refString] = checkString
    }
    fun lockWrite() {
        if (lockMap.isNotEmpty() || kprojectLockFile.exists()) {
            kprojectLockFile.writeText(lockMap.toList().sortedBy { it.first }.map { it.first + " ::: " + it.second }
                .joinToString("\n") + "\n")
        }
    }
}
