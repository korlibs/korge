package com.soywiz.korge

import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korge.view.*
import com.soywiz.korim.atlas.readAtlas
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korio.file.std.resourcesVfs
import kotlin.test.Test
import kotlin.test.assertTrue

class ViewsForTestingTest : ViewsForTesting() {
    object DummyModule : Module() {
        override suspend fun AsyncInjector.configure() {
            mapPrototype {
                DummyScene()
            }
        }
    }

    class DummyScene : Scene() {
        var running = false

        override suspend fun Container.sceneInit() {
            running = true
        }
    }

    @Test
    fun sceneTestRunsScene() = sceneTest<DummyScene>(DummyModule) {
        assertTrue(running)
    }
}
