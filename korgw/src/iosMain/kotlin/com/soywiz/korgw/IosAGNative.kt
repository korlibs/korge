package com.soywiz.korgw

import com.soywiz.kgl.checkedIf
import com.soywiz.korag.gl.AGNative
import platform.UIKit.UIScreen

open class IosAGNative : AGNative() {
    override val gl: com.soywiz.kgl.KmlGl = com.soywiz.kgl.KmlGlNative(gles = true).checkedIf(checked = false)
}
