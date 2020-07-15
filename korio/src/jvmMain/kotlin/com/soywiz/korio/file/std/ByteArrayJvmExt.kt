package com.soywiz.korio.file.std

import java.io.*

suspend fun ByteArray.writeToFile(file: File) = localVfs(file).write(this)
