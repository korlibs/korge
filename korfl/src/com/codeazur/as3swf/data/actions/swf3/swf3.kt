package com.codeazur.as3swf.data.actions.swf3

import com.codeazur.as3swf.SWFData
import com.codeazur.as3swf.data.actions.Action
import com.codeazur.as3swf.data.actions.ActionExecutionContext
import com.codeazur.as3swf.data.actions.IAction

class ActionGetURL(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x83
	}

	var urlString: String? = null
	var targetString: String? = null

	override fun parse(data: SWFData) {
		urlString = data.readString()
		targetString = data.readString()
	}

	override fun toString(indent: Int): String = "[ActionGetURL] URL: $urlString, Target: $targetString"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = "${toBytecodeLabel(indent)}getURL \"$urlString\", \"$targetString\""
}

class ActionGotoFrame(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		val CODE = 0x81
	}

	var frame = 0

	override fun parse(data: SWFData): Unit {
		frame = data.readUI16()
	}

	override fun toString(indent: Int): String = "[ActionGotoFrame] Frame: " + frame
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "gotoFrame " + frame
}

class ActionGotoLabel(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x8c
	}

	var label: String? = null

	override fun parse(data: SWFData): Unit {
		label = data.readString()
	}

	override fun toString(indent: Int): String = "[ActionGotoLabel] Label: $label"

	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = "${toBytecodeLabel(indent)}gotoLabel \"$label\""
}

class ActionNextFrame(code: Int, length: Int, pos: Int) : Action(code, length, pos) {
	companion object {
		const val CODE = 0x04
	}

	override fun toString(indent: Int): String = "[ActionNextFrame]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "nextFrame"
}

class ActionPlay(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x06
	}

	override fun toString(indent: Int): String = "[ActionPlay]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "play"
}

class ActionPreviousFrame(code: Int, length: Int, pos: Int) : Action(code, length, pos) {
	companion object {
		const val CODE = 0x05
	}

	override fun toString(indent: Int): String = "[ActionPreviousFrame]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "previousFrame"
}

class ActionSetTarget(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x8b
	}

	var targetName: String? = null

	override fun parse(data: SWFData): Unit {
		targetName = data.readString()
	}

	override fun toString(indent: Int): String = "[ActionSetTarget] TargetName: $targetName"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = "${toBytecodeLabel(indent)}setTarget \"$targetName\""
}

class ActionStop(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x07
	}

	override fun toString(indent: Int): String = "[ActionStop]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "stop"
}

class ActionStopSounds(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x09
	}

	override fun toString(indent: Int): String = "[ActionStopSounds]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "stopSounds"
}

class ActionToggleQuality(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x08
	}

	override fun toString(indent: Int): String = "[ActionToggleQuality]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "toggleQuality"
}

class ActionWaitForFrame(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x8a
	}

	var frame: Int = 0
	var skipCount: Int = 0

	override fun parse(data: SWFData): Unit {
		frame = data.readUI16()
		skipCount = data.readUI8()
	}

	override fun toString(indent: Int): String = "[ActionWaitForFrame] Frame: $frame, SkipCount: $skipCount"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "waitForFrame " + frame + (if (skipCount > 0) ", " + skipCount else "")
}
