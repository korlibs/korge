package com.soywiz.korge.sample

import com.soywiz.korge.Korge
import com.soywiz.korge.render.Texture
import com.soywiz.korge.resources.Path
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.ui.UIFactory
import com.soywiz.korge.ui.korui.koruiFrame
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.dump
import com.soywiz.korim.color.RGBA
import com.soywiz.korui.geom.len.Padding
import com.soywiz.korui.geom.len.pt
import com.soywiz.korui.style.padding
import com.soywiz.korui.ui.button
import com.soywiz.korui.ui.horizontal
import com.soywiz.korui.ui.label
import com.soywiz.korui.ui.onMouseClick

object SampleKorui : Module() {
	@JvmStatic fun main(args: Array<String>) = Korge(this@SampleKorui)

	override val width: Int = 560
	override val height: Int = 380

	//override val bgcolor: Int = Colors.WHITE
	override val bgcolor: Int = RGBA(0x70, 0x70, 0x70, 0xFF)
	override val mainScene: Class<out Scene> = MainScene::class.java

	class MainScene(
		@Path("tiles.png") val tiles: Texture,
		val ui: UIFactory
	) : Scene() {
		suspend override fun sceneInit(sceneView: Container) {
			sceneView += ui.koruiFrame {
				horizontal {
					padding = Padding(5.pt)
					label("HELLO")
					button("HELLO").onMouseClick {
						println("HELLO")
					}
					button("WORLD!").onMouseClick {
						println("WORLD!")
					}
				}
			}
			sceneView.dump()
		}
	}
}
