package com.soywiz.korge.intellij.editor

import com.soywiz.korio.serialization.xml.*
import java.awt.datatransfer.*

class KTreeTransferable(val xml: Xml) : Transferable {
    companion object {
        val FLAVOR = DataFlavor("text/ktree; charset=unicode; class=java.lang.String", "KTree XML")
    }

    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(FLAVOR, DataFlavor.stringFlavor)
    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor in transferDataFlavors
    override fun getTransferData(flavor: DataFlavor): Any = when (flavor) {
        KTreeTransferable.FLAVOR -> xml
        DataFlavor.stringFlavor -> xml.toOuterXmlIndentedString()
        else -> throw UnsupportedFlavorException(flavor)
    }
}
