package com.soywiz.korui.native

import javax.swing.*

open class AwtToolbar(factory: BaseAwtUiFactory, val toolbar: JToolBar = JToolBar()) : AwtContainer(factory, toolbar), NativeUiFactory.NativeToolbar {
}
