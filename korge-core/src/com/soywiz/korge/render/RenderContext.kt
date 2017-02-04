package com.soywiz.korge.render

import com.soywiz.korag.AG

class RenderContext(
        val ag: AG
) {
    val batch = BatchBuilder2D(ag)

    fun flush() {
        batch.flush()
    }
}