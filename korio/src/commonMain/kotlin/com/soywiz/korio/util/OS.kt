package com.soywiz.korio.util

import com.soywiz.kmem.Platform
import com.soywiz.kmem.*

@Deprecated("Use com.soywiz.kmem.Platform instead")
object OS {
	val rawName: String get() = Platform.rawOsName
	val rawNameLC: String by lazy { rawName.lowercase() }

	val platformName: String get() = Platform.rawPlatformName
	val platformNameLC: String by lazy { platformName.lowercase() }

	val isWindows: Boolean get() = Platform.isWindows
	val isUnix: Boolean get() = Platform.isPosix
	val isPosix: Boolean get() = Platform.isPosix
	val isLinux: Boolean get() = Platform.isLinux
	val isMac: Boolean get() = Platform.isMac

	val isIos: Boolean get() = Platform.isIos
	val isAndroid: Boolean get() = Platform.isAndroid
    val isWatchos: Boolean get() = Platform.isWatchos
    val isTvos: Boolean get() = Platform.isTvos

	val isJs: Boolean get() = Platform.isJs
	val isNative: Boolean get() = Platform.isNative
    val isNativeDesktop: Boolean get() = Platform.isNativeDesktop
	val isJvm: Boolean get() = Platform.isJvm

	val isJsShell: Boolean get() = Platform.isJsShell
	val isJsNodeJs: Boolean get() = Platform.isJsNodeJs
    val isJsDenoJs: Boolean get() = Platform.isJsDenoJs
	val isJsBrowser: Boolean get() = Platform.isJsBrowser
	val isJsWorker: Boolean get() = Platform.isJsWorker
	val isJsBrowserOrWorker: Boolean get() = Platform.isJsBrowserOrWorker

    val isDebug: Boolean get() = Platform.isDebug
    val isRelease: Boolean get() = Platform.isRelease
}
