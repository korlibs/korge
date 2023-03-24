package korlibs.korge.gradle

import korlibs.korge.gradle.targets.ios.*
import korlibs.korge.gradle.util.*
import java.io.*
import kotlin.test.*

class IosDeployTest : AbstractGradleIntegrationTest() {
    @Test
    @Ignore
    fun testInstall() = createTempDirectory { tempDir ->
        project.korgeCacheDir = tempDir
        val commandLog = arrayListOf<String>()
        project.defineExecResult("git", "clone", "https://github.com/korlibs/ios-deploy.git", project.korgeCacheDir["ios-deploy"].absolutePath, result = {
            commandLog += "clone"
            File(it.commandLine.last(), ".git").mkdirs()
            TestableExecResult("")
        })
        project.defineExecResult("xcodebuild", "-target", "ios-deploy") {
            commandLog += "xcodebuild"
            it.workingDir["build/Release"].also { it.mkdirs() }["ios-deploy"].writeText("")
            TestableExecResult("")
        }
        assertEquals(project.iosDeployExt.isInstalled, false)
        assertEquals("", commandLog.joinToString(", "))
        run {
            project.iosDeployExt.installIfRequired()
        }
        assertEquals(project.iosDeployExt.isInstalled, true)
        assertEquals("clone, xcodebuild", commandLog.joinToString(", "))
    }
}