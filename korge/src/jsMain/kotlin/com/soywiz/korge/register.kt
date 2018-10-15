package com.soywiz.korge

import com.soywiz.korge.view.*
import kotlin.browser.*

actual fun Korge.register(views: Views) {
	(window.asDynamic()).views = views
}