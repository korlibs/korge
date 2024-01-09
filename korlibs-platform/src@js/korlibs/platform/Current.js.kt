package korlibs.platform

import kotlinx.browser.*
import org.khronos.webgl.Uint32Array
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.dom.*

internal val isDenoJs: Boolean by lazy { js("(typeof Deno === 'object' && Deno.statSync)").unsafeCast<Boolean>() }
internal val isWeb: Boolean by lazy { js("(typeof window === 'object')").unsafeCast<Boolean>() }
internal val isWorker: Boolean by lazy { js("(typeof importScripts === 'function')").unsafeCast<Boolean>() }
internal val isNodeJs: Boolean by lazy { js("((typeof process !== 'undefined') && process.release && (process.release.name.search(/node|io.js/) !== -1))").unsafeCast<Boolean>() }
internal val isShell: Boolean get() = !isWeb && !isNodeJs && !isWorker

private external val Deno: dynamic

// @TODO: Check navigator.userAgent
internal actual val currentOs: Os
    get() = when {
        isDenoJs -> {
            when (Deno.build.os) {
                "darwin" -> Os.MACOSX
                "linux" -> Os.LINUX
                "windows" -> Os.WINDOWS
                else -> Os.UNKNOWN
            }
        }
        else -> {
            Os.UNKNOWN
        }
    }
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

@JsName("process")
private external val _process: dynamic // nodejs

val process: dynamic get() {
    if (js("(typeof process === 'undefined')")) {
        try {
            error("Not in NodeJS. Can't access process")
        } catch (e: Throwable) {
            e.printStackTrace()
            throw e
        }
    }
    return _process
}

val jsWindow: Window get() {
    if (js("(typeof window === 'undefined')")) {
        try {
            error("Not in Browser. Can't access window")
        } catch (e: Throwable) {
            e.printStackTrace()
            throw e
        }
    }
    return window
}

@JsName("import")
external fun jsImport(url: String): dynamic

@JsName("globalThis")
private external val globalThis: dynamic // all

val jsGlobalThis: WindowOrWorkerGlobalScope get() {
    return globalThis
}

val jsDocument: Document get() {
    if (js("(typeof document === 'undefined')")) {
        try {
            error("Not in Browser. Can't access document")
        } catch (e: Throwable) {
            e.printStackTrace()
            throw e
        }
    }
    return document
}

internal actual val currentRawOsName: String = when {
    isDenoJs -> "deno"
    isWeb || isWorker -> navigator.userAgent.unsafeCast<String>()
    else -> process.platform.unsafeCast<String>()
}

@JsName("Object")
external object JsObject {
    fun keys(obj: dynamic): dynamic
}
