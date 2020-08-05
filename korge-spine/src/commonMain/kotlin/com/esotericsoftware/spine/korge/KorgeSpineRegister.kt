package com.esotericsoftware.spine.korge

import com.soywiz.korge.view.*
import com.soywiz.korio.serialization.xml.*

fun Views.registerSpine() {
    serializer.register(
        name = "spine",
        deserializer = { xml ->
            when (xml.nameLC) {
                "spineref" -> SpineViewRef()
                else -> null
            }
        },
        serializer = { view, properties ->
            when (view) {
                is SpineViewRef -> Xml("spineref", properties)
                else -> null
            }
        }
    )
}
