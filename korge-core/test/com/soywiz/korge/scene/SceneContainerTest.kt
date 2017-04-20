package com.soywiz.korge.scene

import com.soywiz.korge.ViewsForTesting
import com.soywiz.korge.log.Logger
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.get
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.syncTest
import org.junit.Assert
import org.junit.Test

class SceneContainerTest : ViewsForTesting() {
	class Scene1 : LogScene() {
		override suspend fun sceneAfterInit() {
			super.sceneAfterInit()
			sceneContainer.changeTo<Scene2>()
		}
	}

	class Scene2 : LogScene() {
		suspend override fun sceneInit(sceneView: Container) {
			super.sceneInit(sceneView)
			sceneView += views.solidRect(100, 100, Colors.RED).apply {
				name = "box"
			}
		}
	}

	@Test
	fun name() = syncTest {
		val out = arrayListOf<String>()
		injector.map<Logger>(Logger { msg -> out += msg })


		val sc = SceneContainer(views)
		sc.changeTo<Scene1>()

		Assert.assertNotNull(sc["box"])
		Assert.assertEquals(
			"Scene1.sceneInit, Scene1.sceneAfterDestroy, Scene1.sceneAfterInit, Scene2.sceneInit, Scene1.sceneDestroy, Scene2.sceneAfterDestroy, Scene2.sceneAfterInit",
			out.joinToString(", ")
		)

	}
}
