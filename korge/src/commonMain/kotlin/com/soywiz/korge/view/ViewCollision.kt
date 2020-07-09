package com.soywiz.korge.view

import com.soywiz.kds.iterators.*
import com.soywiz.korio.lang.Cancellable
import com.soywiz.korio.lang.threadLocal
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

@PublishedApi
internal class ViewCollisionContext {
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

    fun getGlobalMatrix(view: View, out: Matrix): Matrix {
        out.copyFrom(view.localMatrix)
        out.pretranslate(-view.anchorDispX, -view.anchorDispY)
        out.multiply(out, view.parent?.globalMatrix ?: ident)
        //return view.globalMatrix
        return out
    }

    fun collidesWith(left: View, right: View, kind: CollisionKind): Boolean {
        left.getGlobalBounds(tempRect1)
        right.getGlobalBounds(tempRect2)
        if (!tempRect1.intersects(tempRect2)) return false
        if (kind == CollisionKind.SHAPE) {
            val leftPaths = getVectorPath(left, tempVectorPath1)
            val rightPaths = getVectorPath(right, tempVectorPath2)
            leftPaths.fastForEach { leftPath ->
                rightPaths.fastForEach { rightPath ->
                    if  (VectorPath.intersects(leftPath, getGlobalMatrix(left, lmat), rightPath, getGlobalMatrix(right, rmat))) return true
                }
            }
            return false
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

fun Container.findCollision(subject: View, kind: CollisionKind = CollisionKind.GLOBAL_RECT,matcher: (View) -> Boolean): View? {
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
