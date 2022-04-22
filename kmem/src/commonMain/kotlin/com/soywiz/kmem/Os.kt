package com.soywiz.kmem

import com.soywiz.kmem.internal.currentOs

enum class Os {
    UNKNOWN, MACOSX, IOS, LINUX, WINDOWS, ANDROID, TVOS, WATCHOS;

    val isWindows: Boolean get() = this == WINDOWS
    val isAndroid: Boolean get() = this == ANDROID
    val isLinux: Boolean get() = this == LINUX
    val isMac: Boolean get() = this == MACOSX
    val isIos: Boolean get() = this == IOS
    val isTvos: Boolean get() = this == TVOS
    val isWatchos: Boolean get() = this == WATCHOS
    val isAppleMobile: Boolean get() = isIos || isTvos || isWatchos
    val isDesktop: Boolean get() = isLinux || isWindows || isMac
    val isMobile: Boolean get() = isAndroid || isAppleMobile
    val isApple: Boolean get() = isMac || isAppleMobile
    val isPosix: Boolean get() = !isWindows

    companion object {
        val CURRENT: Os get() = currentOs
    }
}
