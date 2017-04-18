package com.soywiz.korge.component.docking

import com.soywiz.korge.component.Component
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.View

class SortedChildrenByComponent(val container: Container, var comparator: Comparator<View>) : Component(container) {
	override fun update(dtMs: Int) {
		super.update(dtMs)
		container.children.sortWith(comparator)
	}
}

fun <T : Container> T.keepChildrenSortedBy(comparator: Comparator<View>) = this.apply { SortedChildrenByComponent(this, comparator).attach() }
fun <T : Container, T2 : Comparable<T2>> T.keepChildrenSortedBy(selector: (View) -> T2) = this.keepChildrenSortedBy(kotlin.Comparator { a, b -> selector(a).compareTo(selector(b)) })
fun <T : Container> T.keepChildrenSortedByY() = this.keepChildrenSortedBy(View::y)
