package com.soywiz.korui

import com.soywiz.korev.*
import com.soywiz.korio.util.*
import com.soywiz.korui.light.log.*
import com.soywiz.korui.ui.*
import kotlin.test.*

class BasicTest {
	fun applicationTest(callback: suspend Application.(LogLightComponents) -> Unit) {
		val lc = LogLightComponents()
		Korui {
			Application(lc) {
				callback(lc)
			}
		}
	}

	@Test
    @Ignore // @TODO: Check
	fun name(): Unit {
		if (OS.isNative) return // @TODO: Ignore kotlin-native for now
		return applicationTest { lc ->
			val frame = frame("Title") {
				button("Hello")
			}

			frame.dispatch(ReshapeEvent(100, 100))
			frame.dispatch(ReshapeEvent(200, 200))

			assertEquals(
				"""
					create(FRAME)=0
					setProperty(FRAME0,LightProperty[TEXT],Title)
					setBounds(FRAME0,0,0,640,480)
					create(BUTTON)=0
					setProperty(BUTTON0,LightProperty[TEXT],Hello)
					setParent(BUTTON0,FRAME0)
					setBounds(BUTTON0,0,0,640,480)
					setBounds(FRAME0,0,0,640,480)
					setProperty(FRAME0,LightProperty[VISIBLE],true)
					setBounds(BUTTON0,0,0,100,100)
					setBounds(FRAME0,0,0,100,100)
					setBounds(BUTTON0,0,0,200,200)
					setBounds(FRAME0,0,0,200,200)
				""".trimIndent(),
				lc.log.joinToString("\n")
			)
		}
	}
}
