package com.soywiz.korge.animate

import com.soywiz.klock.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class AnimStateManagerTest {
    @Test
    fun test() {
        val log = arrayListOf<String>()
        val view = DummyView()
        val state1 = AnimState(view::x[100], time = 0.5.seconds)
        view.animStateManager.set(state1)
        fun log() { log += "${view.pos.niceStr} : ${view.alpha.niceStr(1)}" }

        log()
        view.updateSingleView(0.seconds)
        log()
        repeat(2) {
            view.updateSingleView(0.25.seconds)
            log()
        }
        view.animStateManager.set()
        repeat(2) {
            view.updateSingleView(0.25.seconds)
            log()
        }

        assertEquals(
            """
                (0, 0) : 1
                (0, 0) : 1
                (50, 0) : 1
                (100, 0) : 1
                (50, 0) : 1
                (0, 0) : 1
            """.trimIndent(),
            log.joinToString("\n")
        )
    }
}
