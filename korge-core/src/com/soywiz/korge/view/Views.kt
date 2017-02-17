package com.soywiz.korge.view

import com.soywiz.korag.AG
import com.soywiz.korge.input.Input
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.Texture
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.inject.Singleton

@Singleton
class Views(
        val ag: AG,
        val injector: AsyncInjector,
        val input: Input
) {
    var lastId = 0
    val renderContext = RenderContext(ag)
    fun container() = Container(this)

    val root: Container = container()
    fun render() {
        root.render(renderContext)
        renderContext.flush()
    }

    fun dump(emit: (String) -> Unit = ::println) = dumpView(root, emit)

    fun dumpView(view: View, emit: (String) -> Unit = ::println, indent: String = "") {
        emit("$indent$view")
        if (view is Container) {
            for (child in view.children) {
                dumpView(child, emit, "$indent ")
            }
        }
    }

    fun update(dtMs: Int) {
        input.frame.reset()
        root.update(dtMs)
    }
}
