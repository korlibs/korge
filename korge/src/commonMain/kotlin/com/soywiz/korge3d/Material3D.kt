package com.soywiz.korge3d

import com.soywiz.korag.*
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.setToColor
import com.soywiz.korma.geom.Vector3D

@Korge3DExperimental
data class Material3D(
	val emission: Light = LightColor(Colors.BLACK),
	val ambient: Light = LightColor(Colors.BLACK),
	val diffuse: Light = LightColor(Colors.BLACK),
	//val specular: Light = LightColor(RGBA.float(.5f, .5f, .5f, 1f)),
	val specular: Light = LightColor(Colors.BLACK),
	val shininess: Float = .5f,
	val indexOfRefraction: Float = 1f
) {
	@Korge3DExperimental
	open class Light(val kind: String)

	@Korge3DExperimental
	data class LightColor(val color: RGBA) : Light("color") {
		val colorVec = Vector3D().setToColor(color)
	}

	@Korge3DExperimental
	data class LightTexture(val bitmap: Bitmap?) : Light("texture") {
	}

	val kind: String = "${emission.kind}_${ambient.kind}_${diffuse.kind}_${specular.kind}"
}
