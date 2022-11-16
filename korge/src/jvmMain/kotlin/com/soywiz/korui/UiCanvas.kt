package com.soywiz.korui

import com.soywiz.korim.bitmap.*
import com.soywiz.korui.layout.*
import com.soywiz.korui.native.*

internal open class UiCanvas(app: UiApplication, val canvas: NativeUiFactory.NativeCanvas = app.factory.createCanvas()) : UiComponent(app, canvas) {
    var image: Bitmap?
        get() = canvas.image
        set(value) {
            canvas.image = value
            this.preferredWidth = value?.width?.pt
            this.preferredHeight = value?.height?.pt
        }

    override fun copyFrom(that: UiComponent) {
        super.copyFrom(that)
        that as UiCanvas
        this.image = that.image
    }
}

internal inline fun UiContainer.canvas(image: Bitmap? = null, block: UiCanvas.() -> Unit): UiCanvas {
    return UiCanvas(app).also { it.image = image }.also { it.parent = this }.also(block)
}
