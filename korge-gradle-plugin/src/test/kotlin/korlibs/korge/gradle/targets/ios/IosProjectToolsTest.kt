package korlibs.korge.gradle.targets.ios

import java.nio.file.Files
import korlibs.korge.gradle.util.get
import kotlin.test.Test
import kotlin.test.assertTrue

class IosProjectToolsTest {
    @Test
    fun testMainUsesSceneLifecycle() {
        val main = IosProjectTools.genMainObjC()
        assertTrue(main.contains("@interface SceneDelegate : UIResponder <UIWindowSceneDelegate>"))
        assertTrue(main.contains("config.delegateClass = [SceneDelegate class];"))
        assertTrue(main.contains("- (void)scene:(UIScene *)scene willConnectToSession:"))
        assertTrue(main.contains("applicationDidFinishLaunchingApp:"))
        assertTrue(main.contains("static BOOL korgeStarted = NO;"))
    }

    @Test
    fun testInfoPlistHasSceneManifest() {
        val folder = Files.createTempDirectory("ios").toFile()
        try {
            IosProjectTools.prepareKotlinNativeIosProject(folder)
            val plist = folder["app/Info.plist"].readText()
            assertTrue(plist.contains("<key>UIApplicationSceneManifest</key>"))
            assertTrue(plist.contains("<key>UIApplicationSupportsMultipleScenes</key>"))
        } finally {
            folder.deleteRecursively()
        }
    }
}
