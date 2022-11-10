package com.soywiz.korge.view

import com.soywiz.korge.render.*
import kotlin.coroutines.*
import kotlin.test.*

class CachedContainerTest {
    @Test
    fun testCachingWorks() {
        val log = ViewsLog(EmptyCoroutineContext)
        val root = Stage(log.views)
        var rlog = ""
        lateinit var view: DummyView
        val cached = root.cachedContainer {
            view = object : DummyView() {
                override fun renderInternal(ctx: RenderContext) {
                    rlog += "a"
                }
            }.addTo(this)
        }
        root.render(log.views.renderContext)
        assertEquals("a", rlog, "First time, rendering of the descendants should happen")
        root.render(log.views.renderContext)
        assertEquals("a", rlog, "No re-rendering should happen")
        view.x = 10.0
        root.render(log.views.renderContext)
        assertEquals("aa", rlog, "After updating a descendant, it should trigger a re-render")
        root.render(log.views.renderContext)
        assertEquals("aa", rlog, "But only once")
        cached.cache = false
        root.render(log.views.renderContext)
        assertEquals("aaa", rlog, "After disabling the cache, re-rendering should happen always")
        root.render(log.views.renderContext)
        assertEquals("aaaa", rlog, "Always")
    }
}
