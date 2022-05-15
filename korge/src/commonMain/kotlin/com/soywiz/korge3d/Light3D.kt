package com.soywiz.korge3d

import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.Vector3D

@Korge3DExperimental
fun Container3D.light(
	color: RGBA = Colors.WHITE,
	constantAttenuation: Double = 1.0,
	linearAttenuation: Double = 0.0,
	quadraticAttenuation: Double = 0.00111109,
	callback: Light3D.() -> Unit = {}
) = Light3D(color, constantAttenuation, linearAttenuation, quadraticAttenuation).addTo(this, callback)

@Korge3DExperimental
open class Light3D(
	var color: RGBA = Colors.WHITE,
	var constantAttenuation: Double = 1.0,
	var linearAttenuation: Double = 0.0,
	var quadraticAttenuation: Double = 0.00111109
) : View3D() {
	internal val colorVec = Vector3D()
	internal val attenuationVec = Vector3D()

	fun setTo(
		color: RGBA = Colors.WHITE,
		constantAttenuation: Double = 1.0,
		linearAttenuation: Double = 0.0,
		quadraticAttenuation: Double = 0.00111109
	): Light3D {
		this.color = color
		this.constantAttenuation = constantAttenuation
		this.linearAttenuation = linearAttenuation
		this.quadraticAttenuation = quadraticAttenuation
        return this
	}

	override fun render(ctx: RenderContext3D) {
	}
}
