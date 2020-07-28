package com.soywiz.korge.intellij.util

import com.soywiz.korim.color.RGBA
import java.awt.Color
import javax.swing.plaf.ColorUIResource

//val controlRgba = MetalLookAndFeel.getCurrentTheme().control.rgba()

fun ColorUIResource.rgba(): RGBA {
    return RGBA(red, green, blue, alpha)
}

fun Color.rgba(): RGBA {
    return RGBA(red, green, blue, alpha)
}
