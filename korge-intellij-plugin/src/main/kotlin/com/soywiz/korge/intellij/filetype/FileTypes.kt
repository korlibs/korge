package com.soywiz.korge.intellij.filetype

import com.intellij.ide.highlighter.*
import com.intellij.lang.*
import com.intellij.lang.xml.*
import com.intellij.openapi.fileTypes.*
import com.soywiz.korge.intellij.*
import javax.swing.*
import com.intellij.lang.xml.XMLParserDefinition
import com.intellij.lang.xml.XMLLanguage
import com.intellij.lexer.*
import com.intellij.openapi.project.*
import com.intellij.psi.*
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.impl.source.xml.XmlFileImpl
import com.intellij.psi.tree.*


open class XmlBaseType(
    val _icon: Icon,
    val _name: String,
    val _extension: String,
    val _description: String,
    val lang: XMLLanguage
) : XmlLikeFileType(lang) {
	override fun getIcon(): Icon? = _icon
	override fun getName(): String = _name
	override fun getDefaultExtension(): String = _extension
	override fun getDescription(): String = _description
}

open class TMXLanguage protected constructor() : XMLLanguage(XMLLanguage.INSTANCE, "KORGE_TMX", "text/tmx") {
	companion object {
		@JvmField val INSTANCE = TMXLanguage()
	}
}
open class TSXLanguage protected constructor() : XMLLanguage(XMLLanguage.INSTANCE, "KORGE_TSX", "text/tsx") {
	companion object {
		@JvmField val INSTANCE = TSXLanguage()
	}
}
open class PEXLanguage protected constructor() : XMLLanguage(XMLLanguage.INSTANCE, "KORGE_PEX", "text/pex") {
	companion object {
		@JvmField val INSTANCE = PEXLanguage()
	}
}

class KtreeFile internal constructor(viewProvider: FileViewProvider) : XmlFileImpl(viewProvider, KTREE_FILE_ELEMENT_TYPE), XmlFile {
    override fun toString(): String = "KtreeFile:$name"

    companion object {
        val KTREE_FILE_ELEMENT_TYPE = IFileElementType("KORGE_KTREE_ELEMENT_TYPE", KTREELanguage.INSTANCE)
    }
}

open class KTREELanguage protected constructor() : XMLLanguage(XMLLanguage.INSTANCE, "KORGE_KTREE", "text/ktree") {

    companion object {
        @JvmField val INSTANCE = KTREELanguage()
    }
}
open class SCMLLanguage protected constructor() : XMLLanguage(XMLLanguage.INSTANCE, "KORGE_SCML", "text/scml") {
	companion object {
		@JvmField val INSTANCE = SCMLLanguage()
	}
}
open class SCONLanguage protected constructor() : XMLLanguage(XMLLanguage.INSTANCE, "KORGE_SCON", "text/scon") {
	companion object {
		@JvmField val INSTANCE = SCONLanguage()
	}
}
open class FNTLanguage protected constructor() : XMLLanguage(XMLLanguage.INSTANCE, "KORGE_FNT", "text/fnt") {
	companion object {
		@JvmField val INSTANCE = FNTLanguage()
	}
}

open class KorgeAniFileType : UserBinaryFileType() {
	companion object {
		@JvmField val INSTANCE = KorgeAniFileType()
	}
	override fun getName(): String = "KORGE_ANI"
}

open class KorgeAudioFileType : UserBinaryFileType() {
	companion object {
		@JvmField val INSTANCE = KorgeAudioFileType()
	}
	override fun getName(): String = "KORGE_AUDIO"
}

open class TmxFileType : XmlBaseType(KorgeIcons.TILED, "KORGE_TMX", "tmx", "Tiled Map Files", TMXLanguage.INSTANCE) {
	companion object {
		@JvmField val INSTANCE = TmxFileType()
	}
}
open class TsxFileType : XmlBaseType(KorgeIcons.TILED, "KORGE_TSX", "tsx", "Tiled Tileset Files", TSXLanguage.INSTANCE) {
	companion object {
		@JvmField val INSTANCE = TsxFileType()
	}
}
open class PexFileType : XmlBaseType(KorgeIcons.PARTICLE, "KORGE_PEX", "pex", "Particle Definitions", PEXLanguage.INSTANCE) {
	companion object {
		@JvmField val INSTANCE = PexFileType()
	}
}
open class KTreeFileType : XmlBaseType(KorgeIcons.KTREE, "KORGE_KTREE", "ktree", "Korge Tree", KTREELanguage.INSTANCE) {
    companion object {
        @JvmField val INSTANCE = KTreeFileType()
    }
}
open class ScmlFileType : XmlBaseType(KorgeIcons.SPRITER, "KORGE_SCML", "scml", "Spriter Text File", SCMLLanguage.INSTANCE) {
	companion object {
		@JvmField val INSTANCE = ScmlFileType()
	}
}
open class SconFileType : XmlBaseType(KorgeIcons.SPRITER, "KORGE_SCON", "scon", "Spriter Binary File", SCONLanguage.INSTANCE) {
	companion object {
		@JvmField val INSTANCE = SconFileType()
	}
}
open class FntFileType : XmlBaseType(KorgeIcons.BITMAP_FONT, "KORGE_FNT", "fnt", "Font Definition File", FNTLanguage.INSTANCE) {
	companion object {
		@JvmField val INSTANCE = FntFileType()
	}
}

open class KraFileType : UserBinaryFileType() {
	companion object {
		@JvmField val INSTANCE = KraFileType()
	}

	override fun getName(): String = "KRA"
	override fun getDescription(): String = "Krita Image Format"

	override fun getIcon(): Icon? = KorgeIcons.KRITA
}

open class SpineSkeletonFileType : UserBinaryFileType() {
    companion object {
        @JvmField val INSTANCE = SpineSkeletonFileType()
    }
    override fun getName(): String = "SKEL"
    override fun getDescription(): String = "Spine Skeleton File"

    override fun getIcon(): Icon? = KorgeIcons.SPINE
}

open class DragonbonesSkeletonFileType : UserBinaryFileType() {
    companion object {
        @JvmField val INSTANCE = DragonbonesSkeletonFileType()
    }
    override fun getName(): String = "DBBIN"
    override fun getDescription(): String = "Dragonbones Skeleton File"

    override fun getIcon(): Icon? = KorgeIcons.DRAGONBONES
}
