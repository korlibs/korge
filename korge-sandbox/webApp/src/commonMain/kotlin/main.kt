package org.korge.sandbox.web

import org.korge.sandbox.main as sandboxMain

// Web (JS / WASM) entry point. Delegates to the shared sandbox game entry.
suspend fun main() {
    sandboxMain()
}
