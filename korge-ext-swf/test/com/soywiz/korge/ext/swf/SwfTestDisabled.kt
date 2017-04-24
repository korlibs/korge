package com.soywiz.korge.ext.swf

import com.soywiz.korge.Korge
import com.soywiz.korge.animate.AnMovieClip
import com.soywiz.korge.animate.serialization.AnLibrarySerializer
import com.soywiz.korge.animate.serialization.writeTo
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korim.vector.Context2d
import com.soywiz.korio.vfs.LocalVfs

class SwfTestDisabled {
	companion object {
		@JvmStatic fun main(args: Array<String>) = Korge(Module(), sceneClass = MyScene::class.java)
	}

	class MyScene : Scene() {
		suspend override fun sceneInit(sceneView: Container) {
			//val library = LocalVfs("c:/temp/test1.swf").readSWF(views)
			//val library = LocalVfs("c:/temp/test2.swf").readSWF(views, mipmaps = true)
			//val library = LocalVfs("c:/temp/test9.swf").readSWF(views, mipmaps = true)
			//val library = LocalVfs("c:/temp/test6.swf").readSWF(views, mipmaps = true)
			//val library = LocalVfs("c:/temp/test6.swf").readSWF(views, mipmaps = true, rasterizerMethod = Context2d.ShapeRasterizerMethod.X4)
			//val library = LocalVfs("c:/temp/test6.swf").readSWF(views, mipmaps = false, rasterizerMethod = Context2d.ShapeRasterizerMethod.NONE)
			//val library = LocalVfs("c:/temp/test2.swf").readSWF(views)
			//val library = LocalVfs("c:/temp/test5.swf").readSWF(views)
			//val library = LocalVfs("c:/temp/test6.swf").readSWF(views)

			//val library = LocalVfs("c:/temp/test9.swf").readSWF(views, mipmaps = true, rasterizerMethod = Context2d.ShapeRasterizerMethod.X4)
			//val library = LocalVfs("c:/temp/test8.swf").readSWF(views, mipmaps = true, rasterizerMethod = Context2d.ShapeRasterizerMethod.X4)
			val library = LocalVfs("c:/temp/test7.swf").readSWF(views, mipmaps = true, rasterizerMethod = Context2d.ShapeRasterizerMethod.X4)

			//val library = LocalVfs("c:/temp/test10.swf").readSWF(views)
			//val library = LocalVfs("c:/temp/test10.swf").readSWF(views).apply { writeTo(LocalVfs("c:/temp/test10.ani")) }
			//val library = LocalVfs("c:/temp/test11.swf").readSWF(views)
			//val library = LocalVfs("c:/temp/test12.swf").readSWF(views)

			//val library = LocalVfs("c:/temp/test14.swf").readSWF(views)

			//val library = LocalVfs("c:/temp/test17.swf").readSWF(views)
			//val library = LocalVfs("c:/temp/test18.swf").readSWF(views)
			//val library = LocalVfs("c:/temp/test19.swf").readSWF(views)
			//val library = LocalVfs("c:/temp/test20.swf").readSWF(views)
			//val library = LocalVfs("c:/temp/test21.swf").readSWF(views)
			//val library = LocalVfs("c:/temp/test22.swf").readSWF(views)
			//val library = LocalVfs("c:/temp/test23.swf").readSWF(views)
			//val library = LocalVfs("c:/temp/test24.swf").readSWF(views)
			//val library = LocalVfs("c:/temp/test25.swf").readSWF(views)
			//val library = LocalVfs("c:/temp/test26.swf").readSWF(views)
			//val library = LocalVfs("c:/temp/test27.swf").readSWF(views)
			//val library = LocalVfs("c:/temp/test28.swf").readSWF(views)
			//val library = LocalVfs("c:/temp/test29.swf").readSWF(views)
			//val library = LocalVfs("c:/temp/test29.swf").readSWF(views).apply { writeTo(LocalVfs("c:/temp/test29.ani")) }
			//val library = LocalVfs("c:/temp/test30.swf").readSWF(views)
			//val library = LocalVfs("c:/temp/test31.swf").readSWF(views)
			//val library = LocalVfs("c:/temp/test1.swf").readSWF(views)
			//val library = LocalVfs("c:/temp/test6.swf").readSWF(views)


			//val library = LocalVfs("c:/temp/test7.swf").readSWF(views)
			//val library = LocalVfs("c:/temp/test1.swf").readSWF(views)
			//library.writeTo(LocalVfs("c:/temp/test6.ani"))
			//library.writeTo(LocalVfs("c:/temp/test2.ani"))
			val ani = library.createMainTimeLine() as AnMovieClip
			//println(ani.stateNames)
			//ani.play("frame172")
			sceneView += ani
		}
	}
}
