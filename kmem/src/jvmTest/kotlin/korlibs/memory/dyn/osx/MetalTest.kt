package korlibs.memory.dyn.osx

import com.sun.jna.*
import korlibs.memory.*
import korlibs.memory.Platform
import kotlin.test.*

class MetalTest {
    @Test
    fun test() {
        if (Platform.isMac) {
            println(ObjcProtocolRef.listAll())
            /*
            val MTLDevice = ObjectiveC.getClassByName("_MTLDevice")!!
            //println(ObjectiveC.getAllClassIDs())
            for (method in MTLDevice.listMethods()) {
                println("$method")
            }

             */
            //ObjectiveC.objc_getClass("")
        }
    }
}
