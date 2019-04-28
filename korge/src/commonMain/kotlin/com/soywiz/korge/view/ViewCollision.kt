package com.soywiz.korge.view

import com.soywiz.korge.internal.fastForEach
import com.soywiz.korio.lang.Cancellable
import com.soywiz.korio.lang.threadLocal
import com.soywiz.korma.geom.Rectangle

private val tempRect1 by threadLocal { Rectangle() }
private val tempRect2 by threadLocal { Rectangle() }

enum class CollisionKind { GLOBAL_RECT }

fun View.collidesWith(other: View, kind: CollisionKind = CollisionKind.GLOBAL_RECT): Boolean {
	if (kind != CollisionKind.GLOBAL_RECT) error("Unsupported $kind")
	this.getGlobalBounds(tempRect1)
	other.getGlobalBounds(tempRect2)
	return tempRect1.intersects(tempRect2)
}

fun View.collidesWith(otherList: List<View>, kind: CollisionKind = CollisionKind.GLOBAL_RECT): Boolean {
	otherList.fastForEach { other ->
		if (this.collidesWith(other, kind)) return true
	}
	return false
}

/**
 * Adds a component to [this] that checks each frame for collisions against descendant views of [root] that matches the [filter],
 * when a collision is found [callback] is executed with this view as receiver, and the collision target as first parameter.
 * if no [root] is provided, it computes the root of the view [this] each frame, you can specify a collision [kind]
 */
fun View.onCollision(filter: (View) -> Boolean = { true }, root: View? = null, kind: CollisionKind = CollisionKind.GLOBAL_RECT, callback: View.(View) -> Unit): Cancellable {
	return addUpdatable {
		(root ?: this.root).foreachDescendant {
			if (this != it && filter(it) && this.collidesWith(it, kind)) {
				callback(this, it)
			}
		}
	}
}

/**
 * Adds a component to [this] that checks collisions each frame of descendants views of [root] matching [filterSrc], matching against descendant views matching [filterDst].
 * When a collision is found, [callback] is called. It returns a [Cancellable] to remove the component.
 */
fun View.onDescendantCollision(root: View = this, filterSrc: (View) -> Boolean = { true }, filterDst: (View) -> Boolean = { true }, kind: CollisionKind = CollisionKind.GLOBAL_RECT, callback: View.(View) -> Unit): Cancellable {
	return addUpdatable {
		root.foreachDescendant { src ->
			if (filterSrc(src)) {
				root.foreachDescendant { dst ->
					if (src !== dst && filterDst(dst) && src.collidesWith(dst, kind)) {
						callback(src, dst)
					}
				}
			}
		}
	}
}
