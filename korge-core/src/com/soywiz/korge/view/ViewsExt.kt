package com.soywiz.korge.view

fun View.dump(emit: (String) -> Unit = ::println) = this.views.dumpView(this, emit)
fun View.dumpToString(): String {
	val out = arrayListOf<String>()
	dump { out += it }
	return out.joinToString("\n")
}

fun View.descendants(handler: (View) -> Unit) {
	handler(this)
	if (this is Container) {
		for (child in this.children) {
			child.descendants(handler)
		}
	}
}

operator fun View.get(name: String): View? = findFirstDescendant { it.name == name }
fun View.descendant(name: String): View? = findFirstDescendant { it.name == name }
fun View.findFirstWithName(name: String): View? = findFirstDescendant { it.name == name }

fun View.findFirstDescendant(check: (View) -> Boolean): View? {
	if (check(this)) return this
	if (this is Container) {
		for (child in this.children) {
			val res = child.findFirstDescendant(check)
			if (res != null) return res
		}
	}
	return null
}
