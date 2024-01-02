package korlibs.platform

import org.khronos.webgl.*

@JsFun("() => { return (typeof Deno === 'object' && Deno.statSync) }")
internal external fun isDenoJs(): Boolean
@JsFun("() => { return (typeof window === 'object') }")
internal external fun isWeb(): Boolean
@JsFun("() => { return (typeof importScripts === 'function') }")
internal external fun isWorker(): Boolean
@JsFun("() => { return ((typeof process !== 'undefined') && process.release && (process.release.name.search(/node|io.js/) !== -1)) }")
internal external fun isNodeJs(): Boolean
internal fun isShell(): Boolean = !isWeb() && !isNodeJs() && !isWorker()

// @TODO: Check navigator.userAgent
internal actual val currentOs: Os = Os.UNKNOWN
internal actual val currentArch: Arch = Arch.UNKNOWN

internal actual val currentRuntime: Runtime = Runtime.WASM
internal actual val currentIsDebug: Boolean = false
internal actual val currentIsLittleEndian: Boolean = Uint8Array(Uint32Array(1).also { it[0] = 0x11223344 }.buffer)[0].toInt() == 0x44
internal actual val multithreadedSharedHeap: Boolean = false // Workers have different heaps

internal actual val currentRawPlatformName: String = when {
    isDenoJs() -> "wasm-deno"
    isWeb() -> "wasm-web"
    isNodeJs() -> "wasm-node"
    isWorker() -> "wasm-worker"
    isShell() -> "wasm-shell"
    else -> "wasm"
}

private external class Navigator {
    val userAgent: String
}
private external class Process {
    val platform: String
}
private external val navigator: Navigator // browser
private external val process: Process // nodejs

internal actual val currentRawOsName: String = when {
    isDenoJs() -> "deno"
    isWeb() || isWorker() -> navigator.userAgent
    else -> process.platform
}
