package com.soywiz.korgw.awt

import com.soywiz.kgl.*
import com.soywiz.korag.*
import com.soywiz.korgw.osx.*
import com.soywiz.korgw.win32.*
import com.soywiz.korgw.x11.*
import com.soywiz.korio.util.*

class AwtAg(override val nativeComponent: Any, private val checkGl: Boolean, logGl: Boolean, val cacheGl: Boolean = false) : AGOpengl() {
    override val gles: Boolean = true
    override val linux: Boolean = OS.isLinux
    private var baseLazyGl: KmlGl? = null
    private var baseLazyGlWithLog: LogKmlGlProxy? = null
    private var lazyGl: KmlGlFastProxy? = null

    var logGl: Boolean = logGl
        set(value) {
            field = value
            setLogGl()
        }

    private fun setLogGl() {
        lazyGl?.parent = if (logGl) baseLazyGlWithLog!! else baseLazyGl!!
        baseLazyGlWithLog?.logBefore = false
        baseLazyGlWithLog?.logAfter = logGl
    }

    override val gl: KmlGlFastProxy get() {
        if (baseLazyGl == null) {
            baseLazyGl = buildGl().cachedIf(cacheGl)
            baseLazyGlWithLog = LogKmlGlProxy(baseLazyGl!!)
            lazyGl = KmlGlFastProxy(baseLazyGl!!)
            setLogGl()
        }
        return lazyGl!!
    }

    private fun buildGl(): KmlGl {
        return when {
            //OS.isMac -> MacKmlGL.checked(throwException = false)
            OS.isMac -> MacKmlGL()
            OS.isWindows -> Win32KmlGl()
            else -> X11KmlGl()
        }.checkedIf(checkGl)
    }

    override fun beforeDoRender() {
        gl.beforeDoRender(contextVersion)
    }
}
