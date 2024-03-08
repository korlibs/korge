package korlibs.io.file.std

import korlibs.io.*
import korlibs.io.file.*
import korlibs.platform.*
import kotlinx.cinterop.*
import platform.Foundation.*

open class StandardBasePathsDarwin : StandardBasePathsNative() {
    override val executableFile: String get() = kotlinx.cinterop.autoreleasepool { platform.Foundation.NSBundle.mainBundle.executablePath ?: "./a.out" }
    override val executableFolder: String get() = PathInfo(executableFile).parent.fullPath
    override val resourcesFolder: String by lazy {
        val path = nativeCwd()
        var cpath = path
        while (cpath.length > 2) {
            if (NSFileManager().contentsOfDirectoryAtPath(path, null) != null) break
            cpath = PathInfo(cpath).parent.fullPath
        }
        cpath
    }
    override fun appPreferencesFolder(appId: String): String {
        return when {
            Platform.isMac -> NSSearchPathForDirectoriesInDomains(NSLibraryDirectory.convert(), NSUserDomainMask.convert(), true).first().toString() + "/Preferences/$appId"
            else -> NSSearchPathForDirectoriesInDomains(NSDocumentDirectory.convert(), NSUserDomainMask.convert(), true).first().toString()
            //else -> NSSearchPathForDirectoriesInDomains(NSLibraryDirectory.convert(), NSUserDomainMask.convert(), true).first().toString() + "/Preferences/$appId"
        }
    }

}
