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
        val STARTS_WITH_NUMBER = Regex("^\\d")
        val REGEX_NON_WORDS = Regex("\\W+")
    }
    fun String.normalizeName(): String {
        val res = this.replace(REGEX_NON_WORDS, "_").trim('_')
        return if (STARTS_WITH_NUMBER.matchesAt(res, 0)) "n$res" else res
    }

    fun String.nameToVariable(): String {
        return normalizeName().textCase().camelCase()
    }

    fun generateForFolders(resourcesFolder: SFile, reporter: (e: Throwable, message: String) -> Unit = { e, message ->
        System.err.println(message)
        e.printStackTrace()
    }): String {
        return Indenter {
            line("import korlibs.audio.sound.*")
            line("import korlibs.io.file.*")
            line("import korlibs.io.file.std.*")
            line("import korlibs.image.bitmap.*")
            line("import korlibs.image.atlas.*")
            line("import korlibs.image.font.*")
            line("import korlibs.image.format.*")
            line("")
            line("// AUTO-GENERATED FILE! DO NOT MODIFY!")
            line("")
            line("@Retention(AnnotationRetention.BINARY) annotation class ResourceVfsPath(val path: String)")
            line("inline class TypedVfsFile(val __file: VfsFile)")
            line("inline class TypedVfsFileTTF(val __file: VfsFile) {")
            line("  suspend fun read(): korlibs.image.font.TtfFont = this.__file.readTtfFont()")
            line("}")
            line("inline class TypedVfsFileBitmap(val __file: VfsFile) {")
            line("  suspend fun read(): korlibs.image.bitmap.Bitmap = this.__file.readBitmap()")
            line("  suspend fun readSlice(atlas: MutableAtlasUnit? = null, name: String? = null): BmpSlice = this.__file.readBitmapSlice(name, atlas)")
            line("}")
            line("inline class TypedVfsFileSound(val __file: VfsFile) {")
            line("  suspend fun read(): korlibs.audio.sound.Sound = this.__file.readSound()")
            line("}")
            line("interface TypedAtlas<T>")

            data class ExtraInfo(val file: SFile, val className: String)

            val atlases = arrayListOf<ExtraInfo>()
            val ases = arrayListOf<ExtraInfo>()

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
                        line("val __file get() = resourcesVfs[${folder.path.quoted}]")
                        for (file in files.sortedBy { it.name }
                            .distinctBy { it.nameWithoutExtension.nameToVariable() }) {
                            if (file.path == "") continue
                            if (file.name.startsWith(".")) continue
                            val path = file.path
                            if (path.isEmpty()) continue
                            val varName = file.nameWithoutExtension.nameToVariable()
                            val fullVarName = file.path.normalizeName()
                            val extension = File(path).extension.lowercase()
                            //println("extension=$extension")
                            var extraSuffix = ""
                            val isDirectory = file.isDirectory()
                            val type: String? = when (extension) {
                                "png", "jpg" -> "TypedVfsFileBitmap"
                                "mp3", "wav" -> "TypedVfsFileSound"
                                "ttf", "otf" -> "TypedVfsFileTTF"
                                "ase" -> {
                                    val className = "Ase${fullVarName.textCase().pascalCase()}"
                                    ases += ExtraInfo(file, className)
                                    "$className.TypedAse"
                                }
                                "atlas" -> {
                                    if (isDirectory) {
                                        extraSuffix += ".json"
                                        val className = "Atlas${fullVarName.textCase().pascalCase()}"
                                        atlases += ExtraInfo(file, className)
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
                            val annotation = "@ResourceVfsPath(${pathWithSuffix.quoted})"
                            when {
                                type != null -> line("$annotation val `$varName` get() = $type(resourcesVfs[${pathWithSuffix.quoted}])")
                                else -> line("$annotation val `$varName` get() = __KR.KR${file.path.textCase().pascalCase()}")
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
                    for (file in atlasBaseDir.list().sortedBy { it.name }) {
                        if (file.name.startsWith(".")) continue
                        if (file.isDirectory()) continue
                        val pathDir = file.name
                        if (pathDir.toString().isEmpty()) continue
                        line("@ResourceVfsPath(${file.path.quoted}) val `${file.nameWithoutExtension.normalizeName()}` get() = __atlas[${pathDir.quoted}]")
                    }
                }
            }

            for (ase in ases) {
                val aseFile = ase.file

                val info = try {
                    ASEInfo.getAseInfo(ase.file.readBytes())
                } catch (e: Throwable) {
                    reporter(e, "ERROR LOADING FILE: aseFile=$aseFile")
                    ASEInfo()
                }

                line("")
                line("inline class ${ase.className}(val data: korlibs.image.format.ImageDataContainer)") {
                    line("inline class TypedAse(val __file: VfsFile) { suspend fun read(atlas: korlibs.image.atlas.MutableAtlasUnit? = null): ${ase.className} = ${ase.className}(this.__file.readImageDataContainer(korlibs.image.format.ASE.toProps(), atlas)) }")

                    line("enum class TypedAnimation(val animationName: String)") {
                        for (tag in info.tags) {
                            line("${tag.tagName.nameToVariable().uppercase()}(${tag.tagName.quoted}),")
                        }
                        line(";")
                        line("companion object") {
                            line("val list: List<TypedAnimation> = values().toList()")
                            for (tag in info.tags) {
                                line("val ${tag.tagName.nameToVariable().lowercase()}: TypedAnimation get() = TypedAnimation.${tag.tagName.nameToVariable().uppercase()}")
                            }
                        }
                    }

                    line("inline class TypedImageData(val data: ImageData)") {
                        line("val animations: TypedAnimation.Companion get() = TypedAnimation")
                    }

                    val uniqueNames = UniqueNameGenerator()
                    uniqueNames["animations"] // reserve names
                    uniqueNames["default"] // reserve names

                    line("val animations: TypedAnimation.Companion get() = TypedAnimation")
                    line("val default: TypedImageData get() = TypedImageData(data.default)")
                    for (sliceName in info.slices.map { it.sliceName }.distinct()) {
                        val varName = uniqueNames[sliceName.nameToVariable()]
                        line("val `$varName`: TypedImageData get() = TypedImageData(data[${sliceName.quoted}]!!)")
                    }
                    // @TODO: We could

                    //println("wizardFemale=${wizardFemale.imageDatasByName.keys}")
                    //println("wizardFemale.animations=${wizardFemale.imageDatas.first().animationsByName.keys}")
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
private val Project.resourceFileCollection: FileCollection get() = project.files(
    "resources",
    "src/commonMain/resources",
)

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
