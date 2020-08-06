package com.soywiz.korui.ui

import com.soywiz.korui.factory.*

open class Container(application: Application, component: NativeUiComponent = application.factory.createContainer()) : Component(application, component) {
    fun addChild(component: Component) = this.apply {
        component.parent = this
    }
}
