package korlibs.korge.gradle

import java.io.ByteArrayOutputStream
import java.io.File
import korlibs.korge.gradle.targets.android.*
import korlibs.korge.gradle.util.SpawnExtension
import korlibs.korge.gradle.util.commandLineCompat
import korlibs.korge.gradle.util.execThis
import korlibs.korge.gradle.util.spawnExt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AndroidTest : AbstractGradleIntegrationTest() {
    val ANDROID_SDK_PATH = "/fake/android/sdk/path"
    val ANDROID_EMULATOR_PATH = "$ANDROID_SDK_PATH/emulator/emulator"
    val ANDROID_ADB_PATH = "$ANDROID_SDK_PATH/platform-tools/adb"
    val spawnResult = arrayListOf<Any>()

    init {
        project.extensions.add(korlibs.korge.gradle.targets.android.AndroidSdk.ANDROID_SDK_PATH_KEY, ANDROID_SDK_PATH)
        project.spawnExt = object : SpawnExtension() {
            override fun spawn(dir: File, command: List<String>) {
                spawnResult.add(dir to command)
            }

            override fun execLogger(projectDir: File, vararg params: String, filter: Process.(line: String) -> String?) {
                project.exec {
                    workingDir(projectDir)
                    commandLine(*params)
                }
            }

            override fun execOutput(projectDir: File, vararg params: String): String {
                val stdout = ByteArrayOutputStream()
                project.exec {
                    commandLineCompat(*params)
                    standardOutput = stdout
                }
                return stdout.toString("UTF-8")
            }
        }
    }
    val androidSdkProvider get() = project.androidSdkProvider

    @Test
    fun testPaths() {
        assertEquals(ANDROID_EMULATOR_PATH, androidSdkProvider.androidEmulatorPath)
        assertEquals(ANDROID_ADB_PATH, androidSdkProvider.androidAdbPath)
    }

    val emulatorListAvds = arrayOf(ANDROID_EMULATOR_PATH, "-list-avds")

    @Test
    fun testAndroidEmulatorListAvds() {
        project.defineExecResult(*emulatorListAvds, stdout = "Pixel_2_XL_API_28\n").also {
            assertEquals(listOf("Pixel_2_XL_API_28"), androidSdkProvider.androidEmulatorListAvds())
        }
        project.defineExecResult(*emulatorListAvds, stdout = "Android_TV_720p_API_28\nPixel_2_API_30\n").also {
            assertEquals(listOf("Android_TV_720p_API_28", "Pixel_2_API_30"), androidSdkProvider.androidEmulatorListAvds())
        }
    }

    @Test
    fun testAndroidEmulatorFirstAvd() {
        project.defineExecResult(*emulatorListAvds, stdout = "").also {
            assertEquals(null, androidSdkProvider.androidEmulatorFirstAvd())
        }
        project.defineExecResult(*emulatorListAvds, stdout = "Pixel_2_XL_API_28\n").also {
            assertEquals("Pixel_2_XL_API_28", androidSdkProvider.androidEmulatorFirstAvd())
        }
        project.defineExecResult(*emulatorListAvds, stdout = "Android_TV_720p_API_28\nPixel_2_API_30\n").also {
            assertEquals("Pixel_2_API_30", androidSdkProvider.androidEmulatorFirstAvd())
        }
    }

    @Test
    fun testAndroidEmulatorStart() {
        assertFailsWith<IllegalStateException> {
            project.defineExecResult(*emulatorListAvds, stdout = "")
            androidSdkProvider.androidEmulatorStart()
        }

        run {
            val DEVICE_NAME = "Pixel_2_XL_API_28"
            project.defineExecResult(*emulatorListAvds, stdout = "$DEVICE_NAME\n")
            project.defineExecResult(
                ANDROID_ADB_PATH, "devices", "-l", result = listOf(
                    //TestableExecResult(""),
                    TestableExecResult(
                        "List of devices attached\n" +
                            "emulator-5554          device product:sdk_gphone_x86 model:sdk_gphone_x86 device:generic_x86_arm transport_id:1"
                    ),
                )
            )
            androidSdkProvider.androidEmulatorStart()
            assertEquals<List<Any>>(
                listOf(
                    project.projectDir to listOf(
                        ANDROID_EMULATOR_PATH, "-avd", DEVICE_NAME, "-netdelay", "none", "-netspeed", "full"
                    )
                ), spawnResult
            )
        }
    }
}
