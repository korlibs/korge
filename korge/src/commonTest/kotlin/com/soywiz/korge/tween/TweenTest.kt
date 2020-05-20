package com.soywiz.korge.tween

import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.tests.*
import com.soywiz.korio.async.*
import com.soywiz.korio.util.*
import com.soywiz.korma.interpolation.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.test.*

class TweenTest : ViewsForTesting(20.ms) {
	@Test
	@Ignore // @TODO: Flaky!
	fun name() = viewsTest {
		//println("BLOCK START")
		val result = arrayListOf<Any>()
		val result2 = arrayListOf<Any>()

		class Demo(var a: Int = -10) {
			var b: Int = 0
			var c: Int = 0
		}

		val demo = Demo()

		val p1 = asyncImmediately {
			views.stage.tween(demo::b[100, 200], time = 100.ms, easing = Easing.LINEAR) {
				result2 += "[b=" + demo.b + ":" + it.niceStr + "]"
				//println(result2)
			}
			//println("p1 done")
			//println(views.stage.unsafeListRawComponents)

		}
		val p2 = asyncImmediately {
			views.stage.tween(demo::c[100, 200], time = 100.ms, easing = Easing.LINEAR) {
				result2 += "[c=" + demo.c + ":" + it.niceStr + "]"
			}
			//println(views.stage.unsafeListRawComponents)
		}


		//println(views.stage.unsafeListRawComponents)
		views.stage.tween(demo::a[+10], time = 100.ms, easing = Easing.LINEAR) {
			result += "[" + demo.a + ":" + it.niceStr + "]"
		}
		result += "---"
		views.stage.tween(demo::a[-100, +100], time = 100.ms, easing = Easing.LINEAR) {
			result += "[" + demo.a + ":" + it.niceStr + "]"
		}
		result += "---"

		p1.await()
		p2.await()

		assertEquals(
			"[-10:0],[-6:0.2],[-2:0.4],[2:0.6],[6:0.8],[10:1],---,[-100:0],[-60:0.2],[-20:0.4],[20:0.6],[60:0.8],[100:1],---",
			result.joinToString(",")
		)

		assertEquals(
			"[b=100:0],[c=100:0],[b=120:0.2],[c=120:0.2],[b=140:0.4],[c=140:0.4],[b=160:0.6],[c=160:0.6],[b=180:0.8],[c=180:0.8],[b=200:1],[c=200:1]",
			result2.joinToString(",")
		)
	}
}
