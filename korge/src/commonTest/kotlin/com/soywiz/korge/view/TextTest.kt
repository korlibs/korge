package com.soywiz.korge.view

import com.soywiz.korag.log.*
import com.soywiz.korge.render.*
import com.soywiz.korio.async.*
import kotlin.test.*

class TextTest {
	@Test
    @Ignore
    // testRender[macosX64]
    /*
    null
    kotlin.ArrayIndexOutOfBoundsException
        at 0   test.kexe                           0x0000000100e3d5e7 kfun:kotlin.Exception.<init>()kotlin.Exception + 55 (/Users/teamcity/buildAgent/work/4d622a065c544371/runtime/src/main/kotlin/kotlin/Exceptions.kt:21:28)
        at 1   test.kexe                           0x0000000100e3c8a7 kfun:kotlin.RuntimeException.<init>()kotlin.RuntimeException + 55 (/Users/teamcity/buildAgent/work/4d622a065c544371/runtime/src/main/kotlin/kotlin/Exceptions.kt:32:28)
        at 2   test.kexe                           0x0000000100e3ca47 kfun:kotlin.IndexOutOfBoundsException.<init>()kotlin.IndexOutOfBoundsException + 55 (/Users/teamcity/buildAgent/work/4d622a065c544371/runtime/src/main/kotlin/kotlin/Exceptions.kt:90:28)
        at 3   test.kexe                           0x0000000100e3d067 kfun:kotlin.ArrayIndexOutOfBoundsException.<init>()kotlin.ArrayIndexOutOfBoundsException + 55 (/Users/teamcity/buildAgent/work/4d622a065c544371/runtime/src/main/kotlin/kotlin/Exceptions.kt:97:21)
        at 4   test.kexe                           0x0000000100e8ff37 ThrowArrayIndexOutOfBoundsException + 119 (/Users/teamcity/buildAgent/work/4d622a065c544371/runtime/src/main/kotlin/kotlin/native/internal/RuntimeUtils.kt:18:11)
        at 5   test.kexe                           0x0000000101440cd5 Kotlin_Arrays_getIntArrayAddressOfElement + 37
        at 6   test.kexe                           0x0000000100ddb94a kfun:kotlinx.cinterop.addressOf@kotlinx.cinterop.Pinned<kotlin.IntArray>.(kotlin.Int)ValueType + 170 (/Users/teamcity/buildAgent/work/4d622a065c544371/Interop/Runtime/src/native/kotlin/kotlinx/cinterop/Pinning.kt:53:75)
        at 7   test.kexe                           0x00000001012302de kfun:com.soywiz.korim.format.cg.CoreGraphicsRenderer.flushCommands() + 2942 (/Users/soywiz/projects/korlibs/korim/korim/src/nativePosixAppleMain/kotlin/com/soywiz/korim/format/cg/CgRenderer.kt:99:37)
        at 8   test.kexe                           0x00000001012012a8 kfun:com.soywiz.korim.vector.Context2d.BufferedRenderer.flush() + 72 (/Users/soywiz/projects/korlibs/korim/korim/src/commonMain/kotlin/com/soywiz/korim/vector/Context2d.kt:87:38)
        at 9   test.kexe                           0x00000001012011ff kfun:com.soywiz.korim.vector.Context2d.BufferedRenderer.renderText(com.soywiz.korim.vector.Context2d.State;com.soywiz.korim.vector.Context2d.Font;kotlin.String;kotlin.Double;kotlin.Double;kotlin.Boolean) + 863 (/Users/soywiz/projects/korlibs/korim/korim/src/commonMain/kotlin/com/soywiz/korim/vector/Context2d.kt:84:33)
        at 10  test.kexe                           0x000000010120f13e kfun:com.soywiz.korim.vector.Context2d.renderText(kotlin.String;kotlin.Double;kotlin.Double;kotlin.Boolean) + 446 (/Users/soywiz/projects/korlibs/korim/korim/src/commonMain/kotlin/com/soywiz/korim/vector/Context2d.kt:445:0)
        at 11  test.kexe                           0x00000001011dcbf7 kfun:com.soywiz.korim.font.BitmapFontGenerator.generate(kotlin.String;kotlin.Int;kotlin.IntArray;kotlin.Boolean)com.soywiz.korim.font.BitmapFont + 4951 (/Users/soywiz/projects/korlibs/korim/korim/src/commonMain/kotlin/com/soywiz/korim/font/BitmapFontGenerator.kt:47:0)
        at 12  test.kexe                           0x00000001011db56a kfun:com.soywiz.korim.font.BitmapFontGenerator.generate(kotlin.String;kotlin.Int;kotlin.String;kotlin.Boolean)com.soywiz.korim.font.BitmapFont + 1194 (/Users/soywiz/projects/korlibs/korim/korim/src/commonMain/kotlin/com/soywiz/korim/font/BitmapFontGenerator.kt:21:3)
        at 13  test.kexe                           0x00000001011db7f6 kfun:com.soywiz.korim.font.BitmapFontGenerator.generate$default(kotlin.String;kotlin.Int;kotlin.String;kotlin.Boolean;kotlin.Int)com.soywiz.korim.font.BitmapFont + 502 (/Users/soywiz/projects/korlibs/korim/korim/src/commonMain/kotlin/com/soywiz/korim/font/BitmapFontGenerator.kt:20:2)
        at 14  test.kexe                           0x000000010136b933 kfun:com.soywiz.korge.view.Fonts.getBitmapFont(kotlin.String;kotlin.Int)com.soywiz.korim.font.BitmapFont + 1091 (/Users/soywiz/projects/korlibs/korge/korge/src/commonMain/kotlin/com/soywiz/korge/view/Fonts.kt:26:25)
        at 15  test.kexe                           0x000000010136bcae kfun:com.soywiz.korge.view.Fonts.getBitmapFont(com.soywiz.korge.html.Html.FontFace;kotlin.Int)com.soywiz.korim.font.BitmapFont + 574 (/Users/soywiz/projects/korlibs/korge/korge/src/commonMain/kotlin/com/soywiz/korge/view/Fonts.kt:33:29)
        at 16  test.kexe                           0x000000010136bf96 kfun:com.soywiz.korge.view.Fonts.getBitmapFont(com.soywiz.korge.html.Html.Format)com.soywiz.korim.font.BitmapFont + 278 (/Users/soywiz/projects/korlibs/korge/korge/src/commonMain/kotlin/com/soywiz/korge/view/Fonts.kt:38:55)
        at 17  test.kexe                           0x00000001013761e3 kfun:com.soywiz.korge.view.Text.renderInternal(com.soywiz.korge.render.RenderContext) + 2595 (/Users/soywiz/projects/korlibs/korge/korge/src/commonMain/kotlin/com/soywiz/korge/view/Text.kt:109:21)
        at 18  test.kexe                           0x0000000101380d09 kfun:com.soywiz.korge.view.View.render(com.soywiz.korge.render.RenderContext) + 473 (/Users/soywiz/projects/korlibs/korge/korge/src/commonMain/kotlin/com/soywiz/korge/view/View.kt:443:4)
        at 19  test.kexe                           0x00000001013c9f23 kfun:com.soywiz.korge.view.TextTest.$testRender$lambda-0COROUTINE$14.invokeSuspend#internal + 867 (/Users/soywiz/projects/korlibs/korge/korge/src/commonTest/kotlin/com/soywiz/korge/view/TextTest.kt:13:8)
        at 20  test.kexe                           0x00000001013ca2de kfun:com.soywiz.korge.view.TextTest.$testRender$lambda-0COROUTINE$14.invoke#internal + 206 (/Users/soywiz/projects/korlibs/korge/korge/src/commonTest/kotlin/com/soywiz/korge/view/TextTest.kt:10:33)
        at 21  test.kexe                           0x0000000101100a3b kfun:com.soywiz.korio.async.$asyncEntryPoint$lambda-0COROUTINE$312.invokeSuspend#internal + 619 (/Users/travis/build/korlibs/korio/korio/src/nativeCommonMain/kotlin/com/soywiz/korio/async/AsyncExtNative.kt:6:74)
        at 22  test.kexe                           0x0000000100e6648a kfun:kotlin.coroutines.native.internal.BaseContinuationImpl.resumeWith(kotlin.Result<kotlin.Any?>) + 730 (/Users/teamcity/buildAgent/work/4d622a065c544371/runtime/src/main/kotlin/kotlin/coroutines/ContinuationImpl.kt:26:0)
        at 23  test.kexe                           0x0000000100f56e7f kfun:kotlinx.coroutines.DispatchedTask.run() + 2783 (/opt/buildAgent/work/44ec6e850d5c63f0/kotlinx-coroutines-core/common/src/Dispatched.kt:227:0)
        at 24  test.kexe                           0x0000000100f5bdc6 kfun:kotlinx.coroutines.EventLoopImplBase.processNextEvent()ValueType + 710 (/opt/buildAgent/work/44ec6e850d5c63f0/kotlinx-coroutines-core/common/src/EventLoop.common.kt:270:20)
        at 25  test.kexe                           0x0000000100fabb92 kfun:kotlinx.coroutines.BlockingCoroutine.joinBlocking#internal + 1570 (/opt/buildAgent/work/44ec6e850d5c63f0/kotlinx-coroutines-core/native/src/Builders.kt:60:0)
        at 26  test.kexe                           0x0000000100faadc1 kfun:kotlinx.coroutines.runBlocking(kotlin.coroutines.CoroutineContext;kotlin.coroutines.SuspendFunction1<kotlinx.coroutines.CoroutineScope,#GENERIC>)Generic + 1233 (/opt/buildAgent/work/44ec6e850d5c63f0/kotlinx-coroutines-core/native/src/Builders.kt:50:22)
        at 27  test.kexe                           0x0000000100fab334 kfun:kotlinx.coroutines.runBlocking$default(kotlin.coroutines.CoroutineContext;kotlin.coroutines.SuspendFunction1<kotlinx.coroutines.CoroutineScope,#GENERIC>;kotlin.Int)Generic + 372 (/opt/buildAgent/work/44ec6e850d5c63f0/kotlinx-coroutines-core/native/src/Builders.kt:33:8)
        at 28  test.kexe                           0x0000000101100629 kfun:com.soywiz.korio.async.asyncEntryPoint(kotlin.coroutines.SuspendFunction0<kotlin.Unit>) + 217 (/Users/travis/build/korlibs/korio/korio/src/nativeCommonMain/kotlin/com/soywiz/korio/async/AsyncExtNative.kt:6:60)
     */
	fun testRender() = suspendTest {
		val text = Text()
		val ag = LogAG()
		text.render(RenderContext(ag))
	}
}
