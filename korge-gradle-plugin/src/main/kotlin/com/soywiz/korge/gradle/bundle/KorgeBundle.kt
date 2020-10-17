package com.soywiz.korge.gradle.bundle

import com.soywiz.korge.gradle.KorgeExtension
import com.soywiz.korge.gradle.gkotlin
import com.soywiz.korge.gradle.util.get
import org.gradle.api.Project
import java.io.File
import java.net.URL

class KorgeBundles(val project: Project) {
    val bundlesDir get() = project.file("bundles").also { it.mkdirs() }
    val logger get() = project.logger

    @JvmOverloads
    fun bundle(zipFile: File, baseName: String? = null) {
        val bundleName = baseName ?: zipFile.name.removeSuffix(".korgebundle")
        val outputDir = project.file("${project.buildDir}/bundles/$bundleName")
        if (!outputDir.exists()) {
            logger.warn("Unzipping $zipFile...")
            project.sync {
                it.from(if (zipFile.isDirectory) project.fileTree(zipFile) else project.zipTree(zipFile))
                it.into(outputDir)
            }
        }
        project.gkotlin.metadata().compilations["main"].kotlinSourceSets.first().kotlin.srcDirs("${project.buildDir}/bundles/${bundleName}/src/commonMain/kotlin")
    }

    @JvmOverloads
    fun bundle(url: java.net.URL, baseName: String? = null) {
        val outFile = bundlesDir["${baseName ?: File(url.path).nameWithoutExtension}.korgebundle"]
        if (!outFile.exists()) {
            logger.warn("Downloading $url...")
            outFile.writeBytes(url.readBytes())
        }
        bundle(outFile, baseName)
    }

    @JvmOverloads
    fun bundleGit(repo: String, folder: String = "", ref: String = "master", bundleName: String? = null) {
        val repoURL = URL(repo)
        val packPath = "${repoURL.host}/${repoURL.path}/$ref"
            .replace("\\", "/")
            .trim('/')
            .replace(Regex("/+"), "/")
            .replace(".git", "")
            .replace("/..", "")

        val packDir = File(bundlesDir, packPath)
        if (!File(packDir, ".git").exists()) {
            packDir.mkdirs()
            logger.warn("Git clonning $repo @ $ref...")
            project.exec {
                it.workingDir(packDir)
                it.commandLine("git", "clone", "--single-branch", "--branch", ref, repo, ".")
            }
        }
        bundle(File(packDir, folder), bundleName)
    }

    @JvmOverloads
    fun bundle(uri: String, baseName: String? = null) {
        when {
            uri.contains(".git") -> {
                val parts = uri.split("::", limit = 3)
                bundleGit(parts[0], parts.getOrElse(1) { "" }, parts.getOrElse(2) { "master" }, parts.getOrNull(3))
            }
            uri.startsWith("http://") || uri.startsWith("https://") -> {
                bundle(URL(uri), baseName)
            }
            else -> {
                bundle(project.file(uri), baseName)
            }
        }
    }

}

