package com.soywiz.korge.box2d

import com.soywiz.korge.view.*
import org.jbox2d.collision.shapes.*
import org.jbox2d.common.*
import org.jbox2d.dynamics.*
import org.jbox2d.userdata.*

inline fun bodyDef(callback: BodyDef.() -> Unit): BodyDef = BodyDef().apply(callback)
inline fun World.createBody(callback: BodyDef.() -> Unit): Body = createBody(bodyDef(callback))
inline fun Body.fixture(callback: FixtureDef.() -> Unit): Body = this.also { createFixture(FixtureDef().apply(callback)) }
inline fun Body.setViewWithContainer(view: View): Body {
	val container = Container()
	container.addChild(view)
	return this.setView(container)
}
inline fun Body.setView(view: View): Body {
	this.view = view
	return this
}
inline fun BoxShape(width: Number, height: Number) = PolygonShape().apply { setAsBox(width.toFloat() / 2, height.toFloat() / 2) }
inline fun BodyDef.setPosition(x: Number, y: Number) = position.set(x.toFloat(), y.toFloat())
inline fun WorldView.createBody(callback: BodyDef.() -> Unit): Body = world.createBody(callback)

inline fun Container.worldView(world: World = World(Vec2(0f, -10f)), callback: WorldView.() -> Unit = {}): WorldView = WorldView().addTo(this).apply(callback)

val World.worldView get() = this[WorldView.WorldViewKey]
var Body.view: View?
	set(view) = run { this[WorldView.ViewKey] = view!! }
	get() = this[WorldView.ViewKey]

class WorldView(val world: World = World(Vec2(0f, -10f))) : Container() {
	companion object {
	    val WorldViewKey = Box2dTypedUserData.Key<WorldView>()
		val ViewKey = Box2dTypedUserData.Key<View>()
	}

	init {
		world[WorldViewKey] = this
		addUpdatable {
			world.step(it.toFloat() / 1000f, velocityIterations = 6, positionIterations = 2)
			updateViews()
		}
	}

	fun updateViews() {
		var node = world.bodyList
		while (node != null) {
			val view = node.view
			if (view != null) {
				if (view.parent != this) {
					this.addChild(view)
				}
				view.x = node.position.x.toDouble()
				view.y = -node.position.y.toDouble()
				view.rotationRadians = -node.angle.toDouble()
			}
			node = node.m_next
		}
	}
}
