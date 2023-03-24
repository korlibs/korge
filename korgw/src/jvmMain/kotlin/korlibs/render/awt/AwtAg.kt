package korlibs.render.awt

import korlibs.kgl.*
import korlibs.memory.*
import korlibs.graphics.gl.AGOpengl
import korlibs.render.*
import korlibs.render.osx.MacKmlGL
import korlibs.render.win32.Win32KmlGl
import korlibs.render.x11.X11KmlGl

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
