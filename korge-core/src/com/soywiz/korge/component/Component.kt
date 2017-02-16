package com.soywiz.korge.component

import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views

open class Component(val view: View) {
    val views: Views get() = view.views

    open fun update(dtMs: Int): Unit = Unit
}