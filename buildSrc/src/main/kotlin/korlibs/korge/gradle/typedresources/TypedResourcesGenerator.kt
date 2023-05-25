package korlibs.korge.gradle.typedresources

import korlibs.*
import korlibs.korge.gradle.*
import korlibs.korge.gradle.kotlin
import korlibs.korge.gradle.util.*
import korlibs.korge.gradle.util.ensureParents
import org.gradle.api.*
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import java.io.*

class TypedResourcesGenerator {
    companion object {
        val REGEX_NON_WORDS = Regex("\\W+")
    }
    fun String.normalizeName(): String = this.replace(REGEX_NON_WORDS, "_").trim('_')

    fun generateForFolders(resourcesFolder: SFile): String {
        return Indenter {
            line("import korlibs.audio.sound.Sound")
            line("import korlibs.audio.sound.readSound")
            line("import korlibs.image.atlas.Atlas")
            line("import korlibs.image.atlas.readAtlas")
            line("import korlibs.io.file.VfsFile")
            line("import korlibs.io.file.std.resourcesVfs")
            line("import korlibs.image.format.readBitmap")
            line("")
            line("// AUTO-GENERATED FILE! DO NOT MODIFY!")
            line("")
            line("inline class TypedVfsFile<T>(val file: VfsFile)")
            line("interface TypedAtlas<T>")

            data class AtlasInfo(val file: SFile, val className: String)

            val atlases = arrayListOf<AtlasInfo>()

            val exploredFolders = LinkedHashSet<SFile>()
            val foldersToExplore = ArrayDeque<SFile>()
            foldersToExplore += resourcesFolder

            while (foldersToExplore.isNotEmpty()) {
                val folder = foldersToExplore.removeFirst()
                if (folder in exploredFolders) continue
                exploredFolders += folder
                val files = folder.list()
                line("")
                line("object KR${folder.path.textCase().pascalCase()}") {
                    line("val __file get() = resourcesVfs[\"${folder.path}\"]")
                    for (file in files.sortedBy { it.name }.distinctBy { it.nameWithoutExtension.normalizeName().textCase().camelCase() }) {
                        if (file.path == "") continue
                        if (file.name.startsWith(".")) continue
                        val path = file.path
                        if (path.isEmpty()) continue
                        val varName = file.nameWithoutExtension.normalizeName().textCase().camelCase()
                        val fullVarName = file.path.normalizeName()
                        val extension = File(path).extension.lowercase()
                        //println("extension=$extension")
                        var extraSuffix = ""
                        val isDirectory = file.isDirectory()
                        val type: String? = when (extension) {
                            "png", "jpg" -> "korlibs.image.bitmap.Bitmap"
                            "mp3", "wav" -> "korlibs.audio.sound.Sound"
                            "atlas" -> {
                                if (isDirectory) {
                                    extraSuffix += ".json"
                                    val className = "Atlas${fullVarName.textCase().pascalCase()}"
                                    atlases += AtlasInfo(file, className)
                                    "TypedAtlas<$className>"
                                } else {
                                    "korlibs.io.file.VfsFile"
                                }
                            }
                            else -> {
                                if (isDirectory) {
                                    foldersToExplore += file
                                    null
                                } else {
                                    "korlibs.io.file.VfsFile"
                                }
                            }
                        }
                        val pathWithSuffix = "$path$extraSuffix"
                        when {
                            type != null -> line("val `$varName` get() = TypedVfsFile<$type>(resourcesVfs[\"$pathWithSuffix\"])")
                            else -> line("val `$varName` get() = KR${file.path.textCase().pascalCase()}")
                        }
                    }
                }
            }

            for (atlas in atlases) {
                line("")
                line("@kotlin.jvm.JvmName(\"read_TypedVfsFile_TypedAtlas_${atlas.className}\")")
                line("suspend fun TypedVfsFile<TypedAtlas<${atlas.className}>>.read() = ${atlas.className}(this.file.readAtlas())")
                line("inline class ${atlas.className}(val __atlas: korlibs.image.atlas.Atlas)") {
                    val atlasBaseDir = atlas.file
                    for (file in atlasBaseDir.list()) {
                        if (file.name.startsWith(".")) continue
                        if (file.isDirectory()) continue
                        val pathDir = file.name
                        if (pathDir.toString().isEmpty()) continue
                        line("val `${file.nameWithoutExtension.normalizeName()}` get() = __atlas[\"$pathDir\"]")
                    }
                }
            }
            line("")
            line("@kotlin.jvm.JvmName(\"read_TypedVfsFile_Bitmap\")")
            line("suspend fun TypedVfsFile<korlibs.image.bitmap.Bitmap>.read() = this.file.readBitmap()")
            line("@kotlin.jvm.JvmName(\"read_TypedVfsFile_Sound\")")
            line("suspend fun TypedVfsFile<korlibs.audio.sound.Sound>.read() = this.file.readSound()")

        }
    }
}

open class GenerateTypedResourcesTask : DefaultTask() {
    @get:OutputDirectory
    var krDir: File = project.krDir

    @get:InputDirectory
    var resourceFolders: FileCollection = project.resourceFileCollection

    @TaskAction
    fun run() {
        generateTypedResources(krDir, resourceFolders.toList())
    }
}

private val Project.krDir: File get() = File(project.buildDir, "KR")
private val Project.resourceFileCollection: FileCollection get() = project.files("src/commonMain/resources")

private fun generateTypedResources(krDir: File, resourcesFolders: List<File>) {
    val file = File(krDir, "KR.kt").ensureParents()
    // @TODO: Multiple resourcesFolders. Combine in a single File as a Merged File System for simplicity
    val generatedText = TypedResourcesGenerator().generateForFolders(LocalSFile(resourcesFolders.first()))
    if (!file.exists() || file.readText() != generatedText) {
        file.writeText(generatedText)
    }
    file.writeText(generatedText)
}

fun Project.configureTypedResourcesGenerator() {
    val generateTypedResources = tasks.createTyped<GenerateTypedResourcesTask>("generateTypedResources")
    afterEvaluate {
        if (project.korge.autoGenerateTypedResources) {
            tasks.getByName("idea").dependsOn(generateTypedResources)
            tasks.withType(KorgeGenerateResourcesTask::class.java).forEach {
                it.finalizedBy(generateTypedResources)
            }
            kotlin.metadata().compilations.main.defaultSourceSet.kotlin.srcDir(generateTypedResources.krDir)
            generateTypedResources(krDir, resourceFileCollection.toList())
        }
    }
}
