package com.soywiz.korui.light

import korlibs.graphics.*
import korlibs.kgl.*
import com.soywiz.korui.*
import kotlin.coroutines.*
import korlibs.io.async.*
import com.soywiz.korui.*

actual object NativeLightsComponentsFactory : LightComponentsFactory {
    actual override fun create(context: CoroutineContext, nativeCtx: Any?): LightComponents = LightComponents()
}
