package com.soywiz.korgw.awt

import com.soywiz.kgl.*
import com.soywiz.korag.gl.AGOpengl
import com.soywiz.korgw.osx.MacKmlGL
import com.soywiz.korgw.win32.Win32KmlGl
import com.soywiz.korgw.x11.X11KmlGl
import com.soywiz.korio.util.OS

fun AwtAg(checkGl: Boolean, logGl: Boolean, cacheGl: Boolean = false): AGOpengl = AGOpengl(when {
    //OS.isMac -> MacKmlGL.checked(throwException = false)
    OS.isMac -> MacKmlGL()
    OS.isWindows -> Win32KmlGl()
    else -> X11KmlGl()
}.checkedIf(checkGl).cachedIf(cacheGl).logIf(logGl, logBefore = false, logAfter = logGl))
