package com.soywiz.korge.view

import com.soywiz.korge.render.*

//fun Container.deferredView(deferred: Boolean? = true, block: DeferredView.() -> Unit): DeferredView =
//    append(DeferredView(deferred)) { block() }
//
//class DeferredView(var deferred: Boolean? = true) : Container() {
//    override fun renderInternal(ctx: RenderContext) {
//        ctx.flush()
//        ctx.batch.mode(
//            when (deferred) {
//                true -> BatchBuilder2D.RenderMode.DEFERRED
//                false -> BatchBuilder2D.RenderMode.NORMAL
//                else -> null
//            }
//        ) {
//            super.renderInternal(ctx)
//        }
//        ctx.flush()
//    }
//}
