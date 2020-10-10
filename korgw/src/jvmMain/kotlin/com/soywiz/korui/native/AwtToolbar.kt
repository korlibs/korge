package com.soywiz.korui.native

import javax.swing.*

open class AwtToolbar(factory: BaseAwtUiFactory, val toolbar: JToolBar = factory.createJToolBar()) : AwtContainer(factory, toolbar), NativeUiFactory.NativeToolbar {
}
