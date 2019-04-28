package com.soywiz.korge.view

import com.soywiz.korge.internal.fastForEach
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
