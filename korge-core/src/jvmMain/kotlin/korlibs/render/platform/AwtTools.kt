package korlibs.render.platform

import com.sun.jna.*
import korlibs.io.dynamic.*
import korlibs.render.osx.*
import java.awt.*

fun Component.awtGetPeer(): Any {
    //Class.forName("AWTAccessor")
    require(!this.isLightweight) { "Component must be heavyweight" }
    require(this.isDisplayable) { "Component must be displayable" }

    try {
        val accessor = Dyn.global["sun.awt.AWTAccessor"].dynamicInvoke("getComponentAccessor")
        //println("accessor=$accessor")
        return accessor.dynamicInvoke("getPeer", this@awtGetPeer).value ?: Unit
    } catch (e: Throwable) {
        e.printStackTrace()
    }

    this.dyn.dynamicInvoke("getPeer").value?.let { return it }
    this.dyn["peer"].value?.let { return it }
    error("Can't get peer from Frame")
}

fun Component.awtNativeHandle(): Long = Native.getComponentPointer(this).address
