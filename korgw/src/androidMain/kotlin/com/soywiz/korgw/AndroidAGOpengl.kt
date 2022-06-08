package com.soywiz.korgw

import android.content.Context
import com.soywiz.kgl.KmlGl
import com.soywiz.kgl.KmlGlAndroid
import com.soywiz.kgl.checkedIf
import com.soywiz.kgl.logIf
import com.soywiz.korag.gl.AGOpengl

class AndroidAGOpengl(
    val androidContext: Context,
    val agCheck: Boolean = false,
    val mGLView: (() -> KorgwSurfaceView?)? = null,
) : AGOpengl() {
    //override val gl: KmlGl = CheckErrorsKmlGlProxy(KmlGlAndroid())
    override val gl: KmlGl = KmlGlAndroid { mGLView?.invoke()?.clientVersion ?: -1 }.checkedIf(agCheck).logIf(agCheck)
    override val nativeComponent: Any get() = this@AndroidAGOpengl

    override fun repaint() {
        mGLView?.invoke()?.invalidate()
    }

    // @TODO: Cache somehow?
    override val pixelsPerInch: Double get() = androidContext.resources.displayMetrics.xdpi.toDouble()

    init {
        println("KorgwActivityAGOpengl: Created ag $this for ${this@AndroidAGOpengl} with gl=$gl")
    }
}
