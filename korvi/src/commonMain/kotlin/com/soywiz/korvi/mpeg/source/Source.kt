package com.soywiz.korvi.mpeg.source

import com.soywiz.korvi.mpeg.mux.Demuxer

interface Source {
    fun connect(demuxer: Demuxer)
}
