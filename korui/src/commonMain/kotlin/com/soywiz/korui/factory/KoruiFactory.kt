package com.soywiz.korui.factory

expect val defaultKoruiFactory: KoruiFactory

open class KoruiFactory {
    open fun createWindow(): NativeUiComponent = NativeUiComponent(null)
    open fun createContainer(): NativeUiComponent = NativeUiComponent(null)
    open fun createButton(): NativeUiComponent = NativeUiComponent(null)
    open fun createLabel(): NativeUiComponent = NativeUiComponent(null)

    open fun setParent(c: NativeUiComponent, p: NativeUiComponent?) = Unit
    open fun setChildIndex(c: NativeUiComponent?, index: Int) = Unit

    open fun setBounds(c: NativeUiComponent, x: Int, y: Int, width: Int, height: Int) = Unit
    open fun getText(c: NativeUiComponent): String? = null
    open fun setText(c: NativeUiComponent, text: String) = Unit

    open fun getVisible(c: NativeUiComponent): Boolean = false
    open fun setVisible(c: NativeUiComponent, visible: Boolean) = Unit
}

inline class NativeUiComponent(val rawComponent: Any?)
