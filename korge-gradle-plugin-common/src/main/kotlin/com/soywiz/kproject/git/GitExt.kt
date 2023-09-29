package com.soywiz.kproject.git

import com.soywiz.kproject.util.*
import org.eclipse.jgit.api.*
import org.eclipse.jgit.api.errors.*
import org.eclipse.jgit.errors.*
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.revwalk.*
import org.eclipse.jgit.treewalk.*
import java.io.*
import java.util.zip.*

fun Git.doPull() {
    this.pull()
        .setProgressMonitor(TextProgressMonitor(PrintWriter(System.out)))
        .call()
}

fun RevWalk.getCommit(git: Git, ref: String): RevCommit {
    val repo = git.repository
    val walk = this
    return try {
        walk.parseCommit(repo.resolve(ref))
        //} catch (e: MissingObjectException) {
    } catch (e: Throwable) {
        git.doPull()
        try {
            walk.parseCommit(repo.resolve(ref))
            //} catch (e: MissingObjectException) {
        } catch (e: Throwable) {
            throw Exception("Can't find ref '$ref' in $repo", e)
        }
    }
}

fun Git.readFile(ref: String, filePath: String): ByteArray {
    val repo = repository
    RevWalk(repo).use { walk ->
        val commit: RevCommit = walk.getCommit(this, ref)
        val tree: RevTree = commit.tree
        TreeWalk.forPath(repo, filePath.trimStart('/'), tree).use { treeWalk ->
            if (treeWalk != null) {
                val objectId: ObjectId = treeWalk.getObjectId(0)
                val loader = repo.open(objectId)

                // Read the file content as a string
                return loader.bytes
            } else {
                error("Can't find file '$filePath' in '$ref' in $this")
            }
        }
    }
}

fun Git.countCommits(ref: String? = null): Int {
    return log()
        .also { if (ref != null) it.add(repository.resolve(ref)) }
        .call()
        .count()
}

fun Git.checkRelMatches(rel1: String, rel2: String): Boolean {
    return repository.resolve(rel1) == repository.resolve(rel2)
}

fun Git.archiveZip(
    path: String,
    rel: String,
): ByteArray {
    val path = if (path.isBlank()) "/" else path

    class ExtZipOutputStream(out: OutputStream, val params: Map<String?, Any?>?) : ZipOutputStream(out) {
        var removePrefix: String? = params?.getOrElse("removePrefix") { null }?.toString()
    }

    class ZipArchiveFormat : ArchiveCommand.Format<ExtZipOutputStream> {
        override fun suffixes(): Iterable<String> = setOf(".mzip")
        override fun createArchiveOutputStream(s: OutputStream): ExtZipOutputStream = createArchiveOutputStream(s, null)
        override fun createArchiveOutputStream(s: OutputStream, o: Map<String?, Any?>?): ExtZipOutputStream = ExtZipOutputStream(s, o).also { it.setLevel(1) }
        override fun putEntry(out: ExtZipOutputStream, tree: ObjectId, path: String, mode: FileMode, loader: ObjectLoader?) {
            //println("ZIP: $path -- ${loader?.bytes?.size}")
            if (loader == null) return
            // loader is null for directories...
            val entry = ZipEntry(path.trim('/').removePrefix(out.removePrefix?.trim('/') ?: "").trim('/'))
            out.putNextEntry(entry)
            out.write(loader.bytes)
            out.closeEntry()
        }
    }

    kotlin.runCatching { ArchiveCommand.unregisterFormat("mzip") }
    ArchiveCommand.registerFormat("mzip", ZipArchiveFormat())

    try {
        val mem = ByteArrayOutputStream()
        //println(this.repository.resolve(rel))
        this.archive()
            .setTree(this.repository.resolve(rel))
            .also {
                if (path != "/") {
                    it.setPaths(path.trimStart('/'))
                }
            }
            .setFilename("archive.mzip")
            .setFormat("mzip")
            .setFormatOptions(mapOf("removePrefix" to "$path/"))
            .setOutputStream(mem)
            .call()
        return mem.toByteArray()
    } finally {
        ArchiveCommand.unregisterFormat("mzip")
    }
}

fun Git.checkRefExists(rel: String): Boolean {
    if (this.repository.findRef(rel) != null) return true
    return try {
        describe().setTarget(rel).call()
        true
    } catch (e: MissingObjectException) {
        false
    } catch (e: RefNotFoundException) {
        false
    }
}

fun getCachedGitCheckout(
    projectName: String,
    repo: String,
    projectPath: String,
    rel: String,
    subfolder: String = "",
    outputCheckoutDir: File
): File {
    val VERSION = 2
    val repo = ensureRepo(repo)
    val gitRepo = "https://github.com/$repo.git"
    val kprojectRoot = getKProjectDir()
    val gitFolder = File(kprojectRoot, "modules/$repo/__git__")

    if (outputCheckoutDir[".gitarchive"].takeIf { it.exists() }?.readText() == "$VERSION") return outputCheckoutDir

    val git = when {
        gitFolder.exists() -> Git.open(gitFolder)
        else -> {
            println("Cloning $gitRepo into $gitFolder...")
            Git.cloneRepository()
                .setProgressMonitor(TextProgressMonitor(PrintWriter(System.out)))
                .setURI(gitRepo)
                .setDirectory(gitFolder)
                //.setBare(true)
                .call()
        }
    }

    if (!git.checkRefExists(rel)) {
        git.pull()
            .setProgressMonitor(TextProgressMonitor(PrintWriter(System.out)))
            .call()
    }

    if (!git.checkRefExists(rel)) {
        error("Can't find '$rel' in git repo")
    }

    //val localZipFile = File.createTempFile("kproject", ".zip")
    val localZipFile = File(outputCheckoutDir.parentFile, outputCheckoutDir.name + ".zip")

    try {
        println("Getting archive for '$projectName':$rel at $gitRepo :: $projectPath...")

        localZipFile.writeBytes(git.archiveZip(path = projectPath, rel = rel))
        unzipTo(File(outputCheckoutDir, subfolder), localZipFile)
        outputCheckoutDir[".gitarchive"].writeText("$VERSION")
    } finally {
        localZipFile.delete()
    }

    return outputCheckoutDir
}

fun generateStableZipContent(zipBytes: ByteArray): ByteArray {
    val files = LinkedHashMap<String, ByteArray>()

    ZipInputStream(zipBytes.inputStream()).use { zis ->
        var zipEntry = zis.nextEntry
        while (zipEntry != null) {
            files[PathInfo(zipEntry.name).fullPath] = zis.readBytes()
            zis.closeEntry()
            zipEntry = zis.nextEntry
        }
    }

    val out = ByteArrayOutputStream(files.size * 1024 + files.values.sumOf { it.size })
    for (name in files.keys.toList().sorted()) {
        val bytes = files[name] ?: continue
        //println("FILE: '$name'")
        out.write("$name\n${bytes.size}\n".toByteArray(Charsets.UTF_8))
        out.write(bytes)
    }
    return out.toByteArray()
}

/*
class GIT(val vfs: java.io.File) {
    companion object {
        fun ensureGitRepo(repo: String): String = when {
            repo.contains("://") || repo.contains("git@") || repo.contains(":") -> repo
            else -> "https://github.com/${java.io.File(repo).normalize()}.git"
        }
    }

    fun downloadArchiveSubfolders(repo: String, path: String, rel: String, vfs: java.io.File = this.vfs) {
        vfs.mkdirs()
        if (vfs.list().isNullOrEmpty()) {
            println(vfs.execToString("git", "clone", "--branch", rel, "--depth", "1", "--filter=blob:none", "--sparse", ensureGitRepo(repo), "."))
            println(vfs.execToString("git", "sparse-checkout", "set", java.io.File(path).normalize().toString().removePrefix("/")))
        }
        vfs.delete()
    }
}
*/
