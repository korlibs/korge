package com.soywiz.korge3d

import com.soywiz.korge3d.internal.toFast
import com.soywiz.korma.geom.Matrix3D
import com.soywiz.korma.geom.invert

@Korge3DExperimental
open class Joint3D constructor(
	val jid: String,
	val jname: String,
	val jsid: String,
	val jointParent: Joint3D? = null,
	initialMatrix: Matrix3D
) : Container3D() {
	init {
		this.transform.setMatrix(initialMatrix)
		this.name = jname
		this.id = jid
		if (jointParent != null) {
			this.parent = jointParent
		}

	}

	val poseMatrix = this.transform.globalMatrix.clone()
	val poseMatrixInv = poseMatrix.clone().invert()

	val childJoints = arrayListOf<Joint3D>()
	val descendants: List<Joint3D> get() = childJoints.flatMap { it.descendantsAndThis }
	val descendantsAndThis: List<Joint3D> get() = listOf(this) + descendants

	//val jointTransform = Transform3D()

	override fun render(ctx: RenderContext3D) {
	}

	override fun toString(): String = "Joint3D(id=$jid, name=$name, sid=$jsid)"
}

@Korge3DExperimental
data class Bone3D constructor(
	val index: Int,
	val name: String,
	val invBindMatrix: Matrix3D
)

@Korge3DExperimental
data class Skin3D(val bindShapeMatrix: Matrix3D, val bones: List<Bone3D>) {
	val bindShapeMatrixInv = bindShapeMatrix.clone().invert()
	val matrices = Array(bones.size) { Matrix3D() }
}

@Korge3DExperimental
class Skeleton3D(val skin: Skin3D, val headJoint: Joint3D) : View3D() {
	val allJoints = headJoint.descendantsAndThis
	val jointsByName = allJoints.associateBy { it.jname }.toFast()
	val jointsBySid = allJoints.associateBy { it.jsid }.toFast()

	override fun render(ctx: RenderContext3D) {
	}
}

