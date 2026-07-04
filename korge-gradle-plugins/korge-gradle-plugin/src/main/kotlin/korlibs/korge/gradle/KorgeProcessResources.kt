package korlibs.korge.gradle

import java.io.File
import java.util.Locale
import java.util.Locale.getDefault
import javax.inject.Inject
import korlibs.korge.gradle.processor.KorgeResourceProcessor
import korlibs.korge.gradle.processor.KorgeResourceProcessorContext
import korlibs.korge.gradle.targets.GROUP_KORGE_LIST
import korlibs.korge.gradle.targets.jvm.getKorgeClassPath
import korlibs.korge.gradle.util.createThis
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

fun Project.getCompilationKorgeProcessedResourcesFolder(compilation: KotlinCompilation<*>): File =
    getCompilationKorgeProcessedResourcesFolder(compilation.target.name, compilation.name)

fun Project.getCompilationKorgeProcessedResourcesFolder(
    targetName: String,
    compilationName: String
): File = project.layout.buildDirectory
    .dir("korgeProcessedResources/${targetName}/${compilationName}")
    .get()
    .asFile

fun getKorgeProcessResourcesTaskName(targetName: String, compilationName: String): String =
    "korgeProcessedResources${
        targetName.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(
                getDefault()
            ) else it.toString()
        }
    }${compilationName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }}"

fun getProcessResourcesTaskName(targetName: String, compilationName: String): String =
    "${targetName.replaceFirstChar { it.lowercase(getDefault()) }}${
        if (compilationName == "main") "" else compilationName.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString()
        }
    }ProcessResources"

fun Project.generateKorgeProcessedFromTask(task: ProcessResources) {
    val targetNameRaw = task.name.removeSuffix("ProcessResources")
    val isTest = targetNameRaw.endsWith("Test")
    val targetName = targetNameRaw.removeSuffix("Test")
    val target = kotlin.targets.findByName(targetName) ?: return
    val compilationName = if (isTest) "test" else "main"
    val korgeGeneratedTaskName = getKorgeProcessResourcesTaskName(target.name, compilationName)
    val korgeGeneratedTask = tasks.createThis<KorgeGenerateResourcesTask>(korgeGeneratedTaskName)
    val korgeGeneratedFolder = getCompilationKorgeProcessedResourcesFolder(targetName, compilationName)
    val compilation = target.compilations.findByName(compilationName)
    val folders: MutableList<FileCollection> = when {
        compilation != null -> compilation.allKotlinSourceSets.map { it.resources.sourceDirectories }.toMutableList()
        else -> arrayListOf(project.files(
            file("resources"),
            file("src/commonMain/resources"),
            file("src/${targetNameRaw}${
                compilationName.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        getDefault()
                    ) else it.toString()
                }
            }/resources")
        ))
    }

    for (subproject in this.project.allDependantProjects()) {
        val files = files(
            file("resources"),
            subproject.file("src/commonMain/resources"),
            subproject.file("src/${targetNameRaw}${
                compilationName.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        getDefault()
                    ) else it.toString()
                }
            }/resources")
        )
        folders.add(files)
        task.from(files)
    }

    korgeGeneratedTask.korgeGeneratedFolder = korgeGeneratedFolder
    korgeGeneratedTask.inputFolders = folders
    korgeGeneratedTask.resourceProcessors = korge.resourceProcessors

    task.from(korgeGeneratedFolder)
    task.dependsOn(korgeGeneratedTask)
    korgeGeneratedTask.addToCopySpec(task)
}

fun Project.addGenResourcesTasks() {
    if (project.extensions.findByType(KotlinMultiplatformExtension::class.java) == null) return

    tasks.withType(Copy::class.java).configureEach {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    val korgeClassPath = project.getKorgeClassPath()

    tasks.createThis<Task>("listKorgeTargets") {
        group = GROUP_KORGE_LIST
        doLast {
            println("gkotlin.targets: ${gkotlin.targets.names}")
        }
    }

    tasks.createThis<Task>("listKorgePlugins") {
        group = GROUP_KORGE_LIST
        doLast {
            println("KorgePlugins:")
            for (item in (korge.resourceProcessors + KorgeResourceProcessor.getAll()).distinct()) {
                println("- $item")
            }
        }
    }

    afterEvaluate {
        for (task in tasks.withType(ProcessResources::class.java).toList()) {
            generateKorgeProcessedFromTask(task)
        }
    }
}

@DisableCachingByDefault
open class KorgeGenerateResourcesTask @Inject constructor() : DefaultTask() {
    @get:OutputDirectory
    lateinit var korgeGeneratedFolder: File

    @get:InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    lateinit var inputFolders: List<FileCollection>

    @Internal
    lateinit var resourceProcessors: List<KorgeResourceProcessor>

    @get:OutputDirectories
    var skippedFiles: Set<String> = setOf()

    fun addToCopySpec(copy: CopySpec, addFrom: Boolean = true) {
        addToCopySpec(this.korgeGeneratedFolder, this.skippedFiles, copy, addFrom)
    }

    companion object {
        fun addToCopySpec(korgeGeneratedFolder: File, skippedFiles: Set<String>, copy: CopySpec, addFrom: Boolean = true) {
            if (addFrom) copy.from(korgeGeneratedFolder)

            copy.exclude {
                val relativeFile = File(it.relativePath.toString())
                if (it.relativePath.startsWith('.')) return@exclude true
                for (skippedFile in skippedFiles) {
                    if (relativeFile.startsWith(skippedFile)) {
                        return@exclude true
                    }
                }
                false
            }
        }
    }

    @TaskAction
    fun run() {
        val resourcesFolders = inputFolders.flatMap { it.toList() }
        val resourcesSubfolders = resourcesFolders.flatMap { base -> base.walk().filter { it.isDirectory }.map { it.relativeTo(base) } }.distinct()

        for (folder in resourcesSubfolders) {
            val korgeGeneratedSubFolder = korgeGeneratedFolder.resolve(folder)
            korgeGeneratedSubFolder.mkdirs()
            processFolder(korgeGeneratedSubFolder, resourcesFolders.mapNotNull { it.resolve(folder).takeIf { it.isDirectory } })
        }
    }

    fun processFolder(generatedFolder: File, resourceFolders: List<File>) {
        val context = KorgeResourceProcessorContext(logger, generatedFolder, resourceFolders)
        try {
            for (processor in (resourceProcessors + KorgeResourceProcessor.getAll()).distinct()) {
                try {
                    processor.processFolder(context)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        skippedFiles += context.skippedFiles
    }
}
