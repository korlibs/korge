package korlibs.render

actual fun createDialogInterfaceForComponent(nativeComponent: Any?): DialogInterface {
    return DialogInterfaceIos()
}

class DialogInterfaceIos : DialogInterface {
}
