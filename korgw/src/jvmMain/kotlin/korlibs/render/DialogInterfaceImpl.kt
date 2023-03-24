package korlibs.render

import korlibs.render.awt.*
import java.awt.*

actual fun createDialogInterfaceForComponent(nativeComponent: Any?): DialogInterface =
    DialogInterfaceAwt { nativeComponent as? Component? }