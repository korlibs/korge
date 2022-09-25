package com.soywiz.korio.file.std

actual object StandardPaths : StandardBasePathsDarwin(), StandardPathsBase {
    //override val executableFolder: String get() = posixRealpath(".")
}
