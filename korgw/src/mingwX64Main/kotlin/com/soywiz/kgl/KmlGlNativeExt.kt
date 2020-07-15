package com.soywiz.kgl

import platform.opengl32.*
import platform.windows.*
import platform.posix.*

private val OPENGL32_DLL_MODULE: HMODULE? by lazy { LoadLibraryA("opengl32.dll") }

internal fun wglGetProcAddressAny(name: String): PROC? {
	return wglGetProcAddress(name)
			?: GetProcAddress(OPENGL32_DLL_MODULE, name)
			?: throw RuntimeException("Can't find GL function: '$name'")
}
