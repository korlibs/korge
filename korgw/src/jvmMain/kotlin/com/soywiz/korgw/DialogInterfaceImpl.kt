package com.soywiz.korgw

import com.soywiz.korgw.awt.*
import java.awt.*

actual fun createDialogInterfaceForComponent(nativeComponent: Any?): DialogInterface =
    DialogInterfaceAwt { nativeComponent as? Component? }
