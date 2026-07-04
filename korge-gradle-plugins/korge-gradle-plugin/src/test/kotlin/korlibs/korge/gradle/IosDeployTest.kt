package korlibs.korge.gradle

import java.io.File
import korlibs.korge.gradle.targets.ios.iosTvosDeployExt
import korlibs.korge.gradle.util.get
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

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
        assertEquals(false, project.iosTvosDeployExt.isInstalled)
        assertEquals("", commandLog.joinToString(", "))
        run {
            project.iosTvosDeployExt.installIfRequired()
        }
        assertEquals(true, project.iosTvosDeployExt.isInstalled)
        assertEquals("clone, xcodebuild", commandLog.joinToString(", "))
    }
}
