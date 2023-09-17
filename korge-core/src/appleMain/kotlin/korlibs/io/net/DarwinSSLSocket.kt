package korlibs.io.net

import cnames.structs.SSLContext
import korlibs.io.async.*
import korlibs.io.posix.*
import kotlinx.cinterop.*
import kotlinx.cinterop.ByteVar
import kotlinx.coroutines.*
import platform.CoreFoundation.*
import platform.Security.*
import platform.darwin.*
import platform.posix.*
import platform.posix.sockaddr_in
import kotlin.ByteArray
import kotlin.Int
import kotlin.String
import kotlin.TODO
import kotlin.UByte
import kotlin.UShort
import kotlin.error
import kotlin.native.concurrent.*
import kotlin.toUShort

class DarwinSSLSocket {
    val arena = Arena()
    var sockfd: Int = -1
    var ctx: CPointer<SSLContext>? = null
    var endpoint: NativeSocket.Endpoint = NativeSocket.Endpoint(NativeSocket.IP(0, 0, 0, 0), 0); private set

    suspend fun connect(host: String, port: Int) {
        close()
        val socketVar = arena.alloc<LongVar>()
        ctx = SSLCreateContext(null, SSLProtocolSide.kSSLClientSide, SSLConnectionType.kSSLStreamType)

        withContext(Dispatchers.CIO) {
            memScoped {
                val sockfd = socket(AF_INET, SOCK_STREAM, 0)
                val timeout = alloc<timeval>()
                timeout.tv_sec = 10     // seconds
                timeout.tv_usec = 500000 // micro seconds ( 0.5 seconds)
                setsockopt(sockfd, SOL_SOCKET, SO_RCVTIMEO, timeout.ptr, sizeOf<timeval>().convert())
                setsockopt(sockfd, SOL_SOCKET, SO_SNDTIMEO, timeout.ptr, sizeOf<timeval>().convert())
                //fcntl(sockfd, F_SETFL, O_NONBLOCK)

                socketVar.value = sockfd.convert()
                SSLSetConnection(ctx, socketVar.ptr)

                SSLSetIOFuncs(ctx, staticCFunction(::SSL_recv_callback), staticCFunction(::SSL_send_callback))
                SSLSetPeerDomainName(ctx, host)

                //println("Socket...")
                val hname = gethostbyname(host)
                //println("hname=$hname")
                val inetaddr: CPointer<UByteVarOf<UByte>> = hname!!.pointed.h_addr_list!![0]!!.reinterpret()

                val endpoint = NativeSocket.Endpoint(
                    NativeSocket.IP(inetaddr[0].toInt(), inetaddr[1].toInt(), inetaddr[2].toInt(), inetaddr[3].toInt()),
                    port
                )

                val servaddr = alloc<sockaddr_in>()
                servaddr.sin_family = AF_INET.convert()
                //println("addr=$addr")
                servaddr.sin_addr.s_addr = inet_addr(endpoint.ip.str)
                servaddr.sin_port = swapBytes(endpoint.port.toUShort()).convert()

                //println("Connecting...")
                val result = connect(sockfd, servaddr.ptr.reinterpret(), sizeOf<sockaddr_in>().convert())

                /*
                if (errno != EINPROGRESS) {
                    error("Error connecting to socket errno=$errno, EINPROGRESS=$EINPROGRESS")
                } else {
                    loop@while (true) {
                        val rc = memScoped {
                            val timeout = alloc<timeval>()
                            val writeFDs = alloc<fd_set>()
                            timeout.tv_sec = 0
                            timeout.tv_usec = 1000
                            __darwin_fd_set(sockfd, writeFDs.ptr)
                            select(1, writeFDs.ptr, writeFDs.ptr, writeFDs.ptr, timeout.ptr)
                        }
                        if (rc == 0 || rc == -1) {
                            println(" Timed out -- Not connected even after 3 secs wait")
                        } else {
                            println(" connected and written")
                            break@loop
                        }
                    }
                }
                */

                //println("connected: $result, sockfd=$sockfd, errno=$errno")

                if (result != 0) error("Error connecting to socket result=$result, sockfd=$sockfd, errno=$errno")
                this@DarwinSSLSocket.sockfd = sockfd
                this@DarwinSSLSocket.endpoint = endpoint
            }
        }
    }

    val connected: Boolean get() {
        if (sockfd < 0 || ctx == null) return false
        return when (SSLGetSessionState(ctx)) {
            SSLSessionState.kSSLClosed, SSLSessionState.kSSLAborted -> false
            else -> ioctlSocketFionRead(sockfd) >= 0
        }
    }

    suspend fun write(data: ByteArray, offset: Int = 0, size: Int = data.size - offset) {
        SSLWrite(ctx, data, offset, size)
    }

    suspend fun read(data: ByteArray, offset: Int = 0, size: Int = data.size - offset): Int {
        return SSLRead(ctx, data, offset, size)
    }

    suspend fun read(size: Int): ByteArray {
        val out = ByteArray(size)
        return out.copyOf(read(out))
    }

    suspend fun close() {
        if (ctx != null) SSLClose(ctx)
        if (sockfd >= 0) close(sockfd)
        ctx = null
        sockfd = -1
        arena.clear()
    }

    companion object {
        private fun SSLSetPeerDomainName(ctx: SSLContextRef?, name: String) {
            val status = SSLSetPeerDomainName(ctx, name, name.length.convert())
            //println("SSLSetPeerDomainName: " + SecCopyErrorMessageString(status, null)?.toKString())
        }

        private fun SSLGetSessionState(ctx: SSLContextRef?): SSLSessionState = memScoped {
            val state = alloc<SSLSessionState.Var>()
            SSLGetSessionState(ctx, state.ptr)
            state.value
        }

        private fun swapBytes(v: UShort): UShort =
            (((v.toInt() and 0xFF) shl 8) or ((v.toInt() ushr 8) and 0xFF)).toUShort()

        private suspend fun SSLEnsure(ctx: SSLContextRef?): Boolean {
            while (true) {
                val state = SSLGetSessionState(ctx)
                //println("state=$state")
                when (state) {
                    SSLSessionState.kSSLIdle -> SSLHandshake(ctx)
                    SSLSessionState.kSSLHandshake -> {
                        memScoped {
                            val data = allocArray<ByteVar>(0)
                            val processed = alloc<size_tVar>()
                            SSLWrite(ctx, data, 0.convert(), processed.ptr)
                        }
                        //SSLHandshake(ctx)
                        kotlinx.coroutines.delay(1L)
                    }
                    SSLSessionState.kSSLClosed -> return false
                    SSLSessionState.kSSLAborted -> return false
                    SSLSessionState.kSSLConnected -> break
                    else -> Unit
                }
            }
            return true
            //println("state=${SSLGetSessionState(ctx)}")
        }

        private suspend fun SSLRead(
            ctx: SSLContextRef?,
            data: ByteArray,
            offset: Int = 0,
            size: Int = data.size - offset
        ): Int {
            if (data.isEmpty() || size == 0) return 0
            if (!SSLEnsure(ctx)) return -1

            memScoped {
                val processed = alloc<size_tVar>()

                while (true) {
                    val result = data.usePinned { dataPin ->
                        SSLRead(ctx, dataPin.addressOf(offset), size.convert(), processed.ptr)
                    }

                    when (result) {
                        0 -> {
                            return processed.value.toInt()
                        }
                        errSSLWouldBlock -> {
                            kotlinx.coroutines.delay(1L)
                            continue
                        }
                        errSSLClosedGraceful -> {
                            return 0
                        }
                        else -> {
                            error("SSLRead: ${SecCopyErrorMessageString(result, null)?.toKString()}")
                        }
                    }

                    //val resultString = SecCopyErrorMessageString(result, null)?.toKString()

                    //println("SSLRead.result=$result, resultString=$resultString")
                    //println("SSLRead.processed=${processed.value}")
                }
            }

            TODO()
        }

        private suspend fun SSLWrite(
            ctx: SSLContextRef?,
            data: ByteArray,
            offset: Int = 0,
            size: Int = data.size - offset
        ) {
            if (data.isEmpty() || size == 0) return
            if (!SSLEnsure(ctx)) return

            memScoped {
                val processed = alloc<size_tVar>()
                data.usePinned { dataPin ->
                    val result = SSLWrite(ctx, dataPin.addressOf(offset), size.convert(), processed.ptr)

                    //println("SSLWrite.result=$result, resultString=$resultString")
                    //println("SSLWrite.processed=${processed.value}")
                    if (result != 0) error("SSLWrite: ${SecCopyErrorMessageString(result, null)?.toKString()}")
                }
            }
        }

        private fun CFStringRef.toKString(): String {
            val len = CFStringGetLength(this).toInt()
            val data = ByteArray(len + 1)
            data.usePinned {
                CFStringGetCString(this@toKString, it.addressOf(0), (len + 1).convert(), kCFStringEncodingUTF8)
            }
            return data.sliceArray(0 until len).decodeToString()
        }
    }
}

private val ioErr: OSStatus = (-36).convert()

/*
 * https://github.com/karosLi/offlineH5/blob/0bf84d9baea37016d73fab70e3005ef0e3453975/node_modules/.0.19.0%40nodegit/vendor/libgit2/src/stransport_stream.c#L170
 *
 * Contrary to typical network IO callbacks, Secure Transport read callback is
 * expected to read *exactly* the requested number of bytes, not just as much
 * as it can, and any other case would be considered a failure.
 *
 * This behavior is actually not specified in the Apple documentation, but is
 * required for things to work correctly (and incidentally, that's also how
 * Apple implements it in its projects at opensource.apple.com).
 */
private fun SSL_recv_callback(
    connection: SSLConnectionRef?,
    ptr: COpaquePointer?,
    size: CPointer<size_tVar>?
): OSStatus {
    val sockfd = connection?.reinterpret<LongVar>()?.get(0) ?: error("No socket provided")
    //println("SSL_recv_callback: sockfd=$sockfd, size=${size?.get(0)}")
    val readSize = size?.get(0)?.toInt() ?: 0
    size?.set(0, 0.convert())
    var currentPtr = ptr?.reinterpret<ByteVar>()
    var pendingSize: Int = readSize
    var totalReadSize = 0
    var error: OSStatus = noErr.convert()

    memScoped {
        //val availableRead = alloc<size_tVar>()
        val availableRead: Int = ioctlSocketFionRead(sockfd.convert()).convert()
        //println("ioctlResult=$ioctlResult, availableRead.value=${availableRead.value}")
        //if (ioctlResult != 0) return errSSLWouldBlock
        //if (availableRead.value < pendingSize.convert()) return errSSLWouldBlock
        if (availableRead < pendingSize) return errSSLWouldBlock
    }

    while (pendingSize > 0) {
        val recvBytes = recv(sockfd.convert(), currentPtr, pendingSize.convert(), 0).toInt()
        if (recvBytes < 0) {
            error = ioErr.convert()
            break
        }
        if (recvBytes == 0) {
            error = errSSLClosedGraceful
            break
        }
        currentPtr += recvBytes
        pendingSize -= recvBytes
        totalReadSize += recvBytes
        //println("  --> $recvBytes")
    }
    size?.set(0, totalReadSize.convert())
    return error.convert()
}

private fun SSL_send_callback(
    connection: SSLConnectionRef?,
    ptr: COpaquePointer?,
    size: CPointer<size_tVar>?
): OSStatus {
    val sockfd = connection?.reinterpret<LongVar>()?.get(0) ?: error("No socket provided")
    //println("SSL_send_callback: sockfd=$sockfd, size=${size?.get(0)}")
    val writeBytes: size_t = size?.get(0) ?: 0.convert()
    val sentBytes = send(sockfd.convert(), ptr, writeBytes, 0)
    size?.set(0, sentBytes.convert())
    //println("  --> $sentBytes")
    return if (sentBytes.toInt() != writeBytes.toInt()) ioErr.convert() else noErr.convert()
}
