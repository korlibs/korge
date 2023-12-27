package korlibs.platform

import org.w3c.dom.*

@JsName("globalThis")
private external val globalThis: dynamic // all

val jsGlobalThis: WindowOrWorkerGlobalScope
    get() {
    return globalThis
}
