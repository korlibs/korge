package com.soywiz.korge.view

/**
 * Something that can be updated [update] providing the delta time in milliseconds since the last update.
 */
interface Updatable {
	fun update(dtMs: Int): Unit
}
