package com.soywiz.korge.dragonbones

import com.soywiz.korge.view.*
import com.soywiz.korge.view.ktree.*
import com.soywiz.korio.serialization.xml.*

@Deprecated("KTree is going to be removed in a future version")
fun Views.registerDragonBones() {
    ktreeSerializer.register(
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
