package com.soywiz.korge.gradle.bundle

import com.soywiz.korge.gradle.*
import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.util.*
import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.file.*
import java.io.*
import java.net.*
import java.security.*

class KorgeBundles(val project: Project) {
    val bundlesDir get() = project.file("bundles").also { it.mkdirs() }
    val logger get() = project.logger
    val bundles = arrayListOf<BundleInfo>()
    data class BundleInfo(
        val path: File,
        val bundleName: String,
        val repositories: List<BundleRepository>,
        val dependencies: List<BundleDependency>
    ) {
        fun dependenciesForSourceSet(sourceSet: String) = dependencies.filter { it.sourceSet == sourceSet }
        fun dependenciesForSourceSet(sourceSet: Set<String>) = dependencies.filter { it.sourceSet in sourceSet }
    }
    data class BundleRepository(val url: String)
    data class BundleDependency(val sourceSet: String, val artifactPath: String)

    fun sha256Tree(tree: FileTree): String {
        val files = LinkedHashMap<String, File>()
        tree.visit {
            if (!it.isDirectory) {
                val mpath = it.path.trim('/')
                val rpath = "/$mpath"
                when {
                    rpath.contains("/.git") -> Unit
                    rpath.endsWith("/.DS_Store") -> Unit
                    rpath.endsWith("/thumbs.db") -> Unit
                    else -> files[mpath] = it.file
                }
            }
        }
        val digest = MessageDigest.getInstance("SHA-256")
        for (fileKey in files.keys.toList().sorted()) {
            val file = files[fileKey]!!
            val hashString = "$fileKey[${file.length()}]"
            digest.update(hashString.toByteArray(Charsets.UTF_8))
            digest.update(file.readBytes())
            if (logger.isInfoEnabled) {
                logger.info("SHA256: $hashString: ${(digest.clone() as MessageDigest).digest().hex}")
            }
        }
        return digest.digest().hex
    }

    val buildDirBundleFolder = "korge-bundles"

    @JvmOverloads
    fun bundle(zipFile: File, baseName: String? = null, checkSha256: String? = null) {
        val bundleName = baseName ?: zipFile.name.removeSuffix(".korgebundle")
        val outputDir = project.file("${project.buildDir}/$buildDirBundleFolder/$bundleName")
        val tree = if (zipFile.isDirectory) project.fileTree(zipFile) else project.zipTree(zipFile)
        val computedSha256 = sha256Tree(tree)

        if (checkSha256.isNullOrEmpty()) {
            println("Bundle $bundleName hash missing; calculated SHA256: $computedSha256")
        }

        if (!outputDir.exists()) {
            logger.warn("KorGE.bundle: Extracting $zipFile...")

            when {
                checkSha256 == null -> logger.warn("  - Security WARNING! Not checking SHA256 for bundle $bundleName. That should be SHA256 = $computedSha256")
                checkSha256 != computedSha256 -> error("Bundle '$bundleName' expects SHA256 = $checkSha256 , but found SHA256 = $computedSha256")
                else -> logger.info("Matching bundle SHA256 = $computedSha256")
            }
            //println("SHA256: ${sha256Tree(tree)}")

            project.sync {
                it.from(tree)
                it.into(outputDir)
            }
        } else {
            logger.info("KorGE.bundle: Already unzipped $zipFile")
        }

        val repositories = arrayListOf<BundleRepository>()
        val dependencies = arrayListOf<BundleDependency>()
        val dependenciesTxtFile = File(outputDir, "dependencies.txt")
        if (dependenciesTxtFile.exists()) {
            for (rline in dependenciesTxtFile.readLines()) {
                val line = rline.trim()
                if (line.startsWith("#")) continue
                val (key, value) = line.split(":", limit = 2).map { it.trim() }.takeIf { it.size >= 2 } ?: continue
                if (key == "repository") {
                    repositories.add(BundleRepository(value))
                } else {
                    dependencies.add(BundleDependency(key, value))
                }
            }
        }

        bundles += BundleInfo(
            path = outputDir,
            bundleName = bundleName,
            repositories = repositories,
            dependencies = dependencies,
        )

        project.afterEvaluate {
            for (repo in repositories) {
                logger.info("KorGE.bundle.repository: $repo")
                project.repositories.maven {
                    it.url = project.uri(repo.url)
                }
            }
            for (dep in dependencies) {
                //val available = dep.sourceSet in project.configurations
                val available = try {
                    project.configurations.getAt(dep.sourceSet) != null
                } catch (e: UnknownConfigurationException) {
                    false
                }
                logger.info("KorGE.bundle.dependency: $dep -- available=$available")
                if (available) {
                    project.dependencies.add(dep.sourceSet, dep.artifactPath)
                }
            }
            logger.info("KorGE.bundle: $outputDir")
            for (target in project.gkotlin.targets) {
                logger.info("  target: $target")
                target.compilations.all { compilation ->
                    logger.info("    compilation: $compilation")

                    val sourceSets = compilation.kotlinSourceSets.toMutableSet()

                    for (sourceSet in sourceSets) {
                        logger.info("      sourceSet: $sourceSet")

                        fun addSource(ss: SourceDirectorySet, sourceSetName: String, folder: String) {
                            val folder = File(project.buildDir, "$buildDirBundleFolder/${bundleName}/src/${sourceSetName}/$folder")
                            if (folder.exists()) {
                                logger.info("        ${ss.name}Src: $folder")
                                ss.srcDirs(folder)
                            }
                        }

                        fun addSources(sourceSetName: String) {
                            addSource(sourceSet.kotlin, sourceSetName, "kotlin")
                            addSource(sourceSet.resources, sourceSetName, "resources")
                        }

                        fun addSourcesAddSuffix(sourceSetName: String) {
                            when {
                                sourceSet.name.endsWith("Test") -> addSources("${sourceSetName}Test")
                                sourceSet.name.endsWith("Main") -> addSources("${sourceSetName}Main")
                            }
                        }

                        addSources(sourceSet.name)
                        if (target.isNative) addSourcesAddSuffix("nativeCommon")
                        if (target.isNativeDesktop) addSourcesAddSuffix("nativeDesktop")
                        if (target.isNativePosix) addSourcesAddSuffix("nativePosix")
                        if (target.isNativePosix && !target.isApple) addSourcesAddSuffix("nativePosixNonApple")
                        if (target.isApple) addSourcesAddSuffix("nativePosixApple")
                        if (target.isIosTvosWatchos) addSourcesAddSuffix("iosWatchosTvosCommon")
                        if (target.isWatchos) addSourcesAddSuffix("iosWatchosCommon")
                        if (target.isTvos) addSourcesAddSuffix("iosTvosCommon")
                        if (target.isMacos || target.isIosTvos) addSourcesAddSuffix("macosIosTvosCommon")
                        if (target.isMacos || target.isIosWatchos) addSourcesAddSuffix("macosIosWatchosCommon")
                        if (target.isIos) addSourcesAddSuffix("iosCommon")
                    }
                }
            }
        }
        //project.gkotlin.sourceSets.configureEach {
        //    logger.info("KorGE.sourceSet: ${it.name}")
        //}
        //println(project.gkotlin.metadata().compilations["main"].kotlinSourceSets)
    }

    @JvmOverloads
    fun bundle(url: java.net.URL, baseName: String? = null, checkSha256: String? = null) {
        val outFile = bundlesDir["${baseName ?: File(url.path).nameWithoutExtension}.korgebundle"]
        if (!outFile.exists()) {
            logger.warn("KorGE.bundle: Downloading $url...")
            outFile.writeBytes(url.readBytes())
        } else {
            logger.info("KorGE.bundle: Already downloaded $url")
        }
        bundle(outFile, baseName, checkSha256)
    }

    @JvmOverloads
    fun bundleGit(repo: String, folder: String = "", ref: String = "master", bundleName: String? = null, checkSha256: String? = null) {
        val repoURL = URL(repo)
        val packPath = "${repoURL.host}/${repoURL.path}/$ref"
            .replace("\\", "/")
            .trim('/')
            .replace(Regex("/+"), "/")
            .replace(".git", "")
            .replace("/..", "")

        val packDir = File(bundlesDir, packPath)
        val packEnsure = File(bundlesDir, "$packPath.refname")
        val existsDotGitFolder = File(packDir, ".git").exists()
        val matchingReg = packEnsure.takeIf { it.exists() }?.readText() == ref

        if (!matchingReg && !existsDotGitFolder) {
            packDir.mkdirs()
            logger.warn("KorGE.bundle: Git cloning $repo @ $ref...")
            project.exec {
                it.workingDir(packDir)
                it.commandLine("git", "-c", "core.autocrlf=false", "clone", repo, ".")
            }.assertNormalExitValue()
        } else {
            logger.info("KorGE.bundle: Already cloned $repo")
        }

        if (!matchingReg) {
            project.exec {
                it.workingDir(packDir)
                it.commandLine("git", "-c", "core.autocrlf=false", "reset", "--hard", ref)
            }.assertNormalExitValue()
            project.delete {
                it.delete(File(packDir, ".git"))
            }
            packEnsure.writeText(ref)
        } else {
            logger.info("KorGE.bundle: Already at reference $ref @ $repo")
        }


        bundle(File(packDir, folder), bundleName, checkSha256)
    }

    @JvmOverloads
    fun bundle(fullUri: String, baseName: String? = null) {
        val (uri, ssha256) = (fullUri.split("##", limit = 2) + listOf(""))
        val sha256 = ssha256.takeIf { it.isNotEmpty() }
        when {
            uri.contains(".git") -> {
                val parts = uri.split("::", limit = 3)
                bundleGit(parts[0], parts.getOrElse(1) { "" }, parts.getOrElse(2) { "master" }, parts.getOrNull(3), checkSha256 = sha256)
            }
            uri.startsWith("http://") || uri.startsWith("https://") -> {
                bundle(URL(uri), baseName, checkSha256 = sha256)
            }
            else -> {
                bundle(project.file(uri), baseName, checkSha256 = sha256)
            }
        }
    }

    fun getPaths(name: String, resources: Boolean, test: Boolean): Set<File> {
        val lfolder = if (resources) "resources" else "kotlin"
        val lmain = if (test) "Test" else "Main"
        return bundles.flatMap { bundle ->
            listOf(File(bundle.path, "src/${name}$lmain/$lfolder"))
        }.filter { it.isDirectory && it.exists() }.toSet()
    }
}

