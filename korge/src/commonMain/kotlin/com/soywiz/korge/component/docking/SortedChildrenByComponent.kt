package com.soywiz.korge.component.docking

import com.soywiz.klock.hr.*
import com.soywiz.korge.component.*
import com.soywiz.korge.view.*

class SortedChildrenByComponent(override val view: Container, var comparator: Comparator<View>) : UpdateComponentV2 {
    override fun update(dt: HRTimeSpan) {
        view.sortChildrenBy(comparator)
    }
}

fun <T, T2 : Comparable<T2>> ((T) -> T2).toComparator() = Comparator { a: T, b: T -> this(a).compareTo(this(b)) }
fun <T2 : Comparable<T2>> Container.sortChildrenBy(selector: (View) -> T2) = sortChildrenBy(selector.toComparator())
fun Container.sortChildrenByY() = sortChildrenBy(View::y)

// @TODO: kotlin-native: kotlin.Comparator { }
//             korge/korge/common/src/com/soywiz/korge/component/docking/SortedChildrenByComponent.kt:25:127: error: unresolved reference: Comparator
// @TODO: kotlin-native: recursive problem!
//korge/korge/common/src/com/soywiz/korge/component/docking/SortedChildrenByComponent.kt:18:140: error: cannot infer a type for this parameter. Please specify it explicitly.
//      fun <T : Container, T2 : Comparable<T2>> T.keepChildrenSortedBy(selector: (View) -> T2): T = this.keepChildrenSortedBy(kotlin.Comparator { a, b -> selector(a).compareTo(selector(b)) })

//fun <T : Container> T.keepChildrenSortedBy(comparator: Comparator<View>) = this.apply { SortedChildrenByComponent(this, comparator).attach() }
//fun <T : Container, T2 : Comparable<T2>> T.keepChildrenSortedBy(selector: (View) -> T2) = this.keepChildrenSortedBy(kotlin.Comparator { a, b -> selector(a).compareTo(selector(b)) })

//fun <T : Container> T.keepChildrenSortedBy(comparator: Comparator<View>): T = this.apply { SortedChildrenByComponent(this, comparator).attach() }
//fun <T : Container, T2 : Comparable<T2>> T.keepChildrenSortedBy(selector: (View) -> T2): T = this.keepChildrenSortedBy(kotlin.Comparator { a, b -> selector(a).compareTo(selector(b)) })
//fun <T : Container> T.keepChildrenSortedByY(): T = this.keepChildrenSortedBy(View::y)
//fun <T : Container, T2 : Comparable<T2>> T.keepChildrenSortedBy(selector: (View) -> T2): T = this.keepChildrenSortedBy(kotlin.Comparator { a: View, b: View -> selector(a).compareTo(selector(b)) })

fun <T : Container> T.keepChildrenSortedBy(comparator: Comparator<View>): T {
    SortedChildrenByComponent(this, comparator).attach()
    return this
}

fun <T : Container, T2 : Comparable<T2>> T.keepChildrenSortedBy(selector: (View) -> T2): T =
	this.keepChildrenSortedBy(selector.toComparator())

fun <T : Container> T.keepChildrenSortedByY(): T = this.keepChildrenSortedBy(View::y)
