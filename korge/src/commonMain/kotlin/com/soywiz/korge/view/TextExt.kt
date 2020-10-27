package com.soywiz.korge.view

interface IText {
    var text: String
}

interface IHtml {
    var html: String
}

fun View?.setText(text: String) { this.foreachDescendant { if (it is IText) it.text = text } }
fun View?.setHtml(html: String) { this.foreachDescendant { if (it is IHtml) it.html = html } }

fun QView.setText(text: String) = fastForEach { it.setText(text) }
fun QView.setHtml(html: String) = fastForEach { it.setHtml(html) }
