package com.soywiz.kgl

import com.soywiz.kmem.*
import java.nio.*

val FBuffer.nioBuffer: java.nio.ByteBuffer get() = this.mem.buffer.apply { (this as Buffer).rewind() }
val FBuffer.nioByteBuffer: java.nio.ByteBuffer get() = this.mem.buffer.apply { (this as Buffer).rewind() }
val FBuffer.nioIntBuffer: java.nio.IntBuffer get() = this.arrayInt.jbuffer.apply { (this as Buffer).rewind() }
val FBuffer.nioFloatBuffer: java.nio.FloatBuffer get() = this.arrayFloat.jbuffer.apply { (this as Buffer).rewind() }
