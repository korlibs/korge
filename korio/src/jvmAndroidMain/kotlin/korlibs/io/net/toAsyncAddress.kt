package korlibs.io.net

import java.net.*

fun SocketAddress?.toAsyncAddress(): AsyncAddress {
    if (this is InetSocketAddress) {
        return AsyncAddress(this.hostString, this.port)
    }
    return AsyncAddress()
}
