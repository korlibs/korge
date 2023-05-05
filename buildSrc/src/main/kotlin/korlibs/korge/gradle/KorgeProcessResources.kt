package korlibs.korge.gradle

import korlibs.korge.gradle.processor.*
import korlibs.korge.gradle.targets.*
import korlibs.korge.gradle.targets.jvm.*
import korlibs.korge.gradle.util.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.language.jvm.tasks.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import java.io.*
import javax.inject.*

fun Project.getCompilationKorgeProcessedResourcesFolder(compilation: KotlinCompilation<*>): File =
    getCompilationKorgeProcessedResourcesFolder(compilation.target.name, compilation.name)

fun Project.getCompilationKorgeProcessedResourcesFolder(
    targetName: String,
    compilationName: String
): File = File(project.buildDir, "korgeProcessedResources/${targetName}/${compilationName}")

fun getKorgeProcessResourcesTaskName(targetName: String, compilationName: String): String =
    "korgeProcessedResources${targetName.capitalize()}${compilationName.capitalize()}"

fun getProcessResourcesTaskName(targetName: String, compilationName: String): String =
    "${targetName.decapitalize()}${if (compilationName == "main") "" else compilationName.capitalize()}ProcessResources"

fun Project.generateKorgeProcessedFromTask(task: ProcessResources?, taskName: String) {
    val targetNameRaw = taskName.removeSuffix("ProcessResources")
    val isTest = targetNameRaw.endsWith("Test")
    val targetName = targetNameRaw.removeSuffix("Test")
    val target = kotlin.targets.findByName(targetName) ?: return
    val isJvm = targetName == "jvm"
    val compilationName = if (isTest) "test" else "main"
    val korgeGeneratedTaskName = getKorgeProcessResourcesTaskName(target.name, compilationName)
    val korgeGeneratedTask = tasks.createThis<KorgeGenerateResourcesTask>(korgeGeneratedTaskName)
    val korgeGeneratedFolder = getCompilationKorgeProcessedResourcesFolder(targetName, compilationName)
    val compilation = target.compilations.findByName(compilationName)
    val folders: MutableList<FileCollection> = when {
        compilation != null -> compilation.allKotlinSourceSets.map { it.resources.sourceDirectories }.toMutableList()
        else -> arrayListOf(project.files(file("src/commonMain/resources"), file("src/${targetNameRaw}${compilationName.capitalize()}/resources")))
    }

    //println("PROJECT: $project : ${this.project.allDependantProjects()}")

    for (subproject in this.project.allDependantProjects()) {
        val files = files(
            subproject.file("src/commonMain/resources"),
            subproject.file("src/${targetNameRaw}${compilationName.capitalize()}/resources")
        )
        //println("ADD : $subproject : ${subproject.file("src/commonMain/resources")}")
        folders.add(files)
        task?.from(files)
    }

    //println("" + project + " :: " + this.project.allDependantProjects())
    //println("project.configurations=${project.configurations["compile"].toList()}")
    //println("$project -> dependantProjects=$dependantProjects")

    korgeGeneratedTask.korgeGeneratedFolder = korgeGeneratedFolder
    korgeGeneratedTask.inputFolders = folders
    korgeGeneratedTask.resourceProcessors = korge.resourceProcessors

    if (task != null) {
        task.from(korgeGeneratedFolder)
        task.dependsOn(korgeGeneratedTask)
        korgeGeneratedTask.addToCopySpec(task)
    }
}

fun Project.addGenResourcesTasks() {
    if (project.extensions.findByType(KotlinMultiplatformExtension::class.java) == null) return

    val copyTasks = tasks.withType(Copy::class.java)
    copyTasks.configureEach {
        //it.duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.WARN
        it.duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
        //println("Task $this")
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
            //URLClassLoader(prepareResourceProcessingClasses.outputs.files.toList().map { it.toURL() }.toTypedArray(), ClassLoader.getSystemClassLoader()).use { classLoader ->
            println("KorgePlugins:")
            for (item in (korge.resourceProcessors + KorgeResourceProcessor.getAll()).distinct()) {
                println("- $item")
            }
        }
    }

    afterEvaluate {
        //for (target in kotlin.targets) {
        //    for (compilation in target.compilations) {
        //        val taskName = getKorgeProcessResourcesTaskName(target.name, compilation.name)
        //        tasks.createThis<Task>(taskName) // dummy for now
        //    }
        //}


        for (task in tasks.withType(ProcessResources::class.java).toList()) {
            //println("TASK: $task : ${task::class}")
            generateKorgeProcessedFromTask(task, task.name)
        }
    }
}

open class KorgeGenerateResourcesTask @Inject constructor(
    //private val fs: FileSystemOperations,
) : DefaultTask() {
    @get:OutputDirectory
    lateinit var korgeGeneratedFolder: File

    @get:InputFiles
    lateinit var inputFolders: List<FileCollection>

    //@get:Input
    @Internal
    lateinit var resourceProcessors: List<KorgeResourceProcessor>

    @get:OutputDirectories
    var skippedFiles: Set<String> = setOf()

    fun addToCopySpec(copy: CopySpec, addFrom: Boolean = true) {
        if (addFrom) copy.from(korgeGeneratedFolder)

        copy.exclude {
            val relativeFile = File(it.relativePath.toString())
            if (it.relativePath.startsWith('.')) return@exclude true
            for (skippedFile in skippedFiles) {
                //println("addExcludeToCopyTask: relativeFile=$relativeFile, skippedFile=$skippedFile")
                if (relativeFile.startsWith(skippedFile)) {
                    //println("!! EXCLUDED")
                    return@exclude true
                }
            }
            false
        }
    }

    @TaskAction
    fun run() {
        val resourcesFolders = inputFolders.flatMap { it.toList() }
        println("resourcesFolders:\n${resourcesFolders.joinToString("\n")}")
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
        //println("processFolder.skippedFiles=$skippedFiles")
    }
}

