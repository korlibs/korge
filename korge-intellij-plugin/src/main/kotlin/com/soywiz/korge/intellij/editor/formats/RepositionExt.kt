package com.soywiz.korge.intellij.editor.formats

import com.soywiz.korge.component.onStageResized
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.position
import com.soywiz.korge.view.scale
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.ScaleMode

fun View.repositionOnResize(views: Views) {
    val view = this
    val localBounds = view.getLocalBounds()
    val initialSize = localBounds.size

    fun reposition() {
        val outRect = Rectangle(0, 0, views.actualVirtualWidth, views.actualVirtualHeight).place(initialSize, Anchor.MIDDLE_CENTER, ScaleMode.SHOW_ALL)
        val scaleX = outRect.width / initialSize.width
        val scaleY = outRect.height / initialSize.height
        view.position(outRect.x - localBounds.x * scaleX, outRect.y - localBounds.y * scaleY).scale(scaleX, scaleY)
        println("############# repositionOnResize")
        println("repositionOnResize.virtualSize: ${views.virtualWidth}x${views.virtualHeight}")
        println("repositionOnResize.localBounds: $localBounds")
        println("repositionOnResize.initialSize: $initialSize")
        println("repositionOnResize.outRect: $outRect")
        println("repositionOnResize.scale: $scaleX,$scaleY")
    }

    view.onStageResized(firstTrigger = false) { width, height ->
        reposition()
    }
    reposition()
}
