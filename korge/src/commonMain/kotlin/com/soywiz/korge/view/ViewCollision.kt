package com.soywiz.korge.view

import com.soywiz.kds.iterators.*
import com.soywiz.korio.lang.Cancellable
import com.soywiz.korio.lang.threadLocal
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.vector.*

@PublishedApi
internal class ViewCollisionContext {
    val tempMat = Matrix()
    val tempRect1 = Rectangle()
    val tempRect2 = Rectangle()
    val tempVectorPath1 = listOf(VectorPath())
    val tempVectorPath2 = listOf(VectorPath())

    val ident = Matrix()
    val lmat = Matrix()
    val rmat = Matrix()

    fun getVectorPath(view: View, out: List<VectorPath>): List<VectorPath> {
        val hitShape = view.hitShape
        val hitShapes = view.hitShapes
        if (hitShapes != null) return hitShapes
        if (hitShape != null) return listOf(hitShape)
        view.getLocalBounds(tempRect1)
        out[0].clear()
        val dispX = view.anchorDispX
        val dispY = view.anchorDispY
        out[0].rect(tempRect1.x + dispX, tempRect1.y + dispY, tempRect1.width, tempRect1.height)
        return out
    }

    fun collidesWith(left: View, right: View, kind: CollisionKind): Boolean {
        left.getGlobalBounds(tempRect1)
        right.getGlobalBounds(tempRect2)
        if (!tempRect1.intersects(tempRect2)) return false
        if (kind == CollisionKind.SHAPE) {
            val leftShape = left.hitShape2d
            val rightShape = right.hitShape2d
            val ml = left.getGlobalMatrixWithAnchor(lmat)
            val mr = right.getGlobalMatrixWithAnchor(rmat)
            //println("intersects[$result]: left=$leftShape, right=$rightShape, ml=$ml, mr=$mr")
            return Shape2d.intersects(leftShape, ml, rightShape, mr, tempMat)
        }
        return true
    }
}

private val collisionContext by threadLocal { ViewCollisionContext() }

enum class CollisionKind { GLOBAL_RECT, SHAPE }

fun View.collidesWith(other: View, kind: CollisionKind = CollisionKind.GLOBAL_RECT): Boolean {
    return collisionContext.collidesWith(this, other, kind)
}

fun View.collidesWith(otherList: List<View>, kind: CollisionKind = CollisionKind.GLOBAL_RECT): Boolean {
    val ctx = collisionContext
	otherList.fastForEach { other ->
		if (ctx.collidesWith(this, other, kind)) return true
	}
	return false
}

fun View.collidesWithGlobalBoundingBox(other: View): Boolean = collidesWith(other, CollisionKind.GLOBAL_RECT)
fun View.collidesWithGlobalBoundingBox(otherList: List<View>): Boolean = collidesWith(otherList, CollisionKind.GLOBAL_RECT)

fun View.collidesWithShape(other: View): Boolean = collidesWith(other, CollisionKind.SHAPE)
fun View.collidesWithShape(otherList: List<View>): Boolean = collidesWith(otherList, CollisionKind.SHAPE)

inline fun <reified T : View> Container.findCollision(subject: View): T? = findCollision(subject) { it is T && it != subject } as T?

fun Container.findCollision(subject: View, kind: CollisionKind = CollisionKind.GLOBAL_RECT, matcher: (View) -> Boolean): View? {
    var collides: View? = null
    this.foreachDescendant {
        if (matcher(it)) {
            if (subject.collidesWith(it, kind)) {
                collides = it
            }
        }
    }
    return collides
}

/**
 * Adds a component to [this] that checks each frame for collisions against descendant views of [root] that matches the [filter],
 * when a collision is found [callback] is executed with this view as receiver, and the collision target as first parameter.
 * if no [root] is provided, it computes the root of the view [this] each frame, you can specify a collision [kind]
 */
fun View.onCollision(filter: (View) -> Boolean = { true }, root: View? = null, kind: CollisionKind = CollisionKind.GLOBAL_RECT, callback: View.(View) -> Unit): Cancellable {
	return addUpdater {
		(root ?: this.root).foreachDescendant {
			if (this != it && filter(it) && this.collidesWith(it, kind)) {
				callback(this, it)
			}
		}
	}
}

fun List<View>.onCollision(filter: (View) -> Boolean = { true }, root: View? = null, kind: CollisionKind = CollisionKind.GLOBAL_RECT, callback: View.(View) -> Unit): Cancellable =
    Cancellable(this.map { it.onCollision(filter, root, kind, callback) })

fun View.onCollisionShape(filter: (View) -> Boolean = { true }, root: View? = null, callback: View.(View) -> Unit): Cancellable {
    return onCollision(filter, root, kind = CollisionKind.SHAPE, callback = callback)
}

/**
 * Adds a component to [this] that checks collisions each frame of descendants views of [root] matching [filterSrc], matching against descendant views matching [filterDst].
 * When a collision is found, [callback] is called. It returns a [Cancellable] to remove the component.
 */
fun View.onDescendantCollision(root: View = this, filterSrc: (View) -> Boolean = { true }, filterDst: (View) -> Boolean = { true }, kind: CollisionKind = CollisionKind.GLOBAL_RECT, callback: View.(View) -> Unit): Cancellable {
	return addUpdater {
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

/**
 * Adds a component to [this] that checks each frame for collisions against descendant views of [root] that matches the [filter],
 * when a collision is found, it remembers it and the next time the collision does not occur, [callback] is executed with this view as receiver, 
 * and the collision target as first parameter. if no [root] is provided, it computes the root of the view [this] each frame, you can specify a collision [kind]
 */
fun View.onCollisionExit(
    filter: (View) -> Boolean = { true },
    root: View? = null,
    kind: CollisionKind = CollisionKind.GLOBAL_RECT,
    callback: View.(View) -> Unit
): Cancellable {
    val collisionState = mutableMapOf<View, Boolean>()
    return addUpdater {
        (root ?: this.root).foreachDescendant {
            if (this != it && filter(it)) {
                if (this.collidesWith(it, kind)) {
                    collisionState[it] = true
                } else if (collisionState[it] == true) {
                    callback(this, it)
                    collisionState[it] = false
                }
            }
        }
    }
}

fun View.onCollisionShapeExit(filter: (View) -> Boolean = { true }, root: View? = null, callback: View.(View) -> Unit): Cancellable {
    return onCollisionExit(filter, root, kind = CollisionKind.SHAPE, callback = callback)
}

fun List<View>.onCollisionExit(filter: (View) -> Boolean = { true }, root: View? = null, kind: CollisionKind = CollisionKind.GLOBAL_RECT, callback: View.(View) -> Unit): Cancellable =
    Cancellable(this.map { it.onCollisionExit(filter, root, kind, callback) })

/**
 * Adds a component to [this] that checks collisions each frame of descendants views of [root] matching [filterSrc], matching against descendant views matching [filterDst].
 * When a collision is found, it remembers it and the next time the collision does not occur, [callback] is called. It returns a [Cancellable] to remove the component.
 */
fun View.onDescendantCollisionExit(root: View = this, filterSrc: (View) -> Boolean = { true }, filterDst: (View) -> Boolean = { true }, kind: CollisionKind = CollisionKind.GLOBAL_RECT, callback: View.(View) -> Unit): Cancellable {
    val collisionState = mutableMapOf<Pair<View, View>, Boolean>()
    return addUpdater {
        root.foreachDescendant { src ->
            if (filterSrc(src)) {
                root.foreachDescendant { dst ->
                    if (src !== dst && filterDst(dst)) {
                        if (src.collidesWith(dst, kind)) {
                            println("collide")
                            collisionState[Pair(src, dst)] = true
                        } else if (collisionState[Pair(src, dst)] == true) {
                            callback(src, dst)
                            collisionState[Pair(src, dst)] = false
                        }
                    }
                }
            }
        }
    }
}
