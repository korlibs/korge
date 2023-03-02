package com.soywiz.korau.sound.backends

import com.soywiz.kmem.*
import com.soywiz.kmem.dyn.*
import kotlinx.cinterop.*

actual object ASoundImpl : ASound2 {
    override val initialized: Boolean get() = A2.initialized

    override fun alloc_params(): Long = platform.posix.malloc(1024)!!.rawValue.toLong()
    override fun free_params(value: Long) = platform.posix.free(value.toCPointer<ByteVar>())

    override fun snd_pcm_open(name: String, stream: Int, mode: Int): Long {
        return memScoped {
            val out = alloc<COpaquePointerVar>()
            A2.snd_pcm_open(out.ptr, name.cstr.ptr, stream, mode)
            out.value.toLong()
        }
    }

    override fun snd_pcm_hw_params_any(pcm: Long, params: Long): Int = A2.snd_pcm_hw_params_any(pcm.toCPointer(), params.toCPointer())
    override fun snd_pcm_hw_params_set_access(pcm: Long, params: Long, access: Int): Int = A2.snd_pcm_hw_params_set_access(pcm.toCPointer(), params.toCPointer(), access)
    override fun snd_pcm_hw_params_set_format(pcm: Long, params: Long, format: Int): Int = A2.snd_pcm_hw_params_set_format(pcm.toCPointer(), params.toCPointer(), format)
    override fun snd_pcm_hw_params_set_channels(pcm: Long, params: Long, channels: Int): Int = A2.snd_pcm_hw_params_set_channels(pcm.toCPointer(), params.toCPointer(), channels)
    override fun snd_pcm_hw_params_set_rate(pcm: Long, params: Long, rate: Int, dir: Int): Int = A2.snd_pcm_hw_params_set_rate(pcm.toCPointer(), params.toCPointer(), rate, dir)
    override fun snd_pcm_hw_params(pcm: Long, params: Long): Int = A2.snd_pcm_hw_params(pcm.toCPointer(), params.toCPointer())

    override fun snd_pcm_name(pcm: Long): String = A2.snd_pcm_name(pcm.toCPointer()).toKString()
    override fun snd_pcm_state(pcm: Long): Int = A2.snd_pcm_state(pcm.toCPointer())
    override fun snd_pcm_state_name(state: Int): String = A2.snd_pcm_state_name(state).toKString()

    override fun snd_pcm_hw_params_get_period_size(params: Long): Int {
        memScoped {
            val out = alloc<IntVar>()
            val dir = alloc<IntVar>()
            A2.snd_pcm_hw_params_get_period_size(params.toCPointer(), out.ptr, dir.ptr)
            return out.value.toInt()
        }
    }

    override fun snd_pcm_writei(pcm: Long, buffer: ShortArray, size: Int): Int = buffer.usePinned {
        A2.snd_pcm_writei(pcm.toCPointer(), it.startAddressOf, size)
    }

    override fun snd_pcm_prepare(pcm: Long): Int = A2.snd_pcm_prepare(pcm.toCPointer())
    override fun snd_pcm_drain(pcm: Long): Int = A2.snd_pcm_drain(pcm.toCPointer())
    override fun snd_pcm_close(pcm: Long): Int = A2.snd_pcm_close(pcm.toCPointer())
}


internal object A2 : DynamicLibrary("libasound.so.2") {
    inline val initialized: Boolean get() = isAvailable
    val snd_pcm_open by func<(pcmPtr: COpaquePointer?, name: CPointer<ByteVar>, stream: Int, mode: Int) -> Int>()
    val snd_pcm_hw_params_any by func<(pcm: COpaquePointer??, params: COpaquePointer?) -> Int>()
    val snd_pcm_hw_params_set_access by func<(pcm: COpaquePointer?, params: COpaquePointer?, access: Int) -> Int>()
    val snd_pcm_hw_params_set_format by func<(pcm: COpaquePointer?, params: COpaquePointer?, format: Int) -> Int>()
    val snd_pcm_hw_params_set_channels by func<(pcm: COpaquePointer?, params: COpaquePointer?, channels: Int) -> Int>()
    val snd_pcm_hw_params_set_rate by func<(pcm: COpaquePointer?, params: COpaquePointer?, rate: Int, dir: Int) -> Int>()
    val snd_pcm_hw_params by func<(pcm: COpaquePointer?, params: COpaquePointer?) -> Int>()
    val snd_pcm_name by func<(pcm: COpaquePointer?) -> CPointer<ByteVar>>()
    val snd_pcm_state by func<(pcm: COpaquePointer?) -> Int>()
    val snd_pcm_state_name by func<(state: Int) -> CPointer<ByteVar>>()
    val snd_pcm_hw_params_get_channels by func<(params: COpaquePointer?, out: COpaquePointer?) -> Int>()
    val snd_pcm_hw_params_get_rate by func<(params: COpaquePointer?, value: COpaquePointer?, dir: COpaquePointer?) -> Int>()
    val snd_pcm_hw_params_get_period_size by func<(params: COpaquePointer?, value: COpaquePointer?, dir: COpaquePointer?) -> Int>()
    val snd_pcm_hw_params_get_period_time by func<(params: COpaquePointer?, value: COpaquePointer?, dir: COpaquePointer?) -> Int>()
    val snd_pcm_writei by func<(pcm: COpaquePointer?, buffer: COpaquePointer?, size: Int) -> Int>()
    val snd_pcm_prepare by func<(pcm: COpaquePointer?) -> Int>()
    val snd_pcm_drain by func<(pcm: COpaquePointer?) -> Int>()
    val snd_pcm_close by func<(pcm: COpaquePointer?) -> Int>()

}
