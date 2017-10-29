package com.soywiz.korge.sample

import com.soywiz.korge.Korge
import com.soywiz.korge.input.Input
import com.soywiz.korge.plugin.KorgePlugins
import com.soywiz.korge.plugin.defaultKorgePlugins
import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korge.scene.EmptyScene
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Views
import com.soywiz.korim.color.Colors
import com.soywiz.korio.inject.AsyncInjector

object Sample7 {
	@JvmStatic
	fun main(args: Array<String>) {
		val injector = AsyncInjector()
		injector
			.mapSingleton { Views(get(), get(), get(), get(), get()) }
			.mapSingleton { Input() }
			.mapInstance<KorgePlugins>(defaultKorgePlugins)
			.mapPrototype { EmptyScene() }
			.mapSingleton { ResourcesRoot() }
			.mapPrototype { MyScene() }

		Korge(MyModule, injector = injector)
	}

	object MyModule : Module() {
		override val mainScene = MyScene::class
	}

	class MyScene : Scene() {
		suspend override fun sceneInit(sceneView: Container) {
			println("MyScene.sceneInit")
			sceneView += views.solidRect(200, 200, Colors.RED)
		}
	}
}
