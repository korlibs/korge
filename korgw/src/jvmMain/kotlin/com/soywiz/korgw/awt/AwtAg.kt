package com.soywiz.korgw.awt

import com.soywiz.kgl.*
import com.soywiz.korag.*
import com.soywiz.korgw.osx.*
import com.soywiz.korgw.win32.*
import com.soywiz.korgw.x11.*
import com.soywiz.korio.util.*

class AwtAg(override val nativeComponent: Any, private val checkGl: Boolean) : AGOpengl() {
    override val gles: Boolean = true
    override val linux: Boolean = OS.isLinux
    private var lazyGl: KmlGl? = null

    override val gl: KmlGl get() {
        if (lazyGl == null) lazyGl = buildGl()
        return lazyGl!!
    }

    private fun buildGl(): KmlGl {
        return when {
            //OS.isMac -> MacKmlGL.checked(throwException = false)
            OS.isMac -> MacKmlGL
            OS.isWindows -> Win32KmlGl
            else -> X11KmlGl
        }.checkedIf(checkGl)
    }
}
