package com.soywiz.korge3d

import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.addTo
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.setToColorPremultiplied

@Korge3DExperimental
inline fun Container.scene3D(views: Views3D = Views3D(stage!!.views), callback: Stage3D.() -> Unit = {}): Stage3DView {
    val stage3D = Stage3D(views)
    val view = Stage3DView(stage3D)
    view.addTo(this)
    stage3D.apply(callback)
    return view
}

@Korge3DExperimental
class Views3D(val views: Views) {
}

@Korge3DExperimental
class Stage3D(val views: Views3D) : Container3D() {
	lateinit var view: Stage3DView
	//var ambientColor: RGBA = Colors.WHITE
	var ambientColor: RGBA = Colors.BLACK // No ambient light
	var ambientPower: Double = 0.3
	var camera: Camera3D = Camera3D.Perspective().apply {
        this.root = this@Stage3D
		//positionLookingAt(0, 1, 10, 0, 0, 0)
	}
}

@Korge3DExperimental
class Stage3DView(val stage3D: Stage3D) : View() {
	init {
		stage3D.view = this
	}

	private val ctx3D = RenderContext3D()
	override fun renderInternal(ctx: RenderContext) {
		ctx.flush()
		ctx.clear(depth = 1f, clearColor = false)
		//ctx.ag.clear(color = Colors.RED)
		ctx3D.ag = ctx.ag
		ctx3D.rctx = ctx
		ctx3D.projMat.copyFrom(stage3D.camera.getProjMatrix(ctx.backWidth.toDouble(), ctx.backHeight.toDouble()))
		ctx3D.cameraMat.copyFrom(stage3D.camera.transform.matrix)
		ctx3D.ambientColor.setToColorPremultiplied(stage3D.ambientColor).scale(stage3D.ambientPower)
		ctx3D.cameraMatInv.invert(stage3D.camera.transform.matrix)
		ctx3D.projCameraMat.multiply(ctx3D.projMat, ctx3D.cameraMatInv)
		ctx3D.lights.clear()
		stage3D.foreachDescendant {
			if (it is Light3D) {
				if (it.active) ctx3D.lights.add(it)
			}
		}
		stage3D.render(ctx3D)
	}
}
