package com.soywiz.kmem.internal

import com.soywiz.kmem.Arch
import com.soywiz.kmem.Os
import com.soywiz.kmem.Runtime
import org.khronos.webgl.Uint32Array
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get

internal val isDenoJs: Boolean by lazy { js("(typeof Deno === 'object' && Deno.statSync)").unsafeCast<Boolean>() }
internal val isWeb: Boolean by lazy { js("(typeof window === 'object')").unsafeCast<Boolean>() }
internal val isWorker: Boolean by lazy { js("(typeof importScripts === 'function')").unsafeCast<Boolean>() }
internal val isNodeJs: Boolean by lazy { js("((typeof process !== 'undefined') && process.release && (process.release.name.search(/node|io.js/) !== -1))").unsafeCast<Boolean>() }
internal val isShell: Boolean get() = !isWeb && !isNodeJs && !isWorker

// @TODO: Check navigator.userAgent
internal actual val currentOs: Os = Os.UNKNOWN
internal actual val currentArch: Arch = Arch.UNKNOWN

internal actual val currentRuntime: Runtime = Runtime.JS
internal actual val currentIsDebug: Boolean = false
internal actual val currentIsLittleEndian: Boolean = Uint8Array(Uint32Array(arrayOf(0x11223344)).buffer)[0].toInt() == 0x44
internal actual val multithreadedSharedHeap: Boolean = false // Workers have different heaps

internal actual val currentRawPlatformName: String = when {
    isDenoJs -> "js-deno"
    isWeb -> "js-web"
    isNodeJs -> "js-node"
    isWorker -> "js-worker"
    isShell -> "js-shell"
    else -> "js"
}

private external val navigator: dynamic // browser
private external val process: dynamic // nodejs

internal actual val currentRawOsName: String = when {
    isDenoJs -> "deno"
    isWeb || isWorker -> navigator.userAgent.unsafeCast<String>()
    else -> process.platform.unsafeCast<String>()
}
