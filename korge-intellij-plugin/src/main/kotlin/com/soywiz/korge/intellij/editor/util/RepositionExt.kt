package com.soywiz.korge.intellij.editor.util

import com.soywiz.korge.component.onStageResized
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.position
import com.soywiz.korge.view.scale
import com.soywiz.korma.geom.*

open class RepositionResult(val view: View, val views: Views) {
    //val originalMatrix = view.localMatrix.copy()
    val originalMatrix = Matrix()
    var localBounds: Rectangle = Rectangle()
    var initialSize: Size = Size()

    fun forceBounds(bounds: Rectangle) {
        localBounds = bounds.copy()
        initialSize = bounds.size.copy()
        reposition()
    }

    fun refreshBounds() {
        val oldMatrix = view.localMatrix.copy()
        view.localMatrix = originalMatrix
        view.getLocalBoundsInternal(localBounds)
        view.localMatrix = oldMatrix
        forceBounds(localBounds)
    }

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

    init {
        view.onStageResized(firstTrigger = false) { width, height ->
            reposition()
        }
        println("Initial View.pos: ${view.pos}")
        refreshBounds()
    }
}

fun View.repositionOnResize(views: Views): RepositionResult {
    return RepositionResult(this, views)
}
