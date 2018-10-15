package com.soywiz.korge.ui

import com.soywiz.korge.view.*

fun Views.registerUIFactory() {
	views.injector.mapSingleton { UIFactory() }
}