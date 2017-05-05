package com.soywiz.korge.view

fun View?.dump(emit: (String) -> Unit = ::println) {
	if (this != null) this.views.dumpView(this, emit)
}
fun View?.dumpToString(): String {
	if (this == null) return ""
	val out = arrayListOf<String>()
	dump { out += it }
	return out.joinToString("\n")
}

fun View?.foreachDescendant(handler: (View) -> Unit) {
	if (this != null) {
		handler(this)
		if (this is Container) {
			for (child in this.children) {
				child.foreachDescendant(handler)
			}
		}
	}
}

fun View?.descendantsWithProp(prop: String, value: String? = null): List<View> {
	if (this == null) return listOf()
	return this.descendantsWith {
		if (value != null) {
			it.props[prop] == value
		} else {
			prop in it.props
		}
	}
}

fun View?.descendantsWithPropString(prop: String, value: String? = null): List<Pair<View, String>> = this.descendantsWithProp(prop, value).map { it to it.getPropString(prop) }
fun View?.descendantsWithPropInt(prop: String, value: Int? = null): List<Pair<View, Int>> = this.descendantsWithProp(prop, "$value").map { it to it.getPropInt(prop) }

operator fun View?.get(name: String): View? = firstDescendantWith { it.name == name }

@Deprecated("", ReplaceWith("this[name]", "com.soywiz.korge.view.get"))
fun View?.firstDescendantWithName(name: String): View? =  this[name]

val View?.allDescendantNames get(): List<String> {
	val out = arrayListOf<String>()
	foreachDescendant {
		if (it.name != null) out += it.name!!
	}
	return out
}

fun View?.firstDescendantWith(check: (View) -> Boolean): View? {
	if (this == null) return null
	if (check(this)) return this
	if (this is Container) {
		for (child in this.children) {
			val res = child.firstDescendantWith(check)
			if (res != null) return res
		}
	}
	return null
}

fun View?.descendantsWith(out: ArrayList<View> = arrayListOf(), check: (View) -> Boolean): List<View> {
	if (this != null) {
		if (check(this)) out += this
		if (this is Container) {
			for (child in this.children) {
				child.descendantsWith(out, check)
			}
		}
	}
	return out
}
