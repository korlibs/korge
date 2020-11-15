@file:Suppress("PackageDirectoryMismatch", "EXPERIMENTAL_API_USAGE", "unused", "UNUSED_PARAMETER",
    "RedundantVisibilityModifier"
)
package com.soywiz.korge.input

/**
 * Migrate from version 1.15.1 to 2.0.0
 */
@Deprecated(
    message = "Need migrate to down{...}",
    replaceWith = ReplaceWith("down(callback)"),
    level = DeprecationLevel.ERROR
)
public inline fun KeysEvents.onKeyDown(crossinline callback: suspend (com.soywiz.korev.KeyEvent) -> Unit): com.soywiz.korio.lang.Closeable {
    throw Error("migrate KeysEvents.onKeyDown")
}

/**
 * Migrate from version 1.15.1 to 2.0.0
 */
@Deprecated(
    message = "Need migrate to up{...}",
    replaceWith = ReplaceWith("up(callback)"),
    level = DeprecationLevel.ERROR
)
public inline fun KeysEvents.onKeyUp(crossinline callback: suspend (com.soywiz.korev.KeyEvent) -> Unit): com.soywiz.korio.lang.Closeable {
    throw Error("migrate KeysEvents.up")
}
