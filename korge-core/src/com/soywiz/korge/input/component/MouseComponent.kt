package com.soywiz.korge.input.component

import com.soywiz.korge.component.Component
import com.soywiz.korge.input.Input
import com.soywiz.korge.view.View
import com.soywiz.korio.async.Signal
import com.soywiz.korio.util.extraProperty

class MouseComponent(view: View) : Component(view) {
    val input = views.input
    val frame = input.frame
    val onClick = Signal<Unit>()
    val onOver = Signal<Unit>()
    val onOut = Signal<Unit>()

    private var lastOver = false

    var Input.Frame.mouseHitSearch by extraProperty("mouseHitSearch", false)
    var Input.Frame.mouseHitResult by extraProperty<View?>("mouseHitResult", null)

    override fun update(dtMs: Int) {
        if (!frame.mouseHitSearch) {
            frame.mouseHitSearch = true
            frame.mouseHitResult = views.root.hitTest(input.mouse)
        }
        val hitTest = input.frame.mouseHitResult
        val over = (hitTest == view)

        //println("MouseComponent: $hitTest, $over")

        if (lastOver != over) {
            if (over) {
                onOver(Unit)
            } else {
                onOut(Unit)
            }
        }

        lastOver = over
    }
}

//var Input.Frame.mouseHitResult by Extra.Property<View?>("mouseHitResult") {
//    views.root.hitTest(input.mouse)
//}


val View.mouse: MouseComponent get() = this.getOrCreateComponent { MouseComponent(this) }

val View.onOver get() = mouse.onOver
val View.onOut get() = mouse.onOut