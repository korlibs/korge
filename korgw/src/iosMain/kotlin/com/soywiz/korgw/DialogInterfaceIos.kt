package com.soywiz.korgw

actual fun createDialogInterfaceForComponent(nativeComponent: Any?): DialogInterface {
    return DialogInterfaceIos()
}

class DialogInterfaceIos : DialogInterface {
}
