package com.soywiz.korge.ext.swf

import com.soywiz.korge.Korge
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korio.vfs.LocalVfs

class SwfTestDisabled {
	class MyScene : Scene() {
		suspend override fun sceneInit(sceneView: Container) {
			val library = LocalVfs("c:/temp/test1.swf").readSWF(views)
			sceneView += library.createMainTimeLine()
		}
	}

	companion object {
		@JvmStatic fun main(args: Array<String>) = Korge(Module(), sceneClass = MyScene::class.java)
	}
}
