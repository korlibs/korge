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
    val folders: List<FileCollection> = when {
        compilation != null -> compilation.allKotlinSourceSets.map { it.resources.sourceDirectories }
        else -> listOf(project.files(file("src/commonMain/resources"), file("src/${targetNameRaw}${compilationName.capitalize()}/resources")))
    }

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

    /*
    tasks.createThis<Task>("listKorgePlugins") {
        group = GROUP_KORGE_LIST
        if (korge.searchResourceProcessorsInMainSourceSet) {
            dependsOn("jvmMainClasses")
        }
        doLast {
            //URLClassLoader(prepareResourceProcessingClasses.outputs.files.toList().map { it.toURL() }.toTypedArray(), ClassLoader.getSystemClassLoader()).use { classLoader ->

            executeInPlugin(
                korgeClassPath,
                "korlibs.korge.resources.ResourceProcessorRunner",
                "printPlugins"
            ) { listOf(it) }
        }
    }
    */

    afterEvaluate {
        //for (target in kotlin.targets) {
        //    for (compilation in target.compilations) {
        //        val taskName = getKorgeProcessResourcesTaskName(target.name, compilation.name)
        //        tasks.createThis<Task>(taskName) // dummy for now
        //    }
        //}


        for (task in tasks.withType(ProcessResources::class.java).toList()) {
            generateKorgeProcessedFromTask(task, task.name)
        }
    }

    //project.afterEvaluate {
    //    (tasks.getByName("processResources") as ProcessResources).apply {
    //        filesMatching("application.properties") {
    //            this.rootSpec.expand()
    //            expand(project.properties)
    //        }
    //    }
    //    println("project.processResources=" + project.extensions.getByName("processResources"))
    //}

    //project.afterEvaluate {
    //    val processedResources = (tasks.getByName("processResources") as ProcessResources)
    //}
    //println("[a]")
    /*
    val tasks = this.tasks
    for (task in tasks.withType(ProcessResources::class.java).toList()) {
        val taskName = task.name
        val targetNameRaw = taskName.removeSuffix("ProcessResources")
        val isTest = targetNameRaw.endsWith("Test")
        val targetName = targetNameRaw.removeSuffix("Test")
        val target = kotlin.targets.findByName(targetName) ?: continue
        val isJvm = targetName == "jvm"
        val compilationName = if (isTest) "test" else "main"
        val compilation = target.compilations[compilationName]
        //println("TASK.ProcessResources: $targetName, test=$isTest : target=$target, $this : ${this::class}")

        println("runJvm.korgeClassPath=${runJvm.korgeClassPath.toList()}")

        val korgeProcessedResources = tasks.createThis<KorgeProcessedResourcesTask>(
            getKorgeProcessResourcesTaskName(targetName, compilationName),
            KorgeProcessedResourcesTaskConfig(
                isJvm, targetName, compilationName, runJvm.korgeClassPath,
                project.korge.getIconBytes(),
            )
        ) {
            val task = this
            //if (!isJvm) task.dependsOn(getKorgeProcessResourcesTaskName("jvm", "main"))
            task.group = korlibs.korge.gradle.targets.GROUP_KORGE_RESOURCES
            task.processedResourcesFolder = getCompilationKorgeProcessedResourcesFolder(targetName, compilationName)
            task.folders = compilation.allKotlinSourceSets
                .flatMap { it.resources.srcDirs }
                .filter { it != processedResourcesFolder }
        }

        task.from(korgeProcessedResources.processedResourcesFolder)
        task.dependsOn(korgeProcessedResources)
    }

     */
    //println("[b]")

    /*
    for (target in kotlin.targets) {
        val isJvm = target.isJvm
        var previousCompilationKorgeProcessedResources: KorgeProcessedResourcesTask? = null
        for (compilation in target.compilations) {
            //val isJvm = compilation.compileKotlinTask.name == "compileKotlinJvm"
            val processedResourcesFolder = getCompilationKorgeProcessedResourcesFolder(compilation)
            compilation.defaultSourceSet.resources.srcDir(processedResourcesFolder)

            //val compilation = project.kotlin.targets.getByName(config.targetName).compilations.getByName(config.compilationName)
            val folders: List<String> =
                compilation.allKotlinSourceSets.flatMap { it.resources.srcDirs }
                    .filter { it != processedResourcesFolder }.map { it.toString() }

            val korgeProcessedResources = tasks.createThis<KorgeProcessedResourcesTask>(
                getKorgeProcessResourcesTaskName(target, compilation),
                KorgeProcessedResourcesTaskConfig(
                    isJvm, target.name, compilation.name, runJvm.korgeClassPath,
                    project.korge.getIconBytes(),
                )
            ) {
                val task = this
                //if (!isJvm) task.dependsOn(getKorgeProcessResourcesTaskName("jvm", "main"))
                task.group = GROUP_KORGE_RESOURCES
                if (korge.searchResourceProcessorsInMainSourceSet) {
                    task.dependsOn("jvmMainClasses")
                }
                task.outputs.dirs(processedResourcesFolder)
                task.folders = folders.map { File(it) }
                task.processedResourcesFolder = processedResourcesFolder
            }

            copyTasks.forEach {
                it?.dependsOn(korgeProcessedResources)
            }
            //previousCompilationKorgeProcessedResources?.dependsOn(korgeProcessedResources)

            if (isJvm) {
                compilation.compileKotlinTask.finalizedBy(korgeProcessedResources)
                tasks.getByName("runJvm").dependsOn(korgeProcessedResources)
            } else {
                compilation.compileKotlinTask.dependsOn(korgeProcessedResources)
            }

            previousCompilationKorgeProcessedResources = korgeProcessedResources
        }
    }

     */
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
            //val rfile = it.file.absolutePath
            //for (skippedFile in skippedFiles) {
            //    println("it.file=${it.file}, rpath=${it.relativePath}, rfile=$rfile, skippedFile=${skippedFile}")
            //    if (rfile.startsWith(skippedFile.absolutePath)) {
            //        println("!! EXCLUDED")
            //        return@exclude true
            //    }
            //}
            false
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
        //println("processFolder.skippedFiles=$skippedFiles")
    }
}

/*
data class KorgeProcessedResourcesTaskConfig(
    val isJvm: Boolean,
    val targetName: String,
    val compilationName: String,
    val korgeClassPath: FileCollection,
    val iconBytes: ByteArray,
)

open class KorgeProcessedResourcesTask @Inject constructor(
    private val config: KorgeProcessedResourcesTaskConfig,
    //private val fs: FileSystemOperations,
) : DefaultTask() {
    @get:OutputDirectory
    lateinit var processedResourcesFolder: File
    // https://docs.gradle.org/7.4/userguide/configuration_cache.html#config_cache:requirements:use_project_during_execution
    @get:InputFiles
    @get:Classpath
    lateinit var folders: List<File>

    @TaskAction
    fun run() {
        processedResourcesFolder.mkdirs()
        //URLClassLoader(prepareResourceProcessingClasses.outputs.files.toList().map { it.toURL() }.toTypedArray(), ClassLoader.getSystemClassLoader()).use { classLoader ->

        if (config.isJvm) {
            File(processedResourcesFolder, "@appicon.png").writeBytes(config.iconBytes)
            //processedResourcesFolder["@appicon-16.png"].writeBytes(korge.getIconBytes(16))
            //processedResourcesFolder["@appicon-32.png"].writeBytes(korge.getIconBytes(32))
            //processedResourcesFolder["@appicon-64.png"].writeBytes(korge.getIconBytes(64))
        }

        //println("config.korgeClassPath:\n${config.korgeClassPath.toList().joinToString("\n")}")

        executeInPlugin(
            config.korgeClassPath,
            "korlibs.korge.resources.ResourceProcessorRunner",
            "run"
        ) { classLoader ->
            listOf(
                classLoader,
                folders.map { it.toString() },
                processedResourcesFolder.toString(),
                config.compilationName
            )
        }
    }
}
*/
