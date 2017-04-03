package com.codeazur.as3swf.data.actions.swf4

import com.codeazur.as3swf.SWFData
import com.codeazur.as3swf.data.actions.Action
import com.codeazur.as3swf.data.actions.ActionExecutionContext
import com.codeazur.as3swf.data.actions.IAction
import com.codeazur.as3swf.data.actions.IActionBranch
import java.util.*

class ActionAdd(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x0a
	}

	override fun toString(indent: Int): String = "[ActionAdd]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "add"
}

class ActionAnd(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x10
	}

	override fun toString(indent: Int): String = "[ActionAnd]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "and"
}

class ActionAsciiToChar(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x33
	}

	override fun toString(indent: Int): String = "[ActionAsciiToChar]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "asciiToChar"
}

class ActionCall(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x9e
	}

	override fun toString(indent: Int): String = "[ActionCall]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "call"
}

class ActionCharToAscii(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x32
	}

	override fun toString(indent: Int): String = "[ActionCharToAscii]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "charToAscii"
}

class ActionCloneSprite(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x24
	}

	override fun toString(indent: Int): String = "[ActionCloneSprite]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "cloneSprite"
}

class ActionDivide(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x0d
	}

	override fun toString(indent: Int): String = "[ActionDivide]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "divide"
}

class ActionEndDrag(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x28
	}

	override fun toString(indent: Int): String = "[ActionEndDrag]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "endDrag"
}

class ActionEquals(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x0e
	}

	override fun toString(indent: Int): String = "[ActionEquals]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "equals"
}

class ActionGetProperty(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x22
	}

	override fun toString(indent: Int): String = "[ActionGetProperty]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "getProperty"
}

class ActionGetTime(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x34
	}

	override fun toString(indent: Int): String = "[ActionGetTime]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "getTime"
}

class ActionGetURL2(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x9a
	}

	var sendVarsMethod: Int = 0
	var reserved: Int = 0
	var loadTargetFlag: Boolean = false
	var loadVariablesFlag: Boolean = false

	override fun parse(data: SWFData): Unit {
		sendVarsMethod = data.readUB(2)
		reserved = data.readUB(4) // reserved, always 0
		loadTargetFlag = (data.readUB(1) == 1)
		loadVariablesFlag = (data.readUB(1) == 1)
	}

	override fun toString(indent: Int): String {
		return "[ActionGetURL2] " +
			"SendVarsMethod: " + sendVarsMethod + " (" + sendVarsMethodToString() + "), " +
			"Reserved: " + reserved + ", " +
			"LoadTargetFlag: " + loadTargetFlag + ", " +
			"LoadVariablesFlag: " + loadVariablesFlag
	}

	override fun toBytecode(indent: Int, context: ActionExecutionContext): String {
		return toBytecodeLabel(indent) +
			"getUrl2 (method: " + sendVarsMethodToString() + ", target: " +
			(if (!loadTargetFlag) "window" else "sprite") + ", variables: " +
			(if (!loadVariablesFlag) "no" else "yes") + ")"
	}

	fun sendVarsMethodToString(): String {
		return if (sendVarsMethod == 0) {
			"None"
		} else if (sendVarsMethod == 1) {
			"GET"
		} else if (sendVarsMethod == 2) {
			"POST"
		} else {
			throw Error("sendVarsMethod is only defined for values of 0, 1, and 2.")
		}
	}
}

class ActionGetVariable(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x1c
	}

	override fun toString(indent: Int): String = "[ActionGetVariable]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "getVariable"
}

class ActionGotoFrame2(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x9f
	}

	var sceneBiasFlag: Boolean = false
	var playFlag: Boolean = false
	var sceneBias: Int = 0

	override fun parse(data: SWFData): Unit {
		val flags = data.readUI8()
		sceneBiasFlag = ((flags and 0x02) != 0)
		playFlag = ((flags and 0x01) != 0)
		if (sceneBiasFlag) {
			sceneBias = data.readUI16()
		}
	}

	override fun toString(indent: Int): String {
		var str: String = "[ActionGotoFrame2] PlayFlag: $playFlag, SceneBiasFlag: $sceneBiasFlag"
		if (sceneBiasFlag) str += ", " + sceneBias
		return str
	}

	override fun toBytecode(indent: Int, context: ActionExecutionContext): String {
		return toBytecodeLabel(indent) + "gotoFrame2 (" +
			(if (!playFlag) "gotoAndStop" else "gotoAndPlay") +
			(if (sceneBiasFlag) ", sceneBias: " + sceneBias else "") +
			")"
	}
}

class ActionIf(code: Int, length: Int, pos: Int) : Action(code, length, pos), IActionBranch {
	companion object {
		const val CODE = 0x9d
	}

	override var branchOffset: Int = 0

	// branchIndex is resolved in TagDoAction::parse()
	override var branchIndex: Int = -2

	override fun parse(data: SWFData): Unit {
		branchOffset = data.readSI16()
	}

	override fun toString(indent: Int): String {
		var bi: String = " ["
		if (branchIndex >= 0) {
			bi += branchIndex.toString()
		} else if (branchIndex == -1) {
			bi += "EOB"
		} else {
			bi += "???"
		}
		bi += "]"
		return "[ActionIf] BranchOffset: " + branchOffset + bi
	}

	override fun toBytecode(indent: Int, context: ActionExecutionContext): String {
		var ls: String = ""
		if (branchIndex >= 0) {
			ls += context.actions[branchIndex]?.lbl
		} else if (branchIndex == -1) {
			ls += "L" + (context.labelCount + 1)
		} else {
			ls += "ILLEGAL BRANCH"
		}
		return toBytecodeLabel(indent) + "if " + ls
	}
}

class ActionJump(code: Int, length: Int, pos: Int) : Action(code, length, pos), IActionBranch {
	companion object {
		const val CODE = 0x99
	}

	override var branchOffset: Int = 0

	// branchIndex is resolved in TagDoAction::parse()
	override var branchIndex: Int = -2

	override fun parse(data: SWFData): Unit {
		branchOffset = data.readSI16()
	}

	override fun toString(indent: Int): String {
		var bi: String = " ["
		if (branchIndex >= 0) {
			bi += branchIndex.toString()
		} else if (branchIndex == -1) {
			bi += "EOB"
		} else {
			bi += "???"
		}
		bi += "]"
		return "[ActionJump] BranchOffset: " + branchOffset + bi
	}

	override fun toBytecode(indent: Int, context: ActionExecutionContext): String {
		var ls: String = ""
		if (branchIndex >= 0) {
			ls += context.actions[branchIndex]?.lbl
		} else if (branchIndex == -1) {
			ls += "L" + (context.labelCount + 1)
		} else {
			ls += "ILLEGAL BRANCH"
		}
		return toBytecodeLabel(indent) + "jump " + ls
	}
}

class ActionLess(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x0f
	}

	override fun toString(indent: Int): String = "[ActionLess]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "less"
}

class ActionMBAsciiToChar(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x37
	}

	override fun toString(indent: Int): String = "[ActionMBAsciiToChar]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "mbAsciiToChar"
}

class ActionMBCharToAscii(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x36
	}

	override fun toString(indent: Int): String = "[ActionMBCharToAscii]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "mbCharToAscii"
}

class ActionMBStringExtract(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x35
	}

	override fun toString(indent: Int): String = "[ActionMBStringExtract]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "mbStringExtract"
}

class ActionMBStringLength(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x31
	}

	override fun toString(indent: Int): String = "[ActionMBStringLength]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "mbStringLength"
}

class ActionMultiply(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x0c
	}

	override fun toString(indent: Int): String = "[ActionMultiply]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "multiply"
}

class ActionNot(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x12
	}

	override fun toString(indent: Int): String = "[ActionNot]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "not"
}

class ActionOr(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x11
	}

	override fun toString(indent: Int): String = "[ActionOr]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "or"
}

class ActionPop(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x17
	}

	override fun toString(indent: Int): String = "[ActionPop]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "pop"
}

class ActionPush(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x96
	}

	var values = ArrayList<com.codeazur.as3swf.data.SWFActionValue>()

	override fun parse(data: SWFData): Unit {
		val endPosition = data.position + length
		while (data.position != endPosition) {
			values.add(data.readACTIONVALUE())
		}
	}

	override fun toString(indent: Int): String = "[ActionPush] " + values.joinToString(", ")

	override fun toBytecode(indent: Int, context: ActionExecutionContext): String {
		var str: String = toBytecodeLabel(indent)
		for (i in 0 until values.size) {
			if (i > 0) str += "\n" + " ".repeat(indent + 2)
			str += "push " + values[i].toBytecodeString(context.cpool)
		}
		return str
	}
}

class ActionRandomNumber(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x30
	}

	override fun toString(indent: Int): String = "[ActionRandomNumber]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "randomNumber"
}

class ActionRemoveSprite(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x25
	}

	override fun toString(indent: Int): String = "[ActionRemoveSprite]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "removeSprite"
}

class ActionSetProperty(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x23
	}

	override fun toString(indent: Int): String = "[ActionSetProperty]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "setProperty"
}

class ActionSetTarget2(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x20
	}

	override fun toString(indent: Int): String = "[ActionSetTarget2]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "setTarget2"
}

class ActionSetVariable(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x1d
	}

	override fun toString(indent: Int): String = "[ActionSetVariable]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "setVariable"
}

class ActionStartDrag(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x27
	}

	override fun toString(indent: Int): String = "[ActionStartDrag]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "startDrag"
}

class ActionStringAdd(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x21
	}

	override fun toString(indent: Int): String = "[ActionStringAdd]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "stringAdd"
}

class ActionStringEquals(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x13
	}

	override fun toString(indent: Int): String = "[ActionStringEquals]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "stringEquals"
}

class ActionStringExtract(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x15
	}

	override fun toString(indent: Int): String = "[ActionStringExtract]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "stringExtract"
}

class ActionStringLength(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x14
	}

	override fun toString(indent: Int): String = "[ActionStringLength]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "stringLength"
}

class ActionStringLess(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x29
	}

	override fun toString(indent: Int): String = "[ActionStringLess]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "stringLess"
}

class ActionSubtract(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x0b
	}

	override fun toString(indent: Int): String = "[ActionSubtract]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "subtract"
}

class ActionToInteger(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x18
	}

	override fun toString(indent: Int): String = "[ActionToInteger]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "toInteger"
}

class ActionTrace(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x26
	}

	override fun toString(indent: Int): String = "[ActionTrace]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "trace"
}

class ActionWaitForFrame2(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x8d
	}

	var skipCount: Int = 0

	override fun parse(data: SWFData): Unit {
		skipCount = data.readUI8()
	}

	override fun toString(indent: Int): String = "[ActionWaitForFrame2] SkipCount: " + skipCount
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "waitForFrame2 (" + skipCount + ")"
}
