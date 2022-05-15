package com.soywiz.korge.view

import com.soywiz.kds.iterators.fastForEach

interface BView {
    val bview: View
    val bviewAll: List<View>
}

inline fun BView.bviewFastForEach(block: (view: View) -> Unit) {
    bviewAll.fastForEach { block(it) }
}
