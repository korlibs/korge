package com.soywiz.korio.file.std

import com.soywiz.korio.posix.posixGetcwd

open class StandardBasePathsNative : StandardPathsBase {
    override val cwd: String get() = customCwd ?: posixGetcwd()
}
