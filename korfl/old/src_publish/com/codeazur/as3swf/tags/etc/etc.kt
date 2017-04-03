package com.codeazur.as3swf.tags.etc

class TagSWFEncryptActions(type: Int = 0) : com.codeazur.as3swf.tags.TagUnknown(), com.codeazur.as3swf.tags.ITag {
	companion object {
		const val TYPE = 253
	}

	override val type = TYPE
	override val name = "SWFEncryptActions"
}

class TagSWFEncryptSignature(type: Int = 0) : com.codeazur.as3swf.tags.TagUnknown(), com.codeazur.as3swf.tags.ITag {
	companion object {
		const val TYPE = 255
	}

	override val type = TYPE
	override val name = "SWFEncryptSignature"
}
