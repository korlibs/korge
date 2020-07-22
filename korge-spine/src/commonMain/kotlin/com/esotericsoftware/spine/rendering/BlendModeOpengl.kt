package com.esotericsoftware.spine.rendering

import com.esotericsoftware.spine.*

internal fun BlendMode.getSource(premultipliedAlpha: Boolean): Int {
    return if (premultipliedAlpha) sourcePMA else source
}

internal val BlendMode.source: Int get() = when (this) {
    BlendMode.normal -> GL20.GL_SRC_ALPHA
    BlendMode.additive -> GL20.GL_SRC_ALPHA
    BlendMode.multiply -> GL20.GL_DST_COLOR
    BlendMode.screen -> GL20.GL_ONE
}

internal val BlendMode.sourcePMA: Int get() = when (this) {
    BlendMode.normal -> GL20.GL_ONE
    BlendMode.additive -> GL20.GL_ONE
    BlendMode.multiply -> GL20.GL_DST_COLOR
    BlendMode.screen -> GL20.GL_ONE
}

internal val BlendMode.dest: Int get() = when (this) {
    BlendMode.normal -> GL20.GL_ONE_MINUS_SRC_ALPHA
    BlendMode.additive -> GL20.GL_ONE
    BlendMode.multiply -> GL20.GL_ONE_MINUS_SRC_ALPHA
    BlendMode.screen -> GL20.GL_ONE_MINUS_SRC_COLOR
}
