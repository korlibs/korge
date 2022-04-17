package com.soywiz.korgw.awt

import com.soywiz.kgl.*
import com.soywiz.korag.gl.AGOpengl
import com.soywiz.korgw.osx.MacKmlGL
import com.soywiz.korgw.win32.Win32KmlGl
import com.soywiz.korgw.x11.X11KmlGl
import com.soywiz.korio.util.OS
import java.awt.Component
import java.awt.GraphicsEnvironment
import java.awt.Toolkit

class AwtAg(override val nativeComponent: Any, private val checkGl: Boolean, logGl: Boolean, val cacheGl: Boolean = false) : AGOpengl() {
    override val gles: Boolean = true
    override val linux: Boolean = OS.isLinux
    private var baseLazyGl: KmlGl? = null
    private var baseLazyGlWithLog: LogKmlGlProxy? = null
    private var lazyGl: KmlGlFastProxy? = null

    private val localGraphicsEnvironment : GraphicsEnvironment by lazy(LazyThreadSafetyMode.PUBLICATION) {
        GraphicsEnvironment.getLocalGraphicsEnvironment()
    }

    override val devicePixelRatio: Double get() {
        if (GraphicsEnvironment.isHeadless()) {
            return super.devicePixelRatio
        }
        // transform
        // https://stackoverflow.com/questions/20767708/how-do-you-detect-a-retina-display-in-java
        val config = (nativeComponent as? Component)?.graphicsConfiguration
            ?: localGraphicsEnvironment.defaultScreenDevice.defaultConfiguration
        return config.defaultTransform.scaleX
    }

    override val pixelsPerInch: Double by lazy(LazyThreadSafetyMode.PUBLICATION) {
        if (GraphicsEnvironment.isHeadless()) {
            return@lazy Companion.defaultPixelsPerInch
        }
        // maybe this is not just windows specific :
        // https://stackoverflow.com/questions/32586883/windows-scaling
        // somehow this value is not update when you change the scaling in the windows settings while the jvm is running :(
        return@lazy Toolkit.getDefaultToolkit().screenResolution.toDouble()
    }

    override val pixelsPerLogicalInchRatio: Double by lazy(LazyThreadSafetyMode.PUBLICATION) {
        pixelsPerInch / defaultPixelsPerInch
    }

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
