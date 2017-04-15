package com.soywiz.korge.ext.spriter

import com.brashmonkey.spriter.*
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.tween.Easing
import com.soywiz.korge.tween.Easings
import com.soywiz.korge.tween.rangeTo
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korio.async.Signal
import com.soywiz.korio.async.waitOne
import com.soywiz.korma.Matrix2d

class SpriterView(views: Views, private val library: SpriterLibrary, private val entity: Entity, private var initialAnimationName1: String, private var initialAnimationName2: String) : View(views) {
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
		updateInternal(0)
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

	suspend fun changeTo(animation: String, time: Int, easing: Easing = Easings.LINEAR) {
		animation1 = prominentAnimation
		animation2 = animation
		animationWeight = 0.0
		tween(SpriterView::animationWeight..1.0, time = time, easing = easing)
	}

	suspend fun waitCompleted() {
		animationFinished.waitOne()
	}

	override fun updateInternal(dtMs: Int) {
		super.updateInternal(dtMs)
		player.speed = dtMs
		player.firstPlayer.speed = dtMs
		player.secondPlayer.speed = dtMs
		//println("${player.time}: $dtMs")
		player.update()
	}

	private val t1: Matrix2d = Matrix2d()
	private val t2: Matrix2d = Matrix2d()

	override fun render(ctx: RenderContext) {
		val batch = ctx.batch
		for (obj in player.objectIterator()) {
			val file = library.data.getFile(obj.ref)
			val tex = library.atlas[file.name] ?: views.dummyTexture

			t1.setTransform(
				obj.position.x.toDouble(), obj.position.y.toDouble(),
				obj.scale.x.toDouble(), -obj.scale.y.toDouble(),
				-Math.toRadians(obj._angle.toDouble()),
				0.0, 0.0
			)
			//t2.setToIdentity()
			t2.copyFrom(globalMatrix)
			//t2.premulitply(globalMatrix)
			t2.prescale(1.0, -1.0)
			t2.premulitply(t1)
			val px = obj.pivot.x.toDouble() * tex.width
			val py = (1.0 - obj.pivot.y) * tex.height
			//file
			//println("$sw, $sh")
			//println("${file.pivot} : ${obj.pivot}")
			batch.addQuad(tex, -px.toFloat(), -py.toFloat(), tex.width.toFloat(), tex.height.toFloat(), t2)
		}
	}

}
