package com.soywiz.korio.net

import com.soywiz.klogger.Console
import com.soywiz.korio.async.await
import kotlinx.cinterop.*
import platform.linux.inet_addr
import platform.posix.*
import kotlin.native.concurrent.*
import kotlin.reflect.KProperty

private object OSSL {
    //const val OPENSSL_INIT_ADD_ALL_CIPHERS = 4
    //const val OPENSSL_INIT_ADD_ALL_DIGESTS = 8
    const val OPENSSL_INIT_LOAD_CRYPTO_STRINGS = 2
    const val OPENSSL_INIT_LOAD_SSL_STRINGS = 2097152
    const val SSL_OP_NO_SSLv2 = 0
    const val LIBSSL_SO_FILE = "libssl.so.1.1"

    val handle: CPointer<out CPointed>? = dlopen(LIBSSL_SO_FILE, RTLD_LAZY)
    init {
        if (handle == null) {
            Console.error("Couldn't load '$LIBSSL_SO_FILE'")
        }
    }

    val isAvailable get() = handle != null

    fun close() {
        dlclose(handle)
    }

    class Func<T : Function<*>>(private val name: String? = null) {
        private var _set = AtomicInt(0)
        private var _value = AtomicReference<CPointer<CFunction<T>>?>(null)

        operator fun getValue(obj: Any?, property: KProperty<*>): CPointer<CFunction<T>> {
            if (_set.value == 0) {
                val rname = name ?: property.name
                _value.value = dlsym(handle, rname)?.reinterpret() ?: error("Can't find '$rname' in '$LIBSSL_SO_FILE'")
                _set.value = 1
            }
            return _value.value!!.reinterpret()
        }
    }

    val OPENSSL_init_ssl by Func<(ULong, COpaquePointer?) -> Int>()
    val TLSv1_2_method by Func<() -> COpaquePointer?>()
    val SSL_CTX_new by Func<(COpaquePointer?) -> COpaquePointer?>()
    val SSL_CTX_set_options by Func<(COpaquePointer?, ULong) -> ULong>()
    val SSL_write by Func<(COpaquePointer?, COpaquePointer?, Int) -> Int>()
    val SSL_read by Func<(COpaquePointer?, COpaquePointer?, Int) -> Int>()
    val SSL_new by Func<(COpaquePointer?) -> COpaquePointer?>()
    val SSL_free by Func<(COpaquePointer?) -> Int>()
    val SSL_get_fd by Func<(COpaquePointer?) -> Int>()
    val SSL_set_fd by Func<(COpaquePointer?, Int) -> Int>()
    val SSL_set1_host by Func<(COpaquePointer?, COpaquePointer?) -> Int>()
    val SSL_connect by Func<(COpaquePointer?) -> Int>()
    val SSL_CTX_free by Func<(COpaquePointer?) -> Int>()
}

class LinuxSSLSocket {
    companion object {
        init{
            OSSL.OPENSSL_init_ssl((OSSL.OPENSSL_INIT_LOAD_SSL_STRINGS or OSSL.OPENSSL_INIT_LOAD_CRYPTO_STRINGS).convert(), null)
            if (OSSL.OPENSSL_init_ssl(0.convert(), null) < 0) {
                Console.error("Could not initialize the OpenSSL library")
            }
        }
        val method = OSSL.TLSv1_2_method()
        val ctx = OSSL.SSL_CTX_new(method).also { ctx ->
            if (ctx == null) {
                Console.error("Unable to create a new SSL context structure")
            } else {
                OSSL.SSL_CTX_set_options(ctx, OSSL.SSL_OP_NO_SSLv2.convert())
            }
        }

        fun deinit() {
            OSSL.SSL_CTX_free(ctx)
            OSSL.close()
        }
    }

    private var ssl: COpaquePointer? = null

    val connected: Boolean get() = true
    var endpoint: NativeSocket.Endpoint = NativeSocket.Endpoint(NativeSocket.IP(0, 0, 0, 0), 0); private set

    private var worker: Worker? = null

    private data class ConnectRequest(val host: String, val port: Int)
    private data class ConnectResponse(val ssl: COpaquePointer?, val endpoint: NativeSocket.Endpoint)

    suspend fun connect(host: String, port: Int) {
        close()

        worker = Worker.start()

        val response = worker?.execute(TransferMode.SAFE, { ConnectRequest(host, port).freeze() }, { (host, port) ->
            memScoped {
                val ssl = OSSL.SSL_new(ctx) ?: error("Can't create a SSL context")
                var sockfd: Int = -1
                try {
                    val hostent = gethostbyname(host) ?: error("Can't get ip of '$host'")
                    val ip = hostent.pointed.h_addr_list?.get(0) ?: error("Can't get ip of '$host' (II)")
                    val endpoint = NativeSocket.Endpoint(NativeSocket.IP(ip), port)
                    sockfd = socket(AF_INET, SOCK_STREAM, 0)
                    if (sockfd < 0) error("Couldn't create INET STREAM socket")
                    val destAddr = alloc<sockaddr_in>()
                    destAddr.sin_family = AF_INET.convert()
                    destAddr.sin_port = htons(port.convert())
                    destAddr.sin_addr.s_addr = inet_addr(endpoint.ip.str)
                    val res = connect(sockfd, destAddr.ptr.reinterpret(), sizeOf<sockaddr_in>().convert())
                    if (res < 0) error("Cannot connect to $endpoint")
                    OSSL.SSL_set_fd(ssl, sockfd);
                    OSSL.SSL_set1_host(ssl, host.cstr.ptr)
                    val connectResult = OSSL.SSL_connect(ssl)
                    if (connectResult < 0) error("Cannot connect securely to $endpoint")
                    ConnectResponse(ssl, endpoint).freeze()
                } catch (e: Throwable) {
                    OSSL.SSL_free(ssl)
                    if (sockfd >= 0) close(sockfd)
                    throw e
                }
            }
        })?.await()

        if (response != null) {
            ssl = response.ssl
            endpoint = response.endpoint
        }
    }

    suspend fun close() {
        worker?.requestTermination(processScheduledJobs = true)?.await()
        worker = null
        if (ssl != null) {
            OSSL.SSL_free(ssl)
            close(OSSL.SSL_get_fd(ssl))
            ssl = null
        }
    }

    private data class TransferRequest(val ssl: COpaquePointer?, val ptr: COpaquePointer?, val size: Int)

    suspend fun write(data: ByteArray, offset: Int = 0, size: Int = data.size - offset): Int {
        if (size <= 0) return 0
        val worker = this.worker ?: return -1
        return data.usePinned {
            it.addressOf(offset)
            worker.execute(TransferMode.SAFE, { TransferRequest(ssl, it.addressOf(offset), size) }) { (ssl, ptr, size) ->
                OSSL.SSL_write(ssl, ptr, size.convert()).also {
                    if (it < 0) error("Error writing SSL : $it")
                }
            }.await()
        }
    }

    suspend fun read(size: Int): ByteArray = ByteArray(size).also { it.copyOf(read(it)) }

    suspend fun read(data: ByteArray, offset: Int = 0, size: Int = data.size - offset): Int {
        if (size <= 0) return 0
        val worker = this.worker ?: return -1
        return data.usePinned {
            it.addressOf(offset)
            worker.execute(TransferMode.SAFE, { TransferRequest(ssl, it.addressOf(offset), size) }) { (ssl, ptr, size) ->
                OSSL.SSL_read(ssl, ptr, size.convert()).also {
                    if (it < 0) error("Error reading SSL : $it")
                }
            }.await()
        }
    }

}

/*
fun main() {
    val socket = LinuxSSLSocket()
    socket.connect("google.es", 443)
    println("write: " + socket.write("GET / HTTP/1.0\r\nHost: google.es\r\n\r\n".encodeToByteArray()))
    println("read: " + socket.read(1024).decodeToString())
}
*/
