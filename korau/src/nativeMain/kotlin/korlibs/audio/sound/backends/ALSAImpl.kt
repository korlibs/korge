package korlibs.audio.sound.backends

import korlibs.memory.*
import korlibs.memory.dyn.*
import kotlinx.cinterop.*

actual object ASoundImpl : ASound2 {
    override val initialized: Boolean get() = A2.initialized

    override fun snd_pcm_open(name: String, stream: Int, mode: Int): Long {
        return memScoped {
            val out = alloc<COpaquePointerVar>()
            A2.snd_pcm_open(out.ptr, name.cstr.ptr, stream, mode)
            out.value.toLong()
        }
    }

    override fun snd_pcm_name(pcm: Long): String = A2.snd_pcm_name(pcm.toCPointer()).toKString()
    override fun snd_pcm_state(pcm: Long): Int = A2.snd_pcm_state(pcm.toCPointer())
    override fun snd_pcm_state_name(state: Int): String = A2.snd_pcm_state_name(state).toKString()

    override fun snd_pcm_delay(params: Long): Int {
        memScoped {
            val out = alloc<IntVar>()
            A2.snd_pcm_delay(params.toCPointer(), out.ptr)
            return out.value.toInt()
        }
    }

    override fun snd_pcm_writei(pcm: Long, buffer: ShortArray, offset: Int, size: Int, frames: Int): Int = buffer.usePinned {
        A2.snd_pcm_writei(pcm.toCPointer(), it.addressOf(offset), frames)
    }

    override fun snd_pcm_prepare(pcm: Long): Int = A2.snd_pcm_prepare(pcm.toCPointer())
    override fun snd_pcm_drain(pcm: Long): Int = A2.snd_pcm_drain(pcm.toCPointer())
    override fun snd_pcm_drop(pcm: Long): Int = A2.snd_pcm_drop(pcm.toCPointer())
    override fun snd_pcm_close(pcm: Long): Int = A2.snd_pcm_close(pcm.toCPointer())

    override fun snd_pcm_recover(pcm: Long, err: Int, silent: Int): Int {
        return A2.snd_pcm_recover(pcm.toCPointer(), err, silent)
    }

    override fun snd_pcm_set_params(
        pcm: Long,
        format: Int,
        acess: Int,
        channels: Int,
        rate: Int,
        soft_resample: Int,
        latency: Int
    ): Int {
        return A2.snd_pcm_set_params(pcm.toCPointer(), format, acess, channels, rate, soft_resample, latency)
    }

    override fun snd_pcm_wait(pcm: Long, timeout: Int): Int {
        return A2.snd_pcm_wait(pcm.toCPointer(), timeout)
    }
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
    val snd_pcm_drop by func<(pcm: COpaquePointer?) -> Int>()
    val snd_pcm_delay by func<(pcm: COpaquePointer?, delay: COpaquePointer?) -> Int>()
    val snd_pcm_close by func<(pcm: COpaquePointer?) -> Int>()
    val snd_pcm_wait by func<(pcm: COpaquePointer?, timeout: Int) -> Int>()

    val snd_pcm_set_params by func<(
        pcm: COpaquePointer?,
        format: Int,
        acess: Int,
        channels: Int,
        rate: Int,
        softResample: Int,
        latency: Int
    ) -> Int>()

    val snd_pcm_recover by func<(pcm: COpaquePointer?, err: Int, silent: Int) -> Int>()
}
