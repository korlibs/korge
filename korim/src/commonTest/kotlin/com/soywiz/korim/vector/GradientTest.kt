package com.soywiz.korim.vector

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.NativeImageOrBitmap32
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.util.OS
import kotlin.test.Test

class GradientTest {
    @Test
    fun test() = suspendTest({ !OS.isAndroid }) {
        val tex = Bitmap32(24, 24) { x, y -> if ((x / 3 + y / 3) % 2 == 0) Colors.RED else Colors.BLUE }
        //NativeImageOrBitmap32(512, 512, true).context2d {
        NativeImageOrBitmap32(512, 512, false).context2d {
            //fillStyle = createPattern(tex, CycleMethod.REPEAT)
            fillStyle = createRadialGradient(100, 100, 20, 150, 100, 100, CycleMethod.REFLECT).addColorStop(0, Colors.ORANGERED).addColorStop(1, Colors.DODGERBLUE)
            //fillStyle = createSweepGradient(100, 100).addColorStop(0, Colors.RED).addColorStop(1, Colors.DARKGREEN)
            fillRect(0, 0, 512, 512)
        }
        //.showImageAndWait()
    }
}
