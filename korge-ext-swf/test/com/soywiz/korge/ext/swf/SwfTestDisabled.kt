package com.soywiz.korge.ext.swf

import com.soywiz.korge.Korge
import com.soywiz.korge.animate.AnLibrary
import com.soywiz.korge.animate.serialization.readAni
import com.soywiz.korge.animate.serialization.writeTo
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Views
import com.soywiz.korio.vfs.LocalVfs
import com.soywiz.korio.vfs.VfsFile

class SwfTestDisabled {
	companion object {
		@JvmStatic fun main(args: Array<String>) = Korge(object : Module() {
			override val width: Int = (550 * 1.5).toInt()
			override val height: Int = (400 * 1.5).toInt()
			override val virtualWidth: Int = 550
			override val virtualHeight: Int = 400
		}, sceneClass = MyScene::class.java)

		suspend fun VfsFile.readSWFDeserializing(views: Views, config: SWFExportConfig? = null): AnLibrary {
			val ani = this.readSWF(views, config)
			val aniFile = this.withExtension("ani")
			ani.writeTo(aniFile, ani.swfExportConfig.toAnLibrarySerializerConfig(compression = 0.3))
			//ani.writeTo(aniFile, ani.swfExportConfig.toAnLibrarySerializerConfig(compression = 0.0))
			println("ANI size:" + aniFile.size())
			return aniFile.readAni(views)
		}
	}

	class MyScene : Scene() {
		suspend override fun sceneInit(sceneView: Container) {
			//sleep(6000)
			//sceneView += LocalVfs("c:/temp/test3.swf").readSWF(views).createMainTimeLine()
			sceneView += LocalVfs("c:/temp/test29.swf").readSWF(views).createMainTimeLine()
			//sceneView += LocalVfs("c:/temp/test27.swf").readSWF(views).createMainTimeLine()
			//sceneView += LocalVfs("c:/temp/test6.swf").readSWF(views).createMainTimeLine()
			//sceneView += LocalVfs("c:/temp/tt6.swf").readSWF(views).createMainTimeLine()
			//sceneView += LocalVfs("c:/temp/tt7.swf").readSWF(views).createMainTimeLine().apply { play("outofdate") }
			//sceneView += LocalVfs("c:/temp/test24.swf").readSWF(views).createMainTimeLine().apply { println(this.stateNames) }
			//sceneView += LocalVfs("c:/temp/test23.swf").readSWFDeserializing(views).createMainTimeLine().apply { println(this.stateNames) }
			//sceneView += LocalVfs("c:/temp/test23.swf").readSWF(views).createMainTimeLine().apply { println(this.stateNames) }
			//sceneView += LocalVfs("c:/temp/test2.swf").readSWF(views).createMainTimeLine().apply { println(this.stateNames) }
		}
	}
}
