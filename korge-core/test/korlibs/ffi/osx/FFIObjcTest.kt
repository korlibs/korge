package korlibs.ffi.osx

import korlibs.ffi.*
import korlibs.io.async.*
import korlibs.platform.*
import kotlin.test.*

class FFIObjcTest {
    @Test
    fun test() = suspendTest({ Platform.isMac && FFILib.isFFISupported }){
        //println(NSDictionary().objcClass)
        val dict = NSMutableDictionary()
        dict[10] = 10
        //dict[10] = 20
        dict[20] = 30
        println(dict.count)
        //println("dict=$dict")
        //println(ObjcClassRef.listAll())
        //ObjcClassRef.fromName("NSDictionary")?.dumpKotlin()
        //ObjcProtocol.fromName("NSDictionary")?.dumpKotlin()
    }
}
