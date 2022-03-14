package com.soywiz.korio.net

import com.soywiz.korio.util.*
import com.soywiz.krypto.encoding.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.posix.*
import platform.posix.AF_INET
import platform.posix.SOCK_STREAM
import platform.windows.*
import platform.windows.WSAGetLastError
import platform.windows.WSAStartup
import win32ssl.*


class NativeSocket private constructor(
    internal val sockfd: SOCKET,
    endpoint: Endpoint,
    val secure: Boolean,
    val debug: Boolean
) {
    var endpoint: Endpoint = endpoint; private set

	companion object {
        const val DEBUG_SOCKET = false

		init {
            InitCommonControls()
            //OleInitialize(null) // @TODO: C:\Users\soywi\.konan\dependencies\msys2-mingw-w64-x86_64-clang-llvm-lld-compiler_rt-8.0.1\bin\ld: C:\Users\soywi\AppData\Local\Temp\konan_temp15231878231369805640\result.o:out:(.text+0xaf7403): referencia a `__imp_OleInitialize' sin definir  //clang++: error: linker command failed with exit code 1 (use -v to see invocation)

			init_sockets()
            // @TODO: Is WSAStartup already called by init_sockets?
            memScoped {
                val wsaData = alloc<WSAData>()
                WSAStartup(0x0202.convert(), wsaData.ptr)
            }
        }

        init {
            //if (debug) println("Before loading Fwpuclnt.dll")
        }

        operator fun invoke(secure: Boolean): NativeSocket {
            //if (secure) TODO("Secure sockets not implemented on Kotlin/Native Windows")
            //val debug = secure
            if (secure) {
                println("WARNING: secure NativeSocket in win32 is experimental. Please, report the bugs you found.")
            }
            val debug = DEBUG_SOCKET
            if (debug) println("NativeSocket.secure=$secure")
            val socket = WSASocket!!(platform.windows.AF_INET, platform.windows.SOCK_STREAM, platform.windows.IPPROTO_TCP, null, 0u, 0u)
            checkErrors("WSASocket")
			//val socket = platform.windows.socket(platform.windows.AF_INET, platform.windows.SOCK_STREAM, platform.windows.IPPROTO_TCP)
            val ssl = if (secure) SSL_alloc() else null
			return NativeSocket(socket, Endpoint(IP(0, 0, 0, 0), 0), secure, debug = debug)
		}
		suspend fun connect(host: String, port: Int, secure: Boolean = false) = NativeSocket(secure).apply { connect(host, port) }
		suspend fun bound(host: String, port: Int, secure: Boolean = false) = NativeSocket(secure).apply { bind(host, port) }
		//suspend fun listen(host: String, port: Int) = NativeSocket().listen(host, port)

		fun checkErrors(name: String = "") {
			val error = WSAGetLastError()
			if (error != 0) {
                val errorStr = GetErrorAsString(error.convert())
                error("WSA error($name): $error :: $errorStr")
			}
		}
	}

	data class Endpoint(val ip: IP, val port: Int) {
		override fun toString(): String = "$ip:$port"
        fun toAsyncAddress(): AsyncAddress = AsyncAddress(ip.str, port)
	}

	class IP(val data: UByteArray) {
        constructor(v0: Int, v1: Int, v2: Int, v3: Int) : this(ubyteArrayOf(v0.toUByte(), v1.toUByte(), v2.toUByte(), v3.toUByte()))

		val v0 get() = data[0]
		val v1 get() = data[1]
		val v2 get() = data[2]
		val v3 get() = data[3]
		val str get() = "$v0.$v1.$v2.$v3"
		val value: Int get() = (v0.toInt() shl 0) or (v1.toInt() shl 8) or (v2.toInt() shl 16) or (v3.toInt() shl 24)
		//val value: Int get() = (v0.toInt() shl 24) or (v1.toInt() shl 16) or (v2.toInt() shl 8) or (v3.toInt() shl 0)
		override fun toString(): String = str

		companion object {
			fun fromHost(host: String): IP {
				memScoped {
					// gethostbyname unusable on windows
					val addr = allocArray<LPADDRINFOVar>(1)
                    val hints = alloc<addrinfo>()
                    hints.ai_family = AF_INET
                    hints.ai_flags = AI_PASSIVE
                    hints.ai_socktype = SOCK_STREAM
					val res = getaddrinfo(host, null, hints.ptr, addr)
					checkErrors("getaddrinfo")
					val info = addr[0]!!.pointed
                    val ad = info.ai_addr!!.reinterpret<sockaddr_in>().pointed.sin_addr.S_un.S_un_b
					return IP(ad.s_b1.toInt(), ad.s_b2.toInt(), ad.s_b3.toInt(), ad.s_b4.toInt())
				}
			}
		}
	}

	fun CPointer<sockaddr_in>.set(ip: IP, port: Int) {
		val addr = this
        addr.pointed.also { p ->
            p.sin_family = platform.windows.AF_INET.convert()
            //p.sin_addr.S_un.S_addr = ip.value.toUInt()
            //println("IP: $ip")
            p.sin_addr.S_un.S_un_b.s_b1 = ip.v0
            p.sin_addr.S_un.S_un_b.s_b2 = ip.v1
            p.sin_addr.S_un.S_un_b.s_b3 = ip.v2
            p.sin_addr.S_un.S_un_b.s_b4 = ip.v3
            p.sin_port = swapBytes(port.toUShort())
        }
        //inet_pton(AF_INET.convert(), ip.str, )
	}

	val connected get() = _connected

    private val arena = Arena()

    var ssl: CPointer<SSL_SOCKET>? = null

    private inline fun debug(message: () -> String) {
        if (debug) println(message())
    }

	fun connect(host: String, port: Int) {
		memScoped {
            doClose()
			val ip = IP.fromHost(host)

            debug { "Before WSAConnect: host=$host, ip=$ip, port=$port" }
			//val connected = platform.windows.connect(sockfd, addr.reinterpret(), sockaddr_in.size.convert())
            val addr: CPointer<sockaddr_in> = allocArray<sockaddr_in>(1)
            addr.set(ip, port)

            debug { "sockaddr_in: ${addr.getBytes(sizeOf<sockaddr_in>().toInt()).hex}" }
            val connected = WSAConnect(sockfd, addr.reinterpret(), sizeOf<sockaddr_in>().convert(), null, null, null, null)
            debug { "After WSAConnect" }
			checkErrors("connect")
			endpoint = Endpoint(ip, port)
			setSocketBlockingEnabled(false)
            if (secure) {
                ssl = SSL_alloc()
                SSL_setDestinationName(ssl, host.cstr)
                SSL_setDebug(ssl, if (debug) 1 else 0)
            }

			if (connected != 0) {
                doClose()
				error("Can't connect to $ip:$port ('$host')")
			}
			_connected = true
            debug { "Connected = true" }
		}
	}

	fun bind(host: String, port: Int, backlog: Int = 10) {
		memScoped {
            if (secure) error("Server secure socket not implemented on win32 yet")
			val ip = IP.fromHost(host)
			val addr = alloc<sockaddr_in>()
			addr.ptr.set(ip, port)
            //println("Binding: $host")
            checkErrors("pbind")
			platform.posix.bind(sockfd, addr.ptr.reinterpret(), sockaddr_in.size.convert())
			checkErrors("bind")
			platform.posix.listen(sockfd, backlog)
			checkErrors("listen")
			setSocketBlockingEnabled(false)
		}
	}

	fun CPointer<sockaddr_in>.toEndpoint(): Endpoint {
		return Endpoint(
			IP(this.pointed.sin_addr.readValue().getBytes().toUByteArray()),
			swapBytes(this.pointed.sin_port.toUShort()).toInt()
		)
	}

    fun wouldBlock() = WSAGetLastError() == platform.windows.WSAEWOULDBLOCK

	fun tryAccept(): NativeSocket? {
		return memScoped {
			val addr = alloc<sockaddr>()
			val socklen = alloc<platform.windows.socklen_tVar>()
			socklen.value = sockaddr.size.convert()
			val fd = platform.posix.accept(sockfd, addr.ptr, socklen.ptr)
            if (wouldBlock()) {
                null
            } else {
                checkErrors("accept")
                if (fd.toInt() < 0) {
                    val errno = posix_errno()
                    //println("accept: fd=$fd, errno=$errno")
                    when (errno) {
                        EWOULDBLOCK -> return null
                        else -> error("Couldn't accept socket ($fd) errno=$errno")
                    }
                }
                //println("accept: fd=$fd")
                NativeSocket(fd, addr.ptr.reinterpret<sockaddr_in>().toEndpoint(), secure, debug = secure).apply {
                    setSocketBlockingEnabled(false)
                }
            }
		}
	}

	val availableBytes: Int
		get() {
			val bytes_available = uintArrayOf(0u, 0u)
			//platform.windows.ioctlsocket(sockfd, platform.windows.FIONREAD, bytes_available.refTo(0).reinterpret())
			platform.windows.ioctlsocket(sockfd, platform.windows.FIONREAD, bytes_available.refTo(0))
			checkErrors("ioctlsocket")
			return bytes_available[0].toInt()
		}

	//val connected: Boolean
	//    get() {
	//        memScoped {
	//            if (!_connected) return false
	//            val errorPtr = allocArray<IntVar>(1)
	//            val lenPtr = longArrayOf(IntVar.size.convert())
	//            val retval = getsockopt(sockfd, SOL_SOCKET, SO_ERROR, errorPtr, lenPtr.refTo(0).uncheckedCast())
	//            return (retval == 0 || errorPtr[0] == 0)
	//        }
	//    }

	private var _connected = false
    private fun doClose() {
        _connected = false
        arena.clear()
        if (ssl != null) {
            SSL_free(ssl)
            ssl = null
        }
    }

    // @TODO: This looks like a Kotlin bug
    @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "UNUSED_VALUE")
    private fun SSL_process(kind: String): Boolean {
        memScoped {
            val BUFFER_SIZE = 10240
            val c = allocArray<ByteVar>(BUFFER_SIZE)
            debug { "SSL_process[$kind][0]" }
            do {
                var retry = false
                SSL_process(ssl)

                while (true) {
                    val toWriteCount = SSL_outDequeue(ssl, c, BUFFER_SIZE)
                    if (toWriteCount > 0) {
                        debug { "---send($toWriteCount)" }
                        var pos = 0
                        while (pos < toWriteCount) {
                            val sent: Int = send(sockfd, (c + pos), toWriteCount - pos, 0)
                            if (sent <= 0) break
                            pos += sent
                        }
                        retry = true
                        //send(s, c, toWriteCount, 0);
                    } else {
                        break
                    }
                }

                while (true) {
                    val readCount: Int = platform.windows.recv(sockfd, c, BUFFER_SIZE, 0)
                    //printf("-->%d, errno=%d\n", readCount, errno);
                    if (readCount <= 0 && !wouldBlock()) {
                        SSL_process(ssl)
                        return true
                    }
                    if (readCount > 0) {
                        debug { "---recv($readCount)" }
                        SSL_writeReceived(ssl, c, readCount)
                        retry = true
                    } else {
                        break;
                    }
                }
            } while (retry)
        }
        return false
    }

	fun recv(data: ByteArray, offset: Int = 0, count: Int = data.size - offset): Int {
        if (ssl != null) {
            val processRes = SSL_process("recv")
            val res = SSL_inDequeue(ssl, data.refTo(offset), count.convert())
            debug { "recv.SSL_inDequeue processRes=$processRes, res=$res, offset=$offset, count=$count" }
            if (res > 0) return res
            return if (processRes) -1 else 0
        }
        val result = platform.windows.recv(sockfd, data.refTo(offset), count.convert(), 0).toInt()
        if (wouldBlock()) return 0
        checkErrors("recv")
        return result
	}

	fun recv(count: Int): ByteArray {
		val data = ByteArray(count)
		val len = recv(data)
		if (len < 0) {
            doClose()
			error("Socket read error")
		}
		return data.copyOf(len.convert())
	}

	fun tryRecv(data: ByteArray, offset: Int = 0, count: Int = data.size - offset): Int {
		//if (availableBytes <= 0) return -1
        if (availableBytes < 0) return -1
		return recv(data, offset, count)
	}

	fun tryRecv(count: Int): ByteArray? {
		//if (availableBytes <= 0) return null
        if (availableBytes < 0) return null
		return recv(count)
	}

	fun send(data: ByteArray, offset: Int = 0, count: Int = data.size - offset) {
		if (count <= 0) return

        if (ssl != null) {
            SSL_process("send0")
            SSL_writeToSend(ssl, data.refTo(offset), count.convert())
            SSL_process("send1")
        } else {
            //val result = platform.windows.send(sockfd, data.refTo(offset), count.convert(), 0)
            val result = platform.posix.send(sockfd, data.refTo(offset), count.convert(), 0)
            checkErrors("send")
            if (result < count) {
                doClose()
                error("Socket write error")
            }
        }
	}

	suspend fun close() {
        if (ssl != null) {
            SSL_close(ssl)
            SSL_process("close")
        }
		platform.windows.closesocket(sockfd)
		checkErrors("closesocket")
		//platform.posix.shutdown(sockfd, SHUT_RDWR)
        doClose()
	}

	private fun setSocketBlockingEnabled(blocking: Boolean): Boolean {
		if (sockfd.toInt() < 0) return false
		memScoped {
			val mode = alloc<u_longVar>()
			mode.value = if (blocking) 0.convert() else 1.convert()
			val result = (platform.windows.ioctlsocket(sockfd, platform.windows.FIONBIO.convert(), mode.ptr) == 0)
			checkErrors("ioctlsocket")
			return result
		}
	}

	fun getLocalEndpoint(): Endpoint {
		memScoped {
			val localAddress = alloc<sockaddr_in>()
			val addressLength = alloc<platform.windows.socklen_tVar>()
			addressLength.value = sockaddr_in.size.convert()
			val result = platform.windows.getsockname(sockfd, localAddress.ptr.reinterpret(), addressLength.ptr)
			checkErrors("getsockname")
			if (result < 0) return Endpoint(IP(0, 0, 0, 0), 0)
			val ip = localAddress.sin_addr.readValue()
			val port = swapBytes(localAddress.sin_port)
			//println("result: $result")
			//println("local address: " + inet_ntoa(localAddress.sin_addr.readValue())?.toKString())
			//println("local port: " + )
			return Endpoint(IP(ip.getBytes().toUByteArray()), port.toInt())
		}
	}

	fun getRemoveEndpoint() = endpoint

	private fun swapBytes(v: UShort): UShort =
		(((v.toInt() and 0xFF) shl 8) or ((v.toInt() ushr 8) and 0xFF)).toUShort()

	override fun toString(): String = "NativeSocket(local=${getLocalEndpoint()}, remote=${getRemoveEndpoint()})"
}

private fun CPointer<*>.getBytes(size: Int): ByteArray {
    val bytes = reinterpret<ByteVar>()
    return ByteArray(size) { bytes[it] }
}

private fun CPointer<wchar_tVar>.writeWString(str: String) {
    for (n in 0 until str.length) this[n] = str[n].code.convert()
}

private fun CPointer<wchar_tVar>.writeWStringz(str: String) {
    writeWString(str)
    this[str.length] = 0.convert()
}

suspend fun NativeSocket.suspendRecvUpTo(data: ByteArray, offset: Int = 0, count: Int = data.size - offset): Int {
	if (count <= 0) return count

    var time = 0L
	while (true) {
		val read = tryRecv(data, offset, count)
        when {
            read < 0 -> {
                //error("Socket error suspendRecvUpTo: read=$read")
                return -1
            }
            read == 0 -> {
                delay(time)
                time = (time + 1).coerceAtMost(10L)
                continue
            }
            else -> return read
        }
	}
}

suspend fun NativeSocket.suspendRecvExact(data: ByteArray, offset: Int = 0, count: Int = data.size - offset): Int {
	var position = offset
	var remaining = count
	while (true) {
		if (remaining <= 0) return count
		val read = suspendRecvUpTo(data, position, remaining)
        if (read < 0) error("suspendRecvExact EOF before reading completely")
		remaining -= read
		position += read
	}
}

suspend fun NativeSocket.suspendRecvExact(count: Int): ByteArray {
	return ByteArray(count).apply { suspendRecvExact(this) }
}

suspend fun NativeSocket.suspendRecvUpTo(count: Int): ByteArray {
	val out = ByteArray(count)
	val result = suspendRecvUpTo(out)
	return out.copyOf(result)
}

suspend fun NativeSocket.suspendSend(data: ByteArray, offset: Int = 0, count: Int = data.size - offset) {
	send(data, offset, count)
}

suspend fun NativeSocket.accept(): NativeSocket {
    var time = 0L
	while (true) {
		val socket = tryAccept()
		//println("suspendAccept: $socket")
		if (socket != null) return socket
		delay(time)
        time = (time + 1).coerceAtMost(10L)
	}
}

internal actual val asyncSocketFactory: AsyncSocketFactory = NativeAsyncSocketFactory

object NativeAsyncSocketFactory : AsyncSocketFactory() {
	class NativeAsyncClient(val socket: NativeSocket) : AsyncClient {
        override val address: AsyncAddress get() = socket.endpoint.toAsyncAddress()

        override suspend fun connect(host: String, port: Int) { socket.connect(host, port) }
		override val connected: Boolean get() = socket.connected
		override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int = socket.suspendRecvUpTo(buffer, offset, len)
		override suspend fun write(buffer: ByteArray, offset: Int, len: Int) = socket.suspendSend(buffer, offset, len)
		override suspend fun close() = socket.close()
	}

	class NativeAsyncServer(val socket: NativeSocket, override val requestPort: Int, override val backlog: Int) :
		AsyncServer {
		override val host: String get() = socket.getLocalEndpoint().ip.str
		override val port: Int get() = socket.getLocalEndpoint().port
		override suspend fun accept(): AsyncClient = NativeAsyncClient(socket.accept())
        override suspend fun close() = socket.close()
    }

	override suspend fun createClient(secure: Boolean): AsyncClient {
        return NativeAsyncClient(NativeSocket(secure))
    }

	override suspend fun createServer(port: Int, host: String, backlog: Int, secure: Boolean): AsyncServer {
		val socket = NativeSocket(secure)
		socket.bind(host, port, backlog)
		return NativeAsyncServer(socket, port, backlog)
	}
}

