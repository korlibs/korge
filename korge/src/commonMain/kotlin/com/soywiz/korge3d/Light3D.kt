package com.soywiz.korge3d

import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*

@Korge3DExperimental
fun Container3D.light(
	color: RGBA = Colors.WHITE,
	constantAttenuation: Double = 1.0,
	linearAttenuation: Double = 0.0,
	quadraticAttenuation: Double = 0.00111109,
	callback: Light3D.() -> Unit = {}
) = Light3D(color, constantAttenuation, linearAttenuation, quadraticAttenuation).apply(callback).addTo(this)

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
