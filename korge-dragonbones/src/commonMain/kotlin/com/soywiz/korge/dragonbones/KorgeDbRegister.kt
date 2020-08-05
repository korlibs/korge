package com.soywiz.korge.dragonbones

import com.soywiz.korge.view.*
import com.soywiz.korio.serialization.xml.*

fun Views.registerDragonBones() {
    serializer.register(
        name = "dragonbones",
        deserializer = { xml ->
            when (xml.nameLC) {
                "dragonbonesref" -> KorgeDbRef()
                else -> null
            }
        },
        serializer = { view, properties ->
            when (view) {
                is KorgeDbRef -> Xml("dragonbonesref", properties)
                else -> null
            }
        }
    )
}
