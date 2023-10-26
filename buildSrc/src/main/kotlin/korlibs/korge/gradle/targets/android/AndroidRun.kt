package korlibs.korge.gradle.targets.android

import korlibs.korge.gradle.*
import korlibs.korge.gradle.targets.*
import korlibs.korge.gradle.util.*
import korlibs.korge.gradle.util.AnsiEscape.Companion.color
import org.gradle.api.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.dsl.*
import java.io.*

fun Project.installAndroidRun(dependsOnList: List<String>, direct: Boolean, isKorge: Boolean) {
    if (!AndroidSdk.hasAndroidSdk(this)) {
        logger.info("Not configuring android because couldn't find the SDK")
        return
    }

    val createAndroidManifest = tasks.createThis<AndroidCreateAndroidManifest>("createAndroidManifest") {
        this.isKorge = isKorge
    }

    val hasKotlinMultiplatformExtension = project.extensions.findByType(KotlinMultiplatformExtension::class.java) != null
    if (hasKotlinMultiplatformExtension) {
        afterEvaluate {
            //generateKorgeProcessedFromTask(null, "androidProcessResources")
        }
    }

    //val generateAndroidProcessedResources = getKorgeProcessResourcesTaskName("jvm", "main")
    val generateAndroidProcessedResources = getProcessResourcesTaskName("jvm", "main")

    afterEvaluate {
        for (Type in listOf("Debug", "Release")) {
            //println("tasks.findByName(\"generate${Type}Assets\")=${tasks.findByName("generate${Type}Assets")}")
            //println("tasks.findByName(\"package${Type}\")=${tasks.findByName("package${Type}")}")
            if (hasKotlinMultiplatformExtension) {
                tasks.findByName("generate${Type}Assets")?.dependsOn(generateAndroidProcessedResources)
                tasks.findByName("package${Type}")?.dependsOn(generateAndroidProcessedResources)
            }

            tasks.findByName("generate${Type}BuildConfig")?.dependsOn(createAndroidManifest)
            tasks.findByName("process${Type}MainManifest")?.dependsOn(createAndroidManifest)


            // Not required anymore
            //(tasks.getByName("install${Type}") as InstallVariantTask).apply { installOptions = listOf("-r") }
            //tasks.getByName("install${Type}").dependsOn("createAndroidManifest")
        }
    }

    // adb shell am start -n com.package.name/com.package.name.ActivityName
    for (debug in listOf(false, true)) {
        val suffixDebug = if (debug) "Debug" else "Release"

        for (emulator in listOf(null, false, true)) {
            val suffixDevice = when (emulator) {
                null -> ""
                false -> "Device"
                true -> "Emulator"
            }

            val extra = when (emulator) {
                null -> arrayOf()
                false -> arrayOf("-d")
                true -> arrayOf("-e")
            }

            val installAndroidTaskName = "installAndroid$suffixDevice$suffixDebug"
            val installAndroidTask = when {
                direct -> tasks.createThis<Task>(installAndroidTaskName) {
                    //task.dependsOn("install$suffixDevice$suffixDebug")
                    dependsOn("install$suffixDebug")
                }

                else -> tasks.createThis<GradleBuild>(installAndroidTaskName) {
                    buildFile = File(buildDir, "platforms/android/build.gradle")
                    //task.version = "4.10.1"
                    //task.tasks = listOf("install$suffixDevice$suffixDebug")
                    tasks = listOf("install$suffixDebug")
                }
            }

            if (emulator == true) {
                installAndroidTask.dependsOn("androidEmulatorStart")
            }
            for (dependsOnTaskName in dependsOnList) {
                installAndroidTask.dependsOn(dependsOnTaskName)
            }
            installAndroidTask.group = GROUP_KORGE_INSTALL

            //installAndroidTask.dependsOn(getKorgeProcessResourcesTaskName("jvm", "main"))
            //installAndroidTask.dependsOn(getKorgeProcessResourcesTaskName("metadata", "main"))

            val androidApplicationId = AndroidGenerated.getAppId(project, isKorge)

            val onlyRunAndroid = tasks.createTyped<OnlyRunAndroidTask>("onlyRunAndroid$suffixDevice$suffixDebug") {
                this.extra = extra
            }

            afterEvaluate {
                onlyRunAndroid.androidApplicationId = AndroidGenerated.getAppId(project, isKorge)
            }

            tasks.createTyped<DefaultTask>("runAndroid$suffixDevice$suffixDebug") {
                group = GROUP_KORGE_RUN
                dependsOn(ordered("createAndroidManifest", installAndroidTaskName))
                finalizedBy(onlyRunAndroid)
            }
        }
    }

    tasks.createTyped<AndroidEmulatorListAvdsTask>("androidEmulatorDeviceList") {
        group = GROUP_KORGE_ADB
    }

    tasks.createTyped<AndroidEmulatorStartTask>("androidEmulatorStart") {
        group = GROUP_KORGE_ADB
        onlyIf { !androidEmulatorIsStarted() }
    }

    tasks.createTyped<AndroidAdbDeviceListTask>("adbDeviceList") {
        group = GROUP_KORGE_ADB
    }

    tasks.createTyped<AndroidAdbLogcatTask>("adbLogcat") {
        group = GROUP_KORGE_ADB
    }
}

open class AndroidCreateAndroidManifest : DefaultTask() {
    private lateinit var generated: AndroidGenerated

    @get:Input
    var isKorge = true

    init {
        project.afterEvaluate {
            generated = project.toAndroidGenerated(isKorge)
        }
    }
    @TaskAction
    fun run() {
        //println("this.generated=${this.generated} : isKorge=$isKorge")
        val mainDir = this.generated.getAndroidManifestFile(isKorge).parentFile
        generated.writeResources(this.generated.getAndroidResFolder(isKorge))
        generated.writeMainActivity(this.generated.getAndroidSrcFolder(isKorge))
        generated.writeKeystore(mainDir)
        generated.writeAndroidManifest(mainDir)
    }
}

open class OnlyRunAndroidTask : DefaultAndroidTask() {
    @get:Input
    var extra: Array<String> = emptyArray()
    @get:Input
    var androidApplicationId: String = ""

    override fun run() {
        execLogger(androidAdbPath, *extra, "shell", "am", "start",
            "-e", "sleepBeforeStart", "300",
            "-n", "$androidApplicationId/$androidApplicationId.MainActivity"
        )
        val pid: String = run {
            val startTime = System.currentTimeMillis()
            while (true) {
                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - startTime
                try {
                    val res = execOutput(androidAdbPath, *extra, "shell", "pidof", androidApplicationId).trim()
                    if (res.isEmpty()) error("PID not found")
                    return@run res
                } catch (e: Throwable) {
                    //e.printStackTrace()
                    Thread.sleep(10L)
                    if (elapsedTime >= 5000L) throw e
                }
            }
            error("Unexpected")
        }
        val EXIT_MESSAGE = "InputTransport: Input channel destroyed:"
        execLogger(androidAdbPath, *extra, "logcat", "--pid=$pid") {
            if (it.contains(EXIT_MESSAGE)) {
                println("Found EXIT_MESSAGE=$EXIT_MESSAGE")
                this.destroy()
            }
            val parts = it.split(" ", limit = 5)
            val end = parts.getOrElse(4) { " " }.trimStart()
            val color = when {
                end.startsWith('V') -> null
                end.startsWith('D') -> AnsiEscape.Color.BLUE
                end.startsWith('I') -> AnsiEscape.Color.GREEN
                end.startsWith('W') -> AnsiEscape.Color.YELLOW
                end.startsWith('E') -> AnsiEscape.Color.RED
                else -> AnsiEscape.Color.WHITE
            }
            //println("parts=$parts")
            if (color != null) it.color(color) else it
        }
    }
}

open class AndroidEmulatorListAvdsTask : DefaultAndroidTask() {
    override fun run() {
        androidEmulatorListAvds().joinToString("\n")
    }
}

open class AndroidAdbDeviceListTask : DefaultAndroidTask() {
    override fun run() {
        println(androidAdbDeviceList().joinToString("\n"))
        //execAndroidAdb("devices", "-l")
    }
}

open class AndroidAdbLogcatTask : DefaultAndroidTask() {
    override fun run() {
        execAndroidAdb("logcat")
    }
}

open class AndroidEmulatorStartTask : DefaultAndroidTask() {
    override fun run() {
        val avdName = androidEmulatorFirstAvd() ?: error("No android emulators available to start. Please create one using Android Studio")
        val spawner = spawnExt
        spawner.spawn(projectDir, listOf(androidEmulatorPath, "-avd", avdName, "-netdelay", "none", "-netspeed", "full"))
        while (!androidEmulatorIsStarted()) {
            Thread.sleep(1000L)
        }
    }
}


abstract class DefaultAndroidTask : DefaultTask(), AndroidSdkProvider {
    @TaskAction
    abstract fun run()

    //@get:InputDirectory
    @Internal
    override lateinit var projectDir: File
    //@get:Input
    @Internal
    override lateinit var androidSdkPath: String
    //@get:Input
    @Internal
    override lateinit var spawnExt: SpawnExtension

    fun initWithProject(project: Project) {
        this.projectDir = project.projectDir
        this.androidSdkPath = project.androidSdkPath
        this.spawnExt = project.spawnExt
    }

    init {
        project.afterEvaluate {
            initWithProject(it)
        }
    }
}

/*
tasks.createThis<Task>("onlyRunAndroid") {
    doFirst {
        val adb = "${AndroidSdk.guessAndroidSdkPath()}/platform-tools/adb"
        execThis {
            commandLine(adb, "shell", "am", "start", "-n", "${androidApplicationId}/${androidApplicationId}.MainActivity")
        }

        var pid = ""
        for (n in 0 until 10) {
            try {
                pid = execOutput(adb, "shell", "pidof", androidApplicationId)
                break
            } catch (e: Throwable) {
                Thread.sleep(500L)
                if (n == 9) throw e
            }
        }
        println(pid)
        execThis {
            commandLine(adb, "logcat", "--pid=${pid.trim()}")
        }
    }
}

afterEvaluate {
    tasks.createThis<Task>("runAndroidDebug") {
        dependsOn(ordered("createAndroidManifest", "installDebug"))
        finalizedBy("onlyRunAndroid")
    }

    tasks.createThis<Task>("runAndroidRelease") {
        dependsOn(ordered("createAndroidManifest", "installRelease"))
        finalizedBy("onlyRunAndroid")
    }
}

 */
