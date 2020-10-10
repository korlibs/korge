package com.esotericsoftware.spine

import com.esotericsoftware.spine.utils.SpineUtils
import com.esotericsoftware.spine.utils.SpineVector2
import com.soywiz.korma.geom.Matrix3D
import com.soywiz.korma.geom.Vector3D
import kotlin.math.absoluteValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.asserter

class BoneTest {
    @Test
    fun testSimpleRotation() {
        val data = BoneData(0, "test", null)
        data.x = 0.0f
        data.y = 0.0f
        data.transformMode = BoneData.TransformMode.normal
        val skeleton = Skeleton(SkeletonData())
        val bone = Bone(data, skeleton, null)
        bone.rotation = 90.0f

        bone.update()
        val origoToWorld = bone.localToWorld(SpineVector2(0.0f, 0.0f))
        assertClose(0.0f, origoToWorld.x)
        assertClose(0.0f, origoToWorld.y)

        val oneZeroToWorld = bone.localToWorld(SpineVector2(1.0f, 0.0f))
        assertClose(0.0f, oneZeroToWorld.x)
        assertClose(1.0f, oneZeroToWorld.y)
        val matrix3D = bone.getWorldTransform(Matrix3D())
        val transformWithMatrix = Vector3D(1.0f, 0.0f, 0.0f).transform(matrix3D)
        assertClose(oneZeroToWorld.x, transformWithMatrix.x)
        assertClose(oneZeroToWorld.y, transformWithMatrix.y)
    }

    private fun assertClose(expected: Float, actual: Float, epsilon: Float = EPSILON) = assertTrue((expected - actual).absoluteValue <= epsilon, "Expected $expected to be close to $actual (epsilon=$epsilon)")
    companion object {
        const val EPSILON = 1e-7f
    }
}

