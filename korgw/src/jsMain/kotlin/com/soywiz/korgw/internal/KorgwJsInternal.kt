package com.soywiz.korgw.internal

import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.w3c.dom.get

internal fun NodeList.toList(): List<Node> = ArrayList<Node>().apply {
    for (n in 0 until this@toList.length) {
        val node = this@toList[n]
        if (node != null) add(node)
    }
}
