package com.soywiz.korge.view

fun View.dump(emit: (String) -> Unit = ::println) = this.views.dumpView(this, emit)
fun View.dumpToString(): String {
    val out = arrayListOf<String>()
    dump { out += it }
    return out.joinToString("\n")
}
