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
            line("import korlibs.image.atlas.Atlas")
            line("import korlibs.io.file.VfsFile")
            line("import korlibs.io.file.std.resourcesVfs")
            line("import korlibs.image.atlas.readAtlas")
            line("import korlibs.audio.sound.readSound")
            line("import korlibs.image.format.readBitmap")
            line("")
            line("// AUTO-GENERATED FILE! DO NOT MODIFY!")
            line("")
            line("inline class TypedVfsFile(val __file: VfsFile)")
            line("inline class TypedVfsFileBitmap(val __file: VfsFile) { suspend fun read(): korlibs.image.bitmap.Bitmap = this.__file.readBitmap() }")
            line("inline class TypedVfsFileSound(val __file: VfsFile) { suspend fun read(): korlibs.audio.sound.Sound = this.__file.readSound() }")
            line("interface TypedAtlas<T>")

            data class AtlasInfo(val file: SFile, val className: String)

            val atlases = arrayListOf<AtlasInfo>()

            val exploredFolders = LinkedHashSet<SFile>()
            val foldersToExplore = ArrayDeque<SFile>()
            foldersToExplore += resourcesFolder

            line("")
            line("object KR : __KR.KR")
            line("")

            line("object __KR") {
                while (foldersToExplore.isNotEmpty()) {
                    val folder = foldersToExplore.removeFirst()
                    if (folder in exploredFolders) continue
                    exploredFolders += folder
                    val files = folder.list()
                    line("")
                    val classSuffix = folder.path.textCase().pascalCase()
                    line("${if (classSuffix.isEmpty()) "interface" else "object"} KR$classSuffix") {
                        line("val __file get() = resourcesVfs[\"${folder.path}\"]")
                        for (file in files.sortedBy { it.name }
                            .distinctBy { it.nameWithoutExtension.normalizeName().textCase().camelCase() }) {
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
                                "png", "jpg" -> "TypedVfsFileBitmap"
                                "mp3", "wav" -> "TypedVfsFileSound"
                                "atlas" -> {
                                    if (isDirectory) {
                                        extraSuffix += ".json"
                                        val className = "Atlas${fullVarName.textCase().pascalCase()}"
                                        atlases += AtlasInfo(file, className)
                                        "$className.TypedAtlas"
                                    } else {
                                        "TypedVfsFile"
                                    }
                                }

                                else -> {
                                    if (isDirectory) {
                                        foldersToExplore += file
                                        null
                                    } else {
                                        "TypedVfsFile"
                                    }
                                }
                            }
                            val pathWithSuffix = "$path$extraSuffix"
                            when {
                                type != null -> line("val `$varName` get() = $type(resourcesVfs[\"$pathWithSuffix\"])")
                                else -> line("val `$varName` get() = __KR.KR${file.path.textCase().pascalCase()}")
                            }
                        }
                    }
                }
            }

            for (atlas in atlases) {
                line("")
                line("inline class ${atlas.className}(val __atlas: korlibs.image.atlas.Atlas)") {
                    line("inline class TypedAtlas(val __file: VfsFile) { suspend fun read(): ${atlas.className} = ${atlas.className}(this.__file.readAtlas()) }")
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
