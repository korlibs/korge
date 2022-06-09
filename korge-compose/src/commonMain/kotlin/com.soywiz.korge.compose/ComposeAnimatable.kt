package com.soywiz.korge.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.kmem.clamp01
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.interpolate
import com.soywiz.korio.async.delay
import com.soywiz.korma.interpolation.Easing
import com.soywiz.korma.interpolation.interpolate

fun Animatable(color: RGBA) = Animatable(color, interpolator = { l, r -> interpolate(l, r) })
fun Animatable(value: Double) = Animatable(value, interpolator = { l, r -> interpolate(l, r) })

class Animatable<T>(initialValue: T, val interpolator: Double.(start: T, end: T) -> T) {
    var value: T by mutableStateOf(initialValue)
        internal set
    suspend fun animateTo(end: T, time: TimeSpan = 0.5.seconds, easing: Easing = Easing.EASE_IN_OUT) {
        val start = this.value
        var elapsed = 0.milliseconds
        while (true) {
            val ratio = (elapsed / time).clamp01()
            value = interpolator(easing(ratio), start, end)
            //println("ratio=$ratio, value=$value")
            delay(10.milliseconds)
            elapsed += 10.milliseconds
            if (ratio >= 1.0) break
        }
    }

    //operator fun getValue(t: Any, property: KProperty<*>): T {
    //    return value
    //}
    //operator fun setValue(t: Any, property: KProperty<*>, value: T) {
    //    this.value = value
    //}
}
