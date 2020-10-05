package com.soywiz.korge.admob

import com.soywiz.korge.view.Views
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.Matrix

actual suspend fun AdmobCreate(views: Views, testing: Boolean): Admob = object : Admob(views) {
    override suspend fun available(): Boolean = false
    override suspend fun bannerShow() {
        views.onAfterRender {
            it.batch.drawQuad(
                it.getTex(Bitmaps.white),
                x = 0f,
                y = 0f,
                width = it.ag.mainRenderBuffer.width.toFloat(),
                height = 86f,
                colorMul = Colors["#f0f0f0"],
                m = Matrix()
            )
        }
        super.bannerShow()
    }
}
