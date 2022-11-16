package com.soywiz.korui.native

import javax.swing.*

internal open class AwtToolbar(factory: BaseAwtUiFactory, val toolbar: JToolBar = factory.createJToolBar()) : AwtContainer(factory, toolbar), NativeUiFactory.NativeToolbar {
}
