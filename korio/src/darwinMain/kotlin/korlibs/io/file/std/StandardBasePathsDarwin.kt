package korlibs.io.file.std

import korlibs.io.file.PathInfo
import korlibs.io.file.parent
import korlibs.io.posix.posixRealpath
import korlibs.memory.Platform
import kotlinx.cinterop.*
import platform.Foundation.*

open class StandardBasePathsDarwin : StandardBasePathsNative() {
    override val executableFile: String get() = kotlinx.cinterop.autoreleasepool { platform.Foundation.NSBundle.mainBundle.executablePath ?: "./a.out" }
    override val executableFolder: String get() = PathInfo(executableFile).parent.fullPath
    override val resourcesFolder: String get() = kotlinx.cinterop.autoreleasepool { platform.Foundation.NSBundle.mainBundle.resourcePath ?: posixRealpath(".") ?: "." }
    override fun appPreferencesFolder(appId: String): String {
        return when {
            Platform.isMac -> NSSearchPathForDirectoriesInDomains(NSLibraryDirectory.convert(), NSUserDomainMask.convert(), true).first().toString() + "/Preferences/$appId"
            else -> NSSearchPathForDirectoriesInDomains(NSDocumentDirectory.convert(), NSUserDomainMask.convert(), true).first().toString()
            //else -> NSSearchPathForDirectoriesInDomains(NSLibraryDirectory.convert(), NSUserDomainMask.convert(), true).first().toString() + "/Preferences/$appId"
        }
    }

}
