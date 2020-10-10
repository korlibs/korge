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
    fun test() = suspendTest({ !OS.isJs }) {
        var expectedResult = """walk,188.15375,135.85548,-51.369995
walk,225.49426,175.73358,-37.55294
walk,230.3062,184.0278,-33.824528
walk,212.12567,164.75337,-37.166363
walk,192.29419,148.27841,-40.16516
walk,166.2741,129.18211,-45.47488
walk,128.5177,105.22072,-54.69001
walk,122.070526,92.15753,-58.378094
walk,100.309746,77.44629,-68.151695
walk,86.26349,71.95198,-75.823
walk,98.45205,80.439896,-75.30566
walk,146.31877,107.18881,-63.339497"""

        val resource = resourcesVfs["spineboy/spineboy-ess.json"]
        val json = SkeletonJson(object : AttachmentLoader { })
        val skeletonData = json.readSkeletonData(resource)
        val skeleton = Skeleton(skeletonData)
        val bone = skeleton.findBone("gun-tip")
        val fps = 1 / 15f
        var result = ""
        val animation = skeletonData.animations.find { it.name == "walk" } ?: throw Exception("Animation walk not found")
        var time = 0f
        while (time < animation.duration) {
            animation.apply(skeleton, time, time, false, null, 1f, MixBlend.first, MixDirection.`in`)
            skeleton.updateWorldTransform()
            result += "${animation.name},${bone!!.worldX},${bone.worldY},${bone.worldRotationX}\n"
            time += fps
        }
        assertEquals(expectedResult, result.trim())
    }
}

