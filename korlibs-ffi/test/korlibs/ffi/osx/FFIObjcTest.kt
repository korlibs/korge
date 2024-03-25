package korlibs.ffi.osx

import korlibs.ffi.*
import korlibs.platform.*
import kotlinx.coroutines.test.*
import kotlin.test.*

class FFIObjcTest {
    @Test
    fun test() = runTest {
        if (!Platform.isMac) return@runTest
        if (!FFILib.isFFISupported) return@runTest
        //println(NSDictionary().objcClass)
        val dict = NSMutableDictionary()
        dict[10] = 10
        //dict[10] = 20
        dict[20] = 30
        assertEquals(2, dict.count)
        //println("dict=$dict")
        //println(ObjcClassRef.listAll())
        //ObjcClassRef.fromName("NSDictionary")?.dumpKotlin()
        //ObjcProtocol.fromName("NSDictionary")?.dumpKotlin()
    }
}
