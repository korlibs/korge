package com.soywiz.korio.util

import com.soywiz.kmem.Platform

@Deprecated("Use com.soywiz.kmem.Platform instead")
object OS {
	val rawName get() = Platform.rawOsName
	val rawNameLC by lazy { rawName.lowercase() }

	val platformName get() = Platform.rawPlatformName
	val platformNameLC by lazy { platformName.lowercase() }

	val isWindows: Boolean get() = Platform.os.isWindows
	val isUnix: Boolean get() = Platform.os.isPosix
	val isPosix: Boolean get() = Platform.os.isPosix
	val isLinux: Boolean get() = Platform.os.isLinux
	val isMac: Boolean get() = Platform.os.isMac

	val isIos: Boolean get() = Platform.os.isIos
	val isAndroid: Boolean get() = Platform.os.isAndroid
    val isWatchos: Boolean get() = Platform.os.isWatchos
    val isTvos: Boolean get() = Platform.os.isTvos

	val isJs: Boolean get() = Platform.runtime.isJs
	val isNative: Boolean get() = Platform.runtime.isNative
    val isNativeDesktop : Boolean get() = isNative && Platform.os.isDesktop
	val isJvm: Boolean get() = Platform.runtime.isJvm

	val isJsShell get() = Platform.rawPlatformName == "js-shell"
	val isJsNodeJs get() = Platform.rawPlatformName == "js-node"
    val isJsDenoJs get() = Platform.rawPlatformName == "js-deno"
	val isJsBrowser get() = Platform.rawPlatformName == "js-web"
	val isJsWorker get() = Platform.rawPlatformName == "js-worker"
	val isJsBrowserOrWorker get() = isJsBrowser || isJsWorker

    val isDebug: Boolean get() = Platform.isDebug
    val isRelease: Boolean get() = Platform.isRelease
}
