package com.soywiz.korio.util

internal expect val rawPlatformName: String
internal expect val rawOsName: String

object OS {
	val rawName get() = rawOsName
	val rawNameLC by lazy { rawName.toLowerCase() }

	val platformName get() = rawPlatformName
	val platformNameLC by lazy { platformName.toLowerCase() }

	val isWindows by lazy { rawNameLC.contains("win") }
	val isUnix get() = !isWindows
	val isPosix get() = !isWindows
	val isLinux by lazy { rawNameLC.contains("nix") || rawNameLC.contains("nux") || rawNameLC.contains("aix") }
	val isMac by lazy { rawNameLC.contains("mac") }

	val isIos by lazy { rawNameLC.contains("ios") }
	val isAndroid by lazy { platformNameLC.contains("android") }
    val isWatchos by lazy { rawNameLC.contains("watchos") }
    val isTvos by lazy { rawNameLC.contains("tvos") }

	val isJs get() = rawPlatformName.endsWith("js")
	val isNative get() = rawPlatformName == "native"
    val isNativeDesktop get() = isNative && !isAndroid && !isIos && !isWatchos && !isTvos
	val isJvm get() = rawPlatformName == "jvm"

	val isJsShell get() = platformNameLC == "shell.js"
	val isJsNodeJs get() = platformNameLC == "node.js"
	val isJsBrowser get() = platformNameLC == "web.js"
	val isJsWorker get() = platformNameLC == "worker.js"
	val isJsBrowserOrWorker get() = isJsBrowser || isJsWorker
}
