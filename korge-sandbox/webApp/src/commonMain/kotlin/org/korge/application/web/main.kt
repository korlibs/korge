package org.korge.application.web

import org.korge.application.main as applicationMain

// Web (JS / WASM) entry point. Delegates to the shared application game entry.
suspend fun main() {
    applicationMain()
}
