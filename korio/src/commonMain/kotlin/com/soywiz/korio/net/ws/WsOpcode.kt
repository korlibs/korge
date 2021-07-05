package com.soywiz.korio.net.ws

inline class WsOpcode(val id: Int) {
    companion object {
        val Continuation = WsOpcode(0x00)
        val Text = WsOpcode(0x01)
        val Binary = WsOpcode(0x02)
        val Close = WsOpcode(0x08)
        val Ping = WsOpcode(0x09)
        val Pong = WsOpcode(0x0A)
    }

    override fun toString(): String = when (this) {
        Continuation -> "Continuation"
        Text -> "Text"
        Binary -> "Binary"
        Close -> "Close"
        Ping -> "Ping"
        Pong -> "Pong"
        else -> "WsOpcode#$id"
    }
}
