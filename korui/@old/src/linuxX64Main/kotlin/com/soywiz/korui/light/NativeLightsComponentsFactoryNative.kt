package com.soywiz.korui.light

import com.soywiz.korag.*
import com.soywiz.kgl.*
import com.soywiz.korui.*
import kotlin.coroutines.*
import com.soywiz.korio.async.*
import com.soywiz.korui.*

actual object NativeLightsComponentsFactory : LightComponentsFactory {
    actual override fun create(context: CoroutineContext, nativeCtx: Any?): LightComponents = (nativeCtx as NativeKoruiContext).light
}
