package com.soywiz.korgw.osx

import com.soywiz.korgw.*

class DialogInterfaceJvmCocoa(val gwProvider: () -> MacGameWindow) : DialogInterface {
    val gw get() = gwProvider()
}
