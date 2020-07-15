package com.soywiz.korag.internal

import com.soywiz.kmem.*

// @TODO: Replace with copyAligned?
internal fun FBuffer.setFloats(offset: Int, data: FloatArray, dataOffset: Int, count: Int) = this.apply { for (n in 0 until count) this.setFloat(offset + n, data[dataOffset + n]) }
