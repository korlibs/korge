package korlibs.korge.ipc

import java.io.*
import java.nio.*
import java.nio.channels.*
import java.nio.file.*

class IPCFrame(val id: Int, val width: Int, val height: Int, val pixels: IntArray = IntArray(0), val buffer: IntBuffer? = null, val pid: Int = -1, val version: Int = -1) {
}

class KorgeFrameBuffer(val path: String) {
    val channel = FileChannel.open(Path.of(path), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)
    var width: Int = 0
    var height: Int = 0
    var buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0L, 32 + 0)
    var ibuffer = buffer.asIntBuffer()

    fun ensureSize(width: Int, height: Int) {
        if (this.width < width || this.height < height) {
            this.width = width
            this.height = height
            buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0L, (32 + (width * height * 4)).toLong())
            ibuffer = buffer.asIntBuffer()
        }
    }

    val currentProcessId = ProcessHandle.current().pid().toInt()

    fun setFrame(frame: IPCFrame) {
        ensureSize(frame.width, frame.height)
        ibuffer.clear()
        ibuffer.put(0) // version
        ibuffer.put(currentProcessId)
        ibuffer.put(frame.id)
        ibuffer.put(frame.width)
        ibuffer.put(frame.height)
        if (frame.buffer != null) {
            ibuffer.put(frame.buffer)
        } else {
            ibuffer.put(frame.pixels)
        }
    }

    fun getFrameId(): Int {
        ibuffer.clear()
        return ibuffer.get(2)
    }

    fun getFrame(): IPCFrame {
        ibuffer.clear()
        val version = ibuffer.get()
        val pid = ibuffer.get()
        val id = ibuffer.get()
        val width = ibuffer.get()
        val height = ibuffer.get()
        ensureSize(width, height)
        val pixels = IntArray(width * height)
        ibuffer.get(pixels)
        return IPCFrame(id, width, height, pixels, pid = pid, version = version)
    }

    fun close() {
        channel.close()
    }

    fun delete() {
        close()
        File(path).delete()
    }
}
