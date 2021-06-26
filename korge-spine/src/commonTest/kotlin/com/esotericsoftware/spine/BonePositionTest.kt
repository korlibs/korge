package com.esotericsoftware.spine

import com.esotericsoftware.spine.Animation.MixBlend
import com.esotericsoftware.spine.Animation.MixDirection
import com.esotericsoftware.spine.attachments.*
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.util.OS
import kotlin.test.Test
import kotlin.test.assertEquals

class BonePositionTest {
    @Test
    fun test() = suspendTest({ !OS.isJs && !OS.isAndroid }) {
        val resource = resourcesVfs["spineboy/spineboy-ess.json"]
        val json = SkeletonJson(object : AttachmentLoader { })
        val skeletonData = json.readSkeletonData(resource)
        val skeleton = Skeleton(skeletonData)
        val bone = skeleton.findBone("gun-tip")
        val fps = 1 / 15f
        val animation = skeletonData.animations.find { it.name == "walk" } ?: throw Exception("Animation walk not found")
        var time = 0f
        val lines = arrayListOf<List<Any?>>()
        while (time < animation.duration) {
            animation.apply(skeleton, time, time, false, null, 1f, MixBlend.first, MixDirection.`in`)
            skeleton.updateWorldTransform()
            lines += listOf(animation.name, bone!!.worldX, bone!!.worldY, bone.worldRotationX)
            time += fps
        }
        assertEqualsDeep(listOf<Any?>(
            listOf("walk", 188.15375, 135.85548, -51.369995),
            listOf("walk", 225.49426, 175.73358, -37.55294),
            listOf("walk", 230.3062, 184.0278, -33.824528),
            listOf("walk", 212.12567, 164.75337, -37.166363),
            listOf("walk", 192.29419, 148.27841, -40.16516),
            listOf("walk", 166.2741, 129.18211, -45.47488),
            listOf("walk", 128.5177, 105.22072, -54.69001),
            listOf("walk", 122.070526, 92.15753, -58.378094),
            listOf("walk", 100.309746, 77.44629, -68.151695),
            listOf("walk", 86.26349, 71.95198, -75.823),
            listOf("walk", 98.45205, 80.439896, -75.30566),
            listOf("walk", 146.31877, 107.18881, -63.339497),
        ), lines)
    }

    fun <T> assertEqualsDeep(a: T, b: T) {
        _assertEqualsDeep(a, b, a, b)
    }

    private fun <T> _assertEqualsDeep(a: T, b: T, originalA: Any?, originalB: Any?) {
        if (a is Iterable<*> && b is Iterable<*>) {
            val aCount = a.count()
            val bCount = b.count()
            assertEquals(aCount, bCount, "$originalA != $originalB")
            for (item in a.zip(b)) {
                _assertEqualsDeep<Any?>(item.first, item.second, originalA, originalB)
            }
            return
        }
        if (a is Double && b is Double) return assertEquals(a, b, 0.001, "($a != $b) -- ($originalA != $originalB)")
        if (a is Float && b is Float) return assertEquals(a, b, 0.001f, "($a != $b) -- ($originalA != $originalB)")
        if (a is Int && b is Int) return assertEquals(a, b, "($a != $b) -- ($originalA != $originalB)")
        if (a is Long && b is Long) return assertEquals(a, b, "($a != $b) -- ($originalA != $originalB)")
        if (a is Number && b is Number) return assertEquals(a.toDouble(), b.toDouble(), 0.001, "($a != $b) -- ($originalA != $originalB)")
        return assertEquals(a, b, "($a != $b) -- ($originalA != $originalB)")
    }
}

