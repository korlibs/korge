package korlibs.io.net.ssl

import korlibs.datastructure.*
import java.nio.*
import javax.net.ssl.*

actual fun DefaultSSLProcessor(): SSLProcessor = SSLProcessorJvm()

class SSLProcessorJvm : SSLProcessor {
    override var isAlive: Boolean = true

    val sslContext = SSLContext.getInstance("TLS").also {
        it.init(null, null, null)
    }
    var engine = sslContext.createSSLEngine().apply {
        useClientMode = true
    }

    override fun setEndPoint(host: String, port: Int) {
        engine = sslContext.createSSLEngine(host, port)
        engine.useClientMode = true
        engine.beginHandshake()
    }

    private val encryptedS2C = ByteArrayDeque()
    private val encryptedC2S = ByteArrayDeque()

    private val plainS2C = ByteArrayDeque()
    private val plainC2S = ByteArrayDeque()

    override fun addEncryptedServerData(data: ByteArray, offset: Int, size: Int) {
        synchronized(encryptedS2C) {
            encryptedS2C.write(data, offset, size)
        }
    }
    override fun addDecryptedClientData(data: ByteArray, offset: Int, size: Int) {
        plainC2S.write(data, offset, size)
    }

    override fun getDecryptedServerData(data: ByteArray, offset: Int, size: Int): Int {
        sync()
        return plainS2C.read(data, offset, size)
    }
    override fun getEncryptedClientData(data: ByteArray, offset: Int, size: Int): Int {
        sync()
        return encryptedC2S.read(data, offset, size)
    }

    override fun clientClose() {
        engine.closeOutbound()
    }

    private val temp1 = ByteArray(engine.session.applicationBufferSize)
    private var temp2 = ByteArray(engine.session.packetBufferSize)

    override val needInput: Boolean get() {
        val status = engine.handshakeStatus
        //println("needInput.engine.handshakeStatus=${engine.handshakeStatus}")
        return status != SSLEngineResult.HandshakeStatus.FINISHED && status != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING
    }

    override val needSync: Boolean get() {
        val status = engine.handshakeStatus
        //println("needInput.engine.handshakeStatus=${engine.handshakeStatus}")
        return status == SSLEngineResult.HandshakeStatus.NEED_WRAP
    }

    override val status: Any? get() = engine.handshakeStatus

    private fun sync() {
        while (true) {
            val handshakeStatus = engine.handshakeStatus
            //println("handshakeStatus=$handshakeStatus, encryptedS2C=${encryptedS2C.availableRead}, encryptedC2S=${encryptedC2S.availableRead}, plainS2C=${plainS2C.availableRead}, plainC2S=${plainC2S.availableRead}")
            when (handshakeStatus) {
                null -> TODO()
                SSLEngineResult.HandshakeStatus.NEED_TASK -> {
                    while (true) {
                        val task = engine.delegatedTask
                        //println("RUN TASK: $task")
                        try {
                            task?.run()
                            // @TODO: THIS IS BLOCKING. WE SHOULD RUN IT IN A SUSPEND FUNCTION
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                        //println("/RUN TASK: $task")
                        if (task == null) break
                    }
                }
                SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING, SSLEngineResult.HandshakeStatus.FINISHED -> {
                    sync(plainC2S, encryptedC2S, wrap = true)
                    sync(encryptedS2C, plainS2C, wrap = false)
                    break
                }
                SSLEngineResult.HandshakeStatus.NEED_WRAP -> {
                    sync(plainC2S, encryptedC2S, wrap = true)
                }
                // @TODO: NEED_UNWRAP_AGAIN not available on Android
                //SSLEngineResult.HandshakeStatus.NEED_UNWRAP, SSLEngineResult.HandshakeStatus.NEED_UNWRAP_AGAIN -> {
                else -> {
                    if (sync(encryptedS2C, plainS2C, wrap = false) == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
                        break
                    }
                }
            }
        }

        /*
        println("engine.handshakeStatus=${engine.handshakeStatus}, encryptedS2C=${encryptedS2C.availableRead}, encryptedC2S=${encryptedC2S.availableRead}, plainS2C=${plainS2C.availableRead}, plainC2S=${plainC2S.availableRead}")
        sync(plainC2S, encryptedC2S, wrap = true)
        sync(encryptedS2C, plainS2C, wrap = false)

        println("engine.handshakeStatus=${engine.handshakeStatus}, encryptedS2C=${encryptedS2C.availableRead}, encryptedC2S=${encryptedC2S.availableRead}, plainS2C=${plainS2C.availableRead}, plainC2S=${plainC2S.availableRead}")
         */
    }

    private fun sync(input: ByteArrayDeque, output: ByteArrayDeque, wrap: Boolean): SSLEngineResult.Status {
        while (true) {
            val temp1Count = input.peek(temp1)
            val inp = ByteBuffer.wrap(temp1, 0, temp1Count.coerceAtLeast(0))
            val out = ByteBuffer.wrap(temp2, 0, temp2.size)
            //println("temp1Count=$temp1Count, temp2.size=${temp2.size} wrap=$wrap, inp.limit()=${inp.limit()}, out.limit()=${out.limit()}")
            val result = if (wrap) engine.wrap(inp, out) else engine.unwrap(inp, out)
            //println("temp1Count=$temp1Count, temp2.size=${temp2.size} wrap=$wrap, status=${result.status}, handShake=${result.handshakeStatus}, result.bytesConsumed()=${result.bytesConsumed()}, result.bytesProduced()=${result.bytesProduced()}")
            input.skip(result.bytesConsumed())
            output.write(temp2, 0, result.bytesProduced())
            result.bytesProduced()
            when (result.status) {
                null, SSLEngineResult.Status.OK -> Unit
                SSLEngineResult.Status.BUFFER_UNDERFLOW -> {
                    if (wrap) error("Expected BUFFER_UNDERFLOW in SSL")
                }
                SSLEngineResult.Status.BUFFER_OVERFLOW -> {
                    temp2 = ByteArray(temp2.size * 2)
                    continue
                }
                SSLEngineResult.Status.CLOSED -> isAlive = false
            }
            if (temp1Count <= 0 || (result.bytesConsumed() == 0 && result.bytesProduced() == 0)) return result.status
        }
    }
}
