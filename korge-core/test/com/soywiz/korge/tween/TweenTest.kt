package com.soywiz.korge.tween

import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korge.time.milliseconds
import com.soywiz.korge.tween.Easing
import com.soywiz.korio.async.go
import org.junit.Assert
import org.junit.Test

class TweenTest : ViewsForTesting() {
	@Test
	fun name() = viewsTest(step = 20.milliseconds) {
		val result = arrayListOf<Any>()
		val result2 = arrayListOf<Any>()

		class Demo(var a: Int = -10) {
			var b: Int = 0
			var c: Int = 0
		}

		val demo = Demo()

		val p1 = go {
			views.stage.tween(demo::b[100, 200], time = 100.milliseconds, easing = Easing.Companion.LINEAR) {
				result2 += "[b=" + demo.b + ":" + it + "]"
			}
		}
		val p2 = go {
			views.stage.tween(demo::c[100, 200], time = 100.milliseconds, easing = Easing.Companion.LINEAR) {
				result2 += "[c=" + demo.c + ":" + it + "]"
			}
		}

		views.stage.tween(demo::a[+10], time = 100.milliseconds, easing = Easing.Companion.LINEAR) {
			result += "[" + demo.a + ":" + it + "]"
		}
		result += "---"
		views.stage.tween(demo::a[-100, +100], time = 100.milliseconds, easing = Easing.Companion.LINEAR) {
			result += "[" + demo.a + ":" + it + "]"
		}
		result += "---"

		p1.await()
		p2.await()

		Assert.assertEquals(
			"[-10:0.0],[-6:0.2],[-2:0.4],[2:0.6],[6:0.8],[10:1.0],---,[-100:0.0],[-60:0.2],[-20:0.4],[20:0.6],[60:0.8],[100:1.0],---",
			result.joinToString(",")
		)

		Assert.assertEquals(
			"[b=100:0.0],[c=100:0.0],[b=120:0.2],[c=120:0.2],[b=140:0.4],[c=140:0.4],[b=160:0.6],[c=160:0.6],[b=180:0.8],[c=180:0.8],[b=200:1.0],[c=200:1.0]",
			result2.joinToString(",")
		)
	}
}
