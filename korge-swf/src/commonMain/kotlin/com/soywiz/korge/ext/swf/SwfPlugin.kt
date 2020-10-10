package com.soywiz.korge.ext.swf

import com.soywiz.korge.animate.*
import com.soywiz.korge.view.*

fun Views.registerSwf() {
    val loderTester = KorgeFileLoaderTester<AnLibrary>("swf") { s, injector ->
        val MAGIC = s.readString(3)
        when (MAGIC) {
            "FWS", "CWS", "ZWS" -> KorgeFileLoader<AnLibrary>("swf") { content, views -> this.readSWF(AnLibrary.Context(views), content.ba) }
            else -> null
        }
    }
	views.animateLibraryLoaders.add(loderTester)
}
