package com.soywiz.korio.file.std

import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString

actual object StandardPaths : StandardBasePathsNative(), StandardPathsBase {
    override val executableFile: String get() = kotlinx.cinterop.memScoped {
        val maxSize = 4096
        val data = allocArray<kotlinx.cinterop.UShortVar>(maxSize + 1)
        platform.windows.GetModuleFileNameW(null, data.reinterpret(), maxSize.convert())
        data.toKString()
    }.replace('\\', '/')
}
