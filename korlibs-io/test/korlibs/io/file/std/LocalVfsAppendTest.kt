package korlibs.io.file.std

import korlibs.io.async.*
import korlibs.platform.*
import kotlin.test.*

class LocalVfsAppendTest {
    @Test
    fun test() = suspendTest(cond = { localVfs(StandardPaths.temp).vfs is LocalVfs }) {
        AppendBaseTest._testAppendVfs(localVfs(StandardPaths.temp)["file.append.${Platform.rawPlatformName}.txt"])
    }
}
