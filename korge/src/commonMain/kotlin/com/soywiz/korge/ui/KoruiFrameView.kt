package com.soywiz.korge.ui

import com.soywiz.korge.view.*
import com.soywiz.korui.*
import com.soywiz.korui.ui.*

class KoruiFrameView(factory: UIFactory, skin: UISkin = factory.skin) : Widget(factory) {
	lateinit var frame: Frame
	lateinit var application: Application
}

suspend fun UIFactory.koruiFrame(builder: suspend Frame.() -> Unit = {}): KoruiFrameView {
	val applicationView = KoruiFrameView(this)
	applicationView.application = Application(KorgeLightComponents(this))
	applicationView.frame = applicationView.application.frame("Main", views.virtualWidth, views.virtualHeight)
	applicationView += applicationView.frame.handle as View
	builder(applicationView.frame)
	return applicationView
}
