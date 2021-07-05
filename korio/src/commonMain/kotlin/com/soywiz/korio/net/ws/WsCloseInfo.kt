package com.soywiz.korio.net.ws

import com.soywiz.korio.stream.*

// https://developer.mozilla.org/en-US/docs/Web/API/CloseEvent#status_codes
data class WsCloseInfo(val code: Int, val reason: String) {
    companion object {
        val NormalClosure = WsCloseInfo(1000, "")
        val GoingAway = WsCloseInfo(1001, "")
        val ProtocolError = WsCloseInfo(1002, "")
        val UnsupportedData = WsCloseInfo(1003, "")
        val NoStatusReceived = WsCloseInfo(1005, "")
        val AbnormalClosure = WsCloseInfo(1006, "")
        val InvalidFramePayloadData = WsCloseInfo(1007, "")
        val PolicyViolation = WsCloseInfo(1008, "")
        val MessageTooBig = WsCloseInfo(1009, "")
        val MissingExtension = WsCloseInfo(1010, "")
        val InternalError = WsCloseInfo(1011, "")
        val ServiceRestart = WsCloseInfo(1012, "")
        val TryAgainLater = WsCloseInfo(1013, "")
        val BadGateway = WsCloseInfo(1014, "")
        val TLSHandshake = WsCloseInfo(1015, "")

        fun fromBytes(data: ByteArray): WsCloseInfo {
            try {
                val s = data.openSync()
                return WsCloseInfo(s.readS16BE(), s.readString(s.availableRead.toInt()))
            } catch (e: Throwable) {
                return WsCloseInfo(-1, "Unknown")
            }
        }
    }
    fun toByteArray() = MemorySyncStreamToByteArray {
        write16BE(code)
        writeString(reason)
    }
    fun toFrame(masked: Boolean) = WsFrame(toByteArray(), WsOpcode.Close, masked = masked)
}
