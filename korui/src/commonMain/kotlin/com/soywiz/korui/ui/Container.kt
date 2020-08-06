package com.soywiz.korui.ui

import com.soywiz.korui.factory.*

open class Container(application: Application, component: NativeUiComponent = application.factory.createContainer()) : Component(application, component) {
    fun addChild(component: Component) = this.apply {
        component.parent = this
    }
}

fun Container.container(block: Container.() -> Unit): Container {
    return Container(app)
        .also { addChild(it) }
        .also(block)
}
