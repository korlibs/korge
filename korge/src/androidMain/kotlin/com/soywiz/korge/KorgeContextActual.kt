package com.soywiz.korge

import android.app.*
import withAndroidContext

actual suspend fun <T> withKorgeContext(context: Any?, callback: suspend () -> T): T {
	return withAndroidContext((context as Activity)) {
		callback()
	}
}
