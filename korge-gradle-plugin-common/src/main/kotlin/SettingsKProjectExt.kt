import com.soywiz.kproject.model.*
import org.gradle.api.initialization.*
import java.io.*

/**
 * Example: "git::korge-dragonbones:korlibs/korge::/korge-dragonbones::v3.2.0"
 */

val Settings.kprojectCache: LinkedHashMap<String, Any?> get() {
    val extra = extensions.extraProperties
    val key = "kprojectCache"
    if (!extra.has(key)) {
        extra.set(key, LinkedHashMap<String, Any?>())
    }
    return extra.get(key) as LinkedHashMap<String, Any?>
}

fun Settings.kproject(path: String) {
    val settings = this
    val file1 = File(rootDir, "$path.kproject.yml")
    val file2 = File(rootDir, "$path/kproject.yml")
    val file = listOf(file1, file2).firstOrNull { it.exists() } ?: error("Can't find kproject.yml at path $path")
    if (file.absolutePath in settings.kprojectCache) {
        //println("Already processed '$file'")
        return
    }
    settings.kprojectCache[file.absolutePath] = true

    val results = NewKProjectGradleGenerator(LocalFileRef(rootDir))
        .generate(file.relativeTo(rootDir).path)
    for (result in results) {
        val rname = result.projectName
        val sourceDirectory = (result.projectDir as LocalFileRef).file
        //println(":$rname -> $sourceDirectory")
        settings.include(":${rname}")
        settings.project(":${rname}").projectDir = sourceDirectory.relativeTo(rootDir)
    }
}
