package korlibs.audio.sound.backends

import korlibs.memory.dyn.*
import com.sun.jna.*
import java.util.concurrent.*
import kotlin.collections.set

actual object ASoundImpl : ASound2 {
    fun Long.toCPointer(): Pointer = Pointer.createConstant(this)
    fun String?.toKString(): String = this ?: ""

    override val initialized: Boolean get() = true

    private val paramsAlloc = ConcurrentHashMap<Long, Memory>()

    override fun alloc_params(): Long {
        val params = Memory(1024).also { it.clear() }
        paramsAlloc[params.address] = params
        return params.address
    }
    override fun free_params(value: Long) {
        val memory = paramsAlloc.remove(value)
        memory?.clear()
    }

    override fun snd_pcm_open(name: String, stream: Int, mode: Int): Long {
        val memory = Memory(16L).also { it.clear() }
        A2.snd_pcm_open(memory, name, stream, mode)
        return memory.getPointer(0L)?.address ?: 0L
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
        val tempOut = Memory(4).also { it.clear() }
        val tempDir = Memory(4).also { it.clear() }
        A2.snd_pcm_hw_params_get_period_size(params.toCPointer(), tempOut, tempDir)
        return tempOut.getInt(0L)
    }

    override fun snd_pcm_delay(pcm: Long): Int {
        val tempDelay = Memory(4).also { it.clear() }
        A2.snd_pcm_delay(pcm.toCPointer(), tempDelay)
        return tempDelay.getInt(0L)
    }

    override fun snd_pcm_writei(pcm: Long, buffer: ShortArray, size: Int): Int {
        val mem = Memory((buffer.size * 2).toLong()).also { it.clear() }
        for (n in 0 until buffer.size) mem.setShort((n * 2).toLong(), buffer[n])
        return A2.snd_pcm_writei(pcm.toCPointer(), mem, size)
    }

    override fun snd_pcm_prepare(pcm: Long): Int = A2.snd_pcm_prepare(pcm.toCPointer())
    override fun snd_pcm_drop(pcm: Long): Int = A2.snd_pcm_drop(pcm.toCPointer())
    override fun snd_pcm_drain(pcm: Long): Int = A2.snd_pcm_drain(pcm.toCPointer())
    override fun snd_pcm_close(pcm: Long): Int = A2.snd_pcm_close(pcm.toCPointer())
}

object A2 {
    @JvmStatic external fun snd_pcm_open(pcmPtr: Pointer?, name: String?, stream: Int, mode: Int): Int
    @JvmStatic external fun snd_pcm_hw_params_any(pcm: Pointer?, params: Pointer?): Int
    @JvmStatic external fun snd_pcm_hw_params_set_access(pcm: Pointer?, params: Pointer?, access: Int): Int
    @JvmStatic external fun snd_pcm_hw_params_set_format(pcm: Pointer?, params: Pointer?, format: Int): Int
    @JvmStatic external fun snd_pcm_hw_params_set_channels(pcm: Pointer?, params: Pointer?, channels: Int): Int
    @JvmStatic external fun snd_pcm_hw_params_set_rate(pcm: Pointer?, params: Pointer?, rate: Int, dir: Int): Int
    @JvmStatic external fun snd_pcm_hw_params(pcm: Pointer?, params: Pointer?): Int
    @JvmStatic external fun snd_pcm_name(pcm: Pointer?): String?
    @JvmStatic external fun snd_pcm_state(pcm: Pointer?): Int
    @JvmStatic external fun snd_pcm_state_name(state: Int): String?
    @JvmStatic external fun snd_pcm_hw_params_get_channels(params: Pointer?, out: Pointer?): Int
    @JvmStatic external fun snd_pcm_hw_params_get_rate(params: Pointer?, value: Pointer?, dir: Pointer?): Int
    @JvmStatic external fun snd_pcm_hw_params_get_period_size(params: Pointer?, value: Pointer?, dir: Pointer?): Int
    @JvmStatic external fun snd_pcm_hw_params_get_period_time(params: Pointer?, value: Pointer?, dir: Pointer?): Int
    @JvmStatic external fun snd_pcm_writei(pcm: Pointer?, buffer: Pointer?, size: Int): Int
    @JvmStatic external fun snd_pcm_prepare(pcm: Pointer?): Int
    @JvmStatic external fun snd_pcm_drain(pcm: Pointer?): Int
    @JvmStatic external fun snd_pcm_drop(pcm: Pointer?): Int
    @JvmStatic external fun snd_pcm_delay(pcm: Pointer?, delay: Pointer?): Int
    @JvmStatic external fun snd_pcm_close(pcm: Pointer?): Int

    init {
        Native.register("libasound.so.2")
    }
}
