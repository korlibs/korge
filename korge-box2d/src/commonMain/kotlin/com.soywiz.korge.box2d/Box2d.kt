package com.soywiz.korge.box2d

import com.soywiz.korge.view.*
import org.jbox2d.collision.shapes.*
import org.jbox2d.common.*
import org.jbox2d.dynamics.*
import kotlin.contracts.*

inline fun bodyDef(callback: BodyDef.() -> Unit): BodyDef = BodyDef().apply(callback)
inline fun World.createBody(callback: BodyDef.() -> Unit): Body = createBody(bodyDef(callback))
inline fun Body.fixture(callback: FixtureDef.() -> Unit): Body = this.also { createFixture(FixtureDef().apply(callback)) }
inline fun Body.setView(view: View): Body = this.also { userData = view }
inline fun BoxShape(width: Number, height: Number) = PolygonShape().apply { setAsBox(width.toFloat() / 2, height.toFloat() / 2) }
inline fun BodyDef.setPosition(x: Number, y: Number) = position.set(x.toFloat(), y.toFloat())
inline fun WorldView.createBody(callback: BodyDef.() -> Unit): Body = world.createBody(callback)

inline fun Container.worldView(world: World = World(Vec2(0f, -10f)), callback: WorldView.() -> Unit = {}): WorldView = WorldView().addTo(this).apply(callback)

class WorldView(val world: World = World(Vec2(0f, -10f))) : Container() {
	init {
		addUpdatable {
			world.step(it.toFloat() / 1000f, velocityIterations = 6, positionIterations = 2)
			updateViews()
		}
	}

	fun updateViews() {
		var node = world.bodyList
		while (node != null) {
			val userData = node.userData
			if (userData is View) {
				userData.x = node.position.x.toDouble()
				userData.y = -node.position.y.toDouble()
				userData.rotationRadians = -node.angle.toDouble()
			}
			node = node.m_next
		}
	}
}
