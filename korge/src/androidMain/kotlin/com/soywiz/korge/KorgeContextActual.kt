package com.soywiz.korge

import android.app.*
import com.soywiz.korio.android.*

actual suspend fun <T> withKorgeContext(context: Any?, callback: suspend () -> T): T {
	return withAndroidContext((context as Activity)) {
		callback()
	}
}
