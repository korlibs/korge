package com.soywiz.korgw.awt

import com.soywiz.kgl.*
import com.soywiz.kmem.*
import com.soywiz.korag.gl.AGOpengl
import com.soywiz.korgw.*
import com.soywiz.korgw.osx.MacKmlGL
import com.soywiz.korgw.win32.Win32KmlGl
import com.soywiz.korgw.x11.X11KmlGl

fun AGOpenglAWT(config: GameWindowCreationConfig, context: KmlGlContext? = null): AGOpengl = AGOpenglAWT(config.checkGl, config.logGl, config.cacheGl, context)
fun AGOpenglAWT(checkGl: Boolean = false, logGl: Boolean = false, cacheGl: Boolean = false, context: KmlGlContext? = null): AGOpengl = AGOpengl(
    when {
        //OS.isMac -> MacKmlGL.checked(throwException = false)
        Platform.isMac -> MacKmlGL()
        Platform.isWindows -> Win32KmlGl()
        else -> X11KmlGl()
    }
        .checkedIf(checkGl)
        .cachedIf(cacheGl)
        .logIf(logGl, logBefore = false, logAfter = logGl),
    context
)
