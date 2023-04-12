package korlibs.korge.gradle.targets.android

import korlibs.korge.gradle.targets.*
import korlibs.korge.gradle.util.*
import org.gradle.api.*
import org.gradle.api.tasks.*
import java.io.*

fun Project.installAndroidRun(dependsOnList: List<String>, direct: Boolean, isKorge: Boolean) {

    val createAndroidManifest = tasks.createThis<Task>("createAndroidManifest") {
        doFirst {
            val generated = AndroidGenerated(project, isKorge)
            val mainDir = AndroidConfig.getAndroidManifestFile(project, isKorge).parentFile
            generated.writeResources(AndroidConfig.getAndroidResFolder(project, isKorge))
            generated.writeMainActivity(AndroidConfig.getAndroidSrcFolder(project, isKorge))
            generated.writeKeystore(mainDir)
            generated.writeAndroidManifest(mainDir)
        }
    }

    afterEvaluate {
        for (Type in listOf("Debug", "Release")) {
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

            val androidApplicationId = AndroidConfig.getAppId(project, isKorge)

            val onlyRunAndroid = tasks.createTyped<DefaultTask>("onlyRunAndroid$suffixDevice$suffixDebug") {
                doFirst {
                    execLogger {
                        it.commandLine(
                            androidAdbPath, *extra, "shell", "am", "start",
                            "-e", "sleepBeforeStart", "300",
                            "-n", "$androidApplicationId/$androidApplicationId.MainActivity"
                        )
                    }
                    val pid = run {
                        val startTime = System.currentTimeMillis()
                        while (true) {
                            val currentTime = System.currentTimeMillis()
                            val elapsedTime = currentTime - startTime
                            try {
                                return@run execOutput(androidAdbPath, *extra, "shell", "pidof", androidApplicationId).trim()
                            } catch (e: Throwable) {
                                //e.printStackTrace()
                                Thread.sleep(10L)
                                if (elapsedTime >= 5000L) throw e
                            }
                        }
                    }
                    execLogger {
                        it.commandLine(androidAdbPath, *extra, "logcat", "--pid=$pid")
                    }
                }
            }

            tasks.createTyped<DefaultTask>("runAndroid$suffixDevice$suffixDebug") {
                group = GROUP_KORGE_RUN
                dependsOn(ordered("createAndroidManifest", installAndroidTaskName))
                finalizedBy(onlyRunAndroid)
            }
        }
    }

    tasks.createTyped<DefaultTask>("androidEmulatorDeviceList") {
        group = GROUP_KORGE_ADB
        doFirst {
            println(androidEmulatorListAvds().joinToString("\n"))
            //execAndroidAdb("devices", "-l")
        }
    }

    tasks.createTyped<DefaultTask>("androidEmulatorStart") {
        group = GROUP_KORGE_ADB
        onlyIf { !androidEmulatorIsStarted() }
        doFirst {
            androidEmulatorStart()
        }
    }

    tasks.createTyped<DefaultTask>("adbDeviceList") {
        group = GROUP_KORGE_ADB
        doFirst {
            println(androidAdbDeviceList().joinToString("\n"))
            //execAndroidAdb("devices", "-l")
        }
    }

    tasks.createTyped<DefaultTask>("adbLogcat") {
        group = GROUP_KORGE_ADB
        doFirst {
            execAndroidAdb("logcat")
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
