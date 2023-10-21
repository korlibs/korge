package korlibs.render.awt

import korlibs.graphics.gl.*
import korlibs.kgl.*
import korlibs.platform.*
import korlibs.render.*
import korlibs.render.osx.*
import korlibs.render.win32.*
import korlibs.render.x11.*

fun AGOpenglAWT(config: GameWindowCreationConfig, context: KmlGlContext? = null): AGOpengl = AGOpenglAWT(config.checkGl, config.logGl, config.cacheGl, context)
fun AGOpenglAWT(checkGl: Boolean = false, logGl: Boolean = false, cacheGl: Boolean = false, context: KmlGlContext? = null): AGOpengl = AGOpengl(
    when {
        Platform.isMac -> MacKmlGl()
        Platform.isWindows -> Win32KmlGl()
        else -> X11KmlGl()
    }.checkedIf(checkGl).cachedIf(cacheGl).logIf(logGl, logBefore = false, logAfter = logGl),
    context
)
