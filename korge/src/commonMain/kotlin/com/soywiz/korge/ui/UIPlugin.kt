package com.soywiz.korge.ui

import com.soywiz.korge.view.*

@Deprecated("Do not use")
fun Views.registerUIFactory() {
	views.injector.mapSingleton { UIFactory() }
}
