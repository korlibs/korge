package com.soywiz.korge.ext.spriter

import com.soywiz.klock.*
import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.*
import com.soywiz.korge.render.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korma.*
import com.soywiz.korma.geom.*
import kotlin.math.*

class SpriterView(
	private val library: SpriterLibrary,
	private val entity: Entity,
	private var initialAnimationName1: String,
	private var initialAnimationName2: String
) : View() {
	private val player = PlayerTweener(entity).apply {
		firstPlayer.setAnimation(initialAnimationName1)
		secondPlayer.setAnimation(initialAnimationName2)
		weight = 0f

		addListener(object : Player.PlayerListener {
			override fun animationFinished(animation: Animation) {
				animationFinished(Unit)
			}

			override fun animationChanged(oldAnim: Animation, newAnim: Animation) {
			}

			override fun preProcess(player: Player) {
			}

			override fun postProcess(player: Player) {
			}

			override fun mainlineKeyChanged(prevKey: Mainline.Key?, newKey: Mainline.Key?) {
				//TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
			}

		})
	}

	val animationFinished = Signal<Unit>()

	init {
		addUpdatable { updateInternal(it) }
	}

	var animationWeight: Double; get () = player.weight.toDouble(); set(value) = run { player.weight = value.toFloat() }

	var animation1: String
		get() = player.firstPlayer._animation.name
		set(value) {
			player.firstPlayer.setAnimation(value)
		}

	var animation2: String
		get() = player.secondPlayer._animation.name
		set(value) {
			player.secondPlayer.setAnimation(value)
		}

	var animation: String
		get() = prominentAnimation
		set(value) {
			animation1 = value
			animation2 = value
			animationWeight = 0.0
		}

	val prominentAnimation: String get() = if (animationWeight <= 0.5) animation1 else animation2

	var time: Int; get() = player._time; set(value) = run { player._time = value }

	suspend fun changeTo(animation: String, time: TimeSpan, easing: Easing = Easings.LINEAR) {
		animation1 = prominentAnimation
		animation2 = animation
		animationWeight = 0.0
		tween(this::animationWeight[1.0], time = time, easing = easing)
	}

	suspend fun waitCompleted() {
		animationFinished.waitOne()
	}

	private fun updateInternal(dtMs: Int) {
		player.speed = dtMs
		player.firstPlayer.speed = dtMs
		player.secondPlayer.speed = dtMs
		//println("${player.time}: $dtMs")
		player.update()
	}

	private val t1: Matrix2d = Matrix2d()
	private val t2: Matrix2d = Matrix2d()

	override fun renderInternal(ctx: RenderContext) {
		if (!visible) return
		val batch = ctx.batch
		val colorMulInt = renderColorMulInt
		val colorAdd = renderColorAdd
		for (obj in player.objectIterator()) {
			val file = library.data.getFile(obj.ref)
			val ttex = library.atlas[file.name] ?: transformedDummyTexture
			val trimLeft = ttex.trimLeft.toDouble()
			val trimTop = ttex.trimTop.toDouble()
			val tex = ttex.texture

			t1.setTransform(
				(obj.position.x - 0.0), (obj.position.y - 0.0),
				obj.scale.x.toDouble(), -obj.scale.y.toDouble(),
				-Angle.toRadians(obj._angle.toDouble()),
				0.0, 0.0
			)
			t2.copyFrom(globalMatrix)
			t2.prescale(1.0, -1.0)
			t2.premultiply(t1)
			//t2.translate(+trimLeft, +trimTop)
			if (ttex.rotated) {
				t2.prerotate(-PI / 2.0)
			}
			val px = obj.pivot.x.toDouble() * tex.width //- trimLeft
			val py = (1.0 - obj.pivot.y) * tex.height //- trimTop
			//if (ttex.rotated) {
			//	batch.addQuad(tex, -px.toFloat(), -py.toFloat(), tex.height.toFloat(), tex.width.toFloat(), t2, rotated = true)
			//} else {
			batch.drawQuad(
				ctx.getTex(tex),
				-px.toFloat(),
				-py.toFloat(),
				tex.width.toFloat(),
				tex.height.toFloat(),
				t2,
				colorMulInt = colorMulInt,
				colorAdd = colorAdd,
				rotated = false
			)
			//}
		}
	}

	companion object {
		val transformedDummyTexture by lazy { TransformedTexture(Bitmaps.transparent) }
	}
}
