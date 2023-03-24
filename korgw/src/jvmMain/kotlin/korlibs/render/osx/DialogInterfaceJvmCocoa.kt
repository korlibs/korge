package korlibs.render.osx

import korlibs.render.*

class DialogInterfaceJvmCocoa(val gwProvider: () -> MacGameWindow) : DialogInterface {
    val gw get() = gwProvider()
}