package com.soywiz.korge.ui.korui

import com.soywiz.korge.ui.UIFactory
import com.soywiz.korge.ui.UISkin
import com.soywiz.korge.ui.Widget
import com.soywiz.korge.view.View
import com.soywiz.korui.Application
import com.soywiz.korui.frame
import com.soywiz.korui.ui.Frame

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
