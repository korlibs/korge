package com.codeazur.as3swf.data.actions

import com.codeazur.as3swf.SWFData
import com.soywiz.korio.util.toString

open class Action(
	override val code: Int,
	override var length: Int,
	override val pos: Int
) : IAction {
	override var lbl: String? = null

	override val lengthWithHeader: Int get() = length + if (code >= 0x80) 3 else 1

	override fun parse(data: SWFData) {
		// Do nothing. Many Actions don't have a payload.
		// For the ones that have one we override this method.
	}

	protected fun write(data: SWFData, body: SWFData? = null) {
		data.writeUI8(code)
		if (code >= 0x80) {
			if (body != null && body.length > 0) {
				length = body.length
				data.writeUI16(length)
				data.writeBytes(body)
			} else {
				length = 0
				throw(Error("Action body null or empty."))
			}
		} else {
			length = 0
		}
	}

	override fun toString(indent: Int): String = "[Action] Code: ${code.toString(16)}, Length: $length"

	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = "${toBytecodeLabel(indent)}unknown (0x${code.toString(16)})"

	fun toBytecodeLabel(indent: Int): String {
		return if (lbl != null) "$lbl:\n${" ".repeat(indent + 2)}" else " ".repeat(2)
	}

	companion object {
		fun resolveOffsets(actions: List<IAction>): Int {
			var labelNr = 1
			var labelCount = 0
			var action: IAction
			val n = actions.size
			for (i in 0 until n) {
				action = actions[i]
				if (action is IActionBranch) {
					var j: Int = 0
					var found = false
					val actionBranch = action
					val targetPos = actionBranch.pos + actionBranch.lengthWithHeader + actionBranch.branchOffset
					if (targetPos <= actionBranch.pos) {
						for (_j in 0..i) {
							j = i - _j
							if (targetPos == actions[j].pos) {
								labelCount++
								found = true
								break
							}
						}
					} else {
						for (_j in i + 1 until n) {
							j = _j
							if (targetPos == actions[j].pos) {
								labelCount++
								found = true
								break
							}
						}
						if (!found) {
							action = actions[j - 1]
							if (targetPos == action.pos + action.lengthWithHeader) {
								j = -1 // End of execution block
								found = true
							}
						}
					}
					if (found) {
						actionBranch.branchIndex = j
						if (j >= 0) {
							action = actions[j]
							action.lbl = "L"
						}
					} else {
						actionBranch.branchIndex = -2
					}
				}
			}
			for (i in 0 until n) {
				action = actions[i]
				if (action.lbl != null) {
					action.lbl += labelNr++
				}
			}
			return labelCount
		}
	}

}

class ActionExecutionContext(
	val actions: List<IAction?>,
	val cpool: ArrayList<String>,
	val labelCount: Int
) {
	var endLabel: String? = if (actions.firstOrNull { it is IActionBranch && it.branchIndex == -1 } != null) {
		"L" + (labelCount + 1)
	} else {
		null
	}
}

class ActionUnknown(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun parse(data: SWFData) {
		if (length > 0) data.skipBytes(length)
	}

	override fun toString(indent: Int): String = "[????] Code: " + code.toString(16) + ", Length: " + length
}

interface IAction {
	val code: Int
	val length: Int
	val lengthWithHeader: Int
	val pos: Int

	var lbl: String?

	fun parse(data: SWFData): Unit
	fun toString(indent: Int = 0): String
	fun toBytecode(indent: Int, context: ActionExecutionContext): String
}

interface IActionBranch : IAction {
	var branchOffset: Int
	var branchIndex: Int
}

class ActionGetURL(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
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
	var frame = 0

	override fun parse(data: SWFData): Unit {
		frame = data.readUI16()
	}

	override fun toString(indent: Int): String = "[ActionGotoFrame] Frame: " + frame
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "gotoFrame " + frame
}

class ActionGotoLabel(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	var label: String? = null

	override fun parse(data: SWFData): Unit {
		label = data.readString()
	}

	override fun toString(indent: Int): String = "[ActionGotoLabel] Label: $label"

	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = "${toBytecodeLabel(indent)}gotoLabel \"$label\""
}

class ActionNextFrame(code: Int, length: Int, pos: Int) : Action(code, length, pos) {
	override fun toString(indent: Int): String = "[ActionNextFrame]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "nextFrame"
}

class ActionPlay(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionPlay]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "play"
}

class ActionPreviousFrame(code: Int, length: Int, pos: Int) : Action(code, length, pos) {
	override fun toString(indent: Int): String = "[ActionPreviousFrame]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "previousFrame"
}

class ActionSetTarget(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	var targetName: String? = null

	override fun parse(data: SWFData): Unit {
		targetName = data.readString()
	}

	override fun toString(indent: Int): String = "[ActionSetTarget] TargetName: $targetName"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = "${toBytecodeLabel(indent)}setTarget \"$targetName\""
}

class ActionStop(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionStop]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "stop"
}

class ActionStopSounds(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionStopSounds]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "stopSounds"
}

class ActionToggleQuality(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionToggleQuality]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "toggleQuality"
}

class ActionWaitForFrame(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	var frame: Int = 0
	var skipCount: Int = 0

	override fun parse(data: SWFData): Unit {
		frame = data.readUI16()
		skipCount = data.readUI8()
	}

	override fun toString(indent: Int): String = "[ActionWaitForFrame] Frame: $frame, SkipCount: $skipCount"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "waitForFrame " + frame + (if (skipCount > 0) ", " + skipCount else "")
}

class ActionAdd(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionAdd]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "add"
}

class ActionAnd(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionAnd]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "and"
}

class ActionAsciiToChar(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionAsciiToChar]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "asciiToChar"
}

class ActionCall(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionCall]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "call"
}

class ActionCharToAscii(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionCharToAscii]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "charToAscii"
}

class ActionCloneSprite(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionCloneSprite]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "cloneSprite"
}

class ActionDivide(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionDivide]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "divide"
}

class ActionEndDrag(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionEndDrag]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "endDrag"
}

class ActionEquals(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionEquals]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "equals"
}

class ActionGetProperty(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionGetProperty]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "getProperty"
}

class ActionGetTime(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionGetTime]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "getTime"
}

class ActionGetURL2(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
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
	override fun toString(indent: Int): String = "[ActionGetVariable]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "getVariable"
}

class ActionGotoFrame2(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
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
	override fun toString(indent: Int): String = "[ActionLess]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "less"
}

class ActionMBAsciiToChar(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionMBAsciiToChar]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "mbAsciiToChar"
}

class ActionMBCharToAscii(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionMBCharToAscii]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "mbCharToAscii"
}

class ActionMBStringExtract(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionMBStringExtract]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "mbStringExtract"
}

class ActionMBStringLength(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionMBStringLength]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "mbStringLength"
}

class ActionMultiply(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionMultiply]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "multiply"
}

class ActionNot(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionNot]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "not"
}

class ActionOr(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionOr]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "or"
}

class ActionPop(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionPop]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "pop"
}

class ActionPush(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
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
	override fun toString(indent: Int): String = "[ActionRandomNumber]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "randomNumber"
}

class ActionRemoveSprite(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionRemoveSprite]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "removeSprite"
}

class ActionSetProperty(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionSetProperty]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "setProperty"
}

class ActionSetTarget2(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionSetTarget2]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "setTarget2"
}

class ActionSetVariable(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionSetVariable]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "setVariable"
}

class ActionStartDrag(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionStartDrag]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "startDrag"
}

class ActionStringAdd(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionStringAdd]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "stringAdd"
}

class ActionStringEquals(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionStringEquals]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "stringEquals"
}

class ActionStringExtract(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionStringExtract]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "stringExtract"
}

class ActionStringLength(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionStringLength]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "stringLength"
}

class ActionStringLess(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionStringLess]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "stringLess"
}

class ActionSubtract(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionSubtract]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "subtract"
}

class ActionToInteger(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionToInteger]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "toInteger"
}

class ActionTrace(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionTrace]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "trace"
}

class ActionWaitForFrame2(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	var skipCount: Int = 0

	override fun parse(data: SWFData): Unit {
		skipCount = data.readUI8()
	}

	override fun toString(indent: Int): String = "[ActionWaitForFrame2] SkipCount: " + skipCount
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "waitForFrame2 (" + skipCount + ")"
}

class ActionAdd2(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionAdd2]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "add2"
}

class ActionBitAnd(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionBitAnd]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "bitAnd"
}

class ActionBitLShift(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionBitLShift]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "bitLShift"
}

class ActionBitOr(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionBitOr]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "bitOr"
}

class ActionBitRShift(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionBitRShift]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "bitRShift"
}

class ActionBitURShift(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionBitURShift]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "bitURShift"
}

class ActionBitXor(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionBitXor]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "bitXor"
}

class ActionCallFunction(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionCallFunction]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "callFunction"
}

class ActionCallMethod(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionCallMethod]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "callMethod"
}

class ActionConstantPool(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	var constants = ArrayList<String>()

	override fun parse(data: SWFData): Unit {
		val count = data.readUI16()
		for (i in 0 until count) {
			constants.add(data.readString())
		}
	}

	override fun toString(indent: Int): String {
		var str: String = "[ActionConstantPool] Values: " + constants.size
		for (i in 0 until constants.size) {
			str += "\n" + " ".repeat(indent + 4) + i + ": " + (constants[i])
		}
		return str
	}

	override fun toBytecode(indent: Int, context: ActionExecutionContext): String {
		var str: String = toBytecodeLabel(indent) + "constantPool"
		context.cpool.clear()
		for (i in 0 until constants.size) {
			str += "\n" + " ".repeat(indent + 4) + i + ": " + (constants[i])
			context.cpool.add(constants[i])
		}
		return str
	}
}

class ActionDecrement(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionDecrement]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "decrement"
}

open class ActionDefineFunction(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	var functionName: String? = null
	var functionParams = ArrayList<String>()
	var functionBody = ArrayList<IAction>()

	protected var labelCount: Int = 0

	override fun parse(data: SWFData): Unit {
		functionName = data.readString()
		val count = data.readUI16()
		for (i in 0 until count) {
			functionParams.add(data.readString())
		}
		val codeSize = data.readUI16()
		val bodyEndPosition = data.position + codeSize
		while (data.position < bodyEndPosition) {
			functionBody.add(data.readACTIONRECORD()!!)
		}
		labelCount = resolveOffsets(functionBody)
	}

	override fun toString(indent: Int): String {
		var str: String = "[ActionDefineFunction] " +
			(if (functionName == null || functionName!!.isEmpty()) "<anonymous>" else functionName) +
			"(" + functionParams.joinToString(", ") + ")"
		for (i in 0 until functionBody.size) str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + functionBody[i].toString(indent + 4)
		return str
	}

	override fun toBytecode(indent: Int, context: ActionExecutionContext): String {
		var str: String = toBytecodeLabel(indent) + "defineFunction " +
			(if (functionName == null || functionName!!.isEmpty()) "" else functionName) +
			"(" + functionParams.joinToString(", ") + ") {"
		val ctx = ActionExecutionContext(functionBody, ArrayList(context.cpool), labelCount)
		for (i in 0 until functionBody.size) {
			str += "\n" + " ".repeat(indent + 4) + functionBody[i].toBytecode(indent + 4, ctx)
		}
		if (ctx.endLabel != null) {
			str += "\n" + " ".repeat(indent + 4) + ctx.endLabel + ":"
		}
		str += "\n" + " ".repeat(indent + 2) + "}"
		return str
	}
}

class ActionDefineLocal(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionDefineLocal]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "defineLocal"
}

class ActionDefineLocal2(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionDefineLocal2]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "defineLocal2"
}

class ActionDelete(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionDelete]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "delete"
}

class ActionDelete2(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionDelete2]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "delete2"
}

class ActionEnumerate(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionEnumerate]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "enumerate"
}

class ActionEquals2(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionEquals2]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "equals2"
}

class ActionGetMember(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionGetMember]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "getMember"
}

class ActionIncrement(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionIncrement]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "increment"
}

class ActionInitArray(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionInitArray]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "initArray"
}

class ActionInitObject(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionInitObject]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "initObject"
}

class ActionLess2(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionLess2]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "less2"
}

class ActionModulo(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionModulo]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "modulo"
}

class ActionNewMethod(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionNewMethod]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "newMethod"
}

class ActionNewObject(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionNewObject]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "newObject"
}

class ActionPushDuplicate(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionPushDuplicate]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "pushDuplicate"
}

class ActionReturn(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionReturn]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "return"
}

class ActionSetMember(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionSetMember]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "setMember"
}

class ActionStackSwap(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String {
		return "[ActionStackSwap]"
	}

	override fun toBytecode(indent: Int, context: ActionExecutionContext): String {
		return toBytecodeLabel(indent) + "stackSwap"
	}
}

class ActionStoreRegister(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	var registerNumber: Int = 0

	override fun parse(data: SWFData): Unit {
		registerNumber = data.readUI8()
	}

	override fun toString(indent: Int): String = "[ActionStoreRegister] RegisterNumber: " + registerNumber
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "store $" + registerNumber
}

class ActionTargetPath(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionTargetPath]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "targetPath"
}

class ActionToNumber(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionToNumber]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "toNumber"
}

class ActionToString(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionToString]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "toString"
}

class ActionTypeOf(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionTypeOf]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "typeOf"
}

open class ActionWith(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	var withBody = ArrayList<IAction>()

	protected var labelCount: Int = 0

	override fun parse(data: SWFData): Unit {
		val codeSize = data.readUI16()
		val bodyEndPosition = data.position + codeSize
		while (data.position < bodyEndPosition) {
			withBody.add(data.readACTIONRECORD()!!)
		}
		labelCount = resolveOffsets(withBody)
	}

	override fun toString(indent: Int): String {
		var str: String = "[ActionWith]"
		for (i in 0 until withBody.size) {
			str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + withBody[i].toString(indent + 4)
		}
		return str
	}

	override fun toBytecode(indent: Int, context: ActionExecutionContext): String {
		var str: String = toBytecodeLabel(indent) + "with {"
		val ctx = ActionExecutionContext(withBody, ArrayList(context.cpool), labelCount)
		for (i in 0 until withBody.size) str += "\n" + " ".repeat(indent + 4) + withBody[i].toBytecode(indent + 4, ctx)
		if (ctx.endLabel != null) str += "\n" + " ".repeat(indent + 4) + ctx.endLabel + ":"
		str += "\n" + " ".repeat(indent + 2) + "}"
		return str
	}
}

class ActionEnumerate2(code: Int, length: Int, pos: Int) : Action(code, length, pos) {


	override fun toString(indent: Int): String = "[ActionEnumerate2]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "enumerate2"
}

class ActionGreater(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionGreater]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "greater"
}

class ActionInstanceOf(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionInstanceOf]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "instanceOf"
}

class ActionStrictEquals(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionStrictEquals]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "strictEquals"
}

class ActionStringGreater(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionStringGreater]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "stringGreater"
}

class ActionCastOp(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionCastOp]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "castOp"
}

class ActionDefineFunction2(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	var functionName: String? = null
	var functionParams = ArrayList<com.codeazur.as3swf.data.SWFRegisterParam>()
	var functionBody = ArrayList<IAction>()
	protected var labelCount: Int = 0

	var registerCount: Int = 0

	var preloadParent: Boolean = false
	var preloadRoot: Boolean = false
	var preloadSuper: Boolean = false
	var preloadArguments: Boolean = false
	var preloadThis: Boolean = false
	var preloadGlobal: Boolean = false
	var suppressSuper: Boolean = false
	var suppressArguments: Boolean = false
	var suppressThis: Boolean = false

	override fun parse(data: SWFData) {
		functionName = data.readString()
		val numParams = data.readUI16()
		registerCount = data.readUI8()
		val flags1 = data.readUI8()
		preloadParent = ((flags1 and 0x80) != 0)
		preloadRoot = ((flags1 and 0x40) != 0)
		suppressSuper = ((flags1 and 0x20) != 0)
		preloadSuper = ((flags1 and 0x10) != 0)
		suppressArguments = ((flags1 and 0x08) != 0)
		preloadArguments = ((flags1 and 0x04) != 0)
		suppressThis = ((flags1 and 0x02) != 0)
		preloadThis = ((flags1 and 0x01) != 0)
		val flags2 = data.readUI8()
		preloadGlobal = ((flags2 and 0x01) != 0)
		for (i in 0 until numParams) functionParams.add(data.readREGISTERPARAM())
		val codeSize = data.readUI16()
		val bodyEndPosition = data.position + codeSize
		while (data.position < bodyEndPosition) functionBody.add(data.readACTIONRECORD()!!)
		labelCount = resolveOffsets(functionBody)
	}

	override fun toString(indent: Int): String {
		var str: String = "[ActionDefineFunction2] " +
			(if (functionName == null || functionName!!.isEmpty()) "<anonymous>" else functionName) +
			"(" + functionParams.joinToString(", ") + "), "
		val a = arrayListOf<String>()
		if (preloadParent) a.add("preloadParent")
		if (preloadRoot) a.add("preloadRoot")
		if (preloadSuper) a.add("preloadSuper")
		if (preloadArguments) a.add("preloadArguments")
		if (preloadThis) a.add("preloadThis")
		if (preloadGlobal) a.add("preloadGlobal")
		if (suppressSuper) a.add("suppressSuper")
		if (suppressArguments) a.add("suppressArguments")
		if (suppressThis) a.add("suppressThis")
		if (a.size == 0) a.add("none")
		str += "Flags: " + a.joinToString(",")
		for (i in 0 until functionBody.size) {
			str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + functionBody[i].toString(indent + 4)
		}
		return str
	}

	override fun toBytecode(indent: Int, context: ActionExecutionContext): String {
		var str: String = toBytecodeLabel(indent) + "defineFunction2 " +
			(if (functionName == null || functionName!!.isEmpty()) "" else functionName) +
			"(" + functionParams.joinToString(", ") + ") {"
		val preload = arrayListOf<String>()
		val suppress = arrayListOf<String>()
		if (preloadParent) preload.add("parent")
		if (preloadRoot) preload.add("root")
		if (preloadSuper) preload.add("super")
		if (preloadArguments) preload.add("arguments")
		if (preloadThis) preload.add("this")
		if (preloadGlobal) preload.add("global")
		if (suppressSuper) suppress.add("super")
		if (suppressArguments) suppress.add("arguments")
		if (suppressThis) suppress.add("this")
		if (preload.size > 0) str += "\n" + " ".repeat(indent + 4) + "// preload: " + preload.joinToString(", ")
		if (suppress.size > 0) {
			str += "\n" + " ".repeat(indent + 4) + "// suppress: " + suppress.joinToString(", ")
		}
		val ctx = ActionExecutionContext(functionBody, ArrayList(context.cpool), labelCount)
		for (i in 0 until functionBody.size) str += "\n" + " ".repeat(indent + 4) + functionBody[i].toBytecode(indent + 4, ctx)
		if (ctx.endLabel != null) str += "\n" + " ".repeat(indent + 4) + ctx.endLabel + ":"
		str += "\n" + " ".repeat(indent + 2) + "}"
		return str
	}
}

class ActionExtends(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun toString(indent: Int): String = "[ActionExtends]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "extends"
}

class ActionImplementsOp(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionImplementsOp]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "implementsOp"
}

class ActionThrow(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	override fun toString(indent: Int): String = "[ActionThrow]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "throw"
}

open class ActionTry(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {


	var catchInRegisterFlag: Boolean = false
	var finallyBlockFlag: Boolean = false
	var catchBlockFlag: Boolean = false
	var catchName: String? = null
	var catchRegister: Int = 0
	var tryBody = ArrayList<IAction>()
	var catchBody = ArrayList<IAction>()
	var finallyBody = ArrayList<IAction>()

	protected var labelCountTry: Int = 0
	protected var labelCountCatch: Int = 0
	protected var labelCountFinally: Int = 0

	override fun parse(data: SWFData): Unit {
		val flags = data.readUI8()
		catchInRegisterFlag = ((flags and 0x04) != 0)
		finallyBlockFlag = ((flags and 0x02) != 0)
		catchBlockFlag = ((flags and 0x01) != 0)
		val trySize = data.readUI16()
		val catchSize = data.readUI16()
		val finallySize = data.readUI16()
		if (catchInRegisterFlag) {
			catchRegister = data.readUI8()
		} else {
			catchName = data.readString()
		}
		val tryEndPosition: Int = data.position + trySize
		while (data.position < tryEndPosition) tryBody.add(data.readACTIONRECORD()!!)
		val catchEndPosition: Int = data.position + catchSize
		while (data.position < catchEndPosition) catchBody.add(data.readACTIONRECORD()!!)
		val finallyEndPosition: Int = data.position + finallySize
		while (data.position < finallyEndPosition) finallyBody.add(data.readACTIONRECORD()!!)
		labelCountTry = resolveOffsets(tryBody)
		labelCountCatch = resolveOffsets(catchBody)
		labelCountFinally = resolveOffsets(finallyBody)
	}

	override fun toString(indent: Int): String {
		var str: String = "[ActionTry] "
		str += if (catchInRegisterFlag) "Register: " + catchRegister else "Name: " + catchName
		if (tryBody.size != 0) {
			str += "\n" + " ".repeat(indent + 2) + "Try:"
			for (i in 0 until tryBody.size) {
				str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + tryBody[i].toString(indent + 4)
			}
		}
		if (catchBody.size != 0) {
			str += "\n" + " ".repeat(indent + 2) + "Catch:"
			for (i in 0 until catchBody.size) {
				str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + catchBody[i].toString(indent + 4)
			}
		}
		if (finallyBody.size != 0) {
			str += "\n" + " ".repeat(indent + 2) + "Finally:"
			for (i in 0 until finallyBody.size) {
				str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + finallyBody[i].toString(indent + 4)
			}
		}
		return str
	}

	override fun toBytecode(indent: Int, context: ActionExecutionContext): String {
		var str: String = if (lbl != null) lbl + ":\n" else ""
		var lf: String = ""
		if (tryBody.size != 0) {
			str += lf + " ".repeat(indent + 2) + "try {"
			val contextTry: ActionExecutionContext = ActionExecutionContext(tryBody, ArrayList(context.cpool), labelCountTry)
			for (i in 0 until tryBody.size) str += "\n" + " ".repeat(indent + 4) + tryBody[i].toBytecode(indent + 4, contextTry)
			if (contextTry.endLabel != null) str += "\n" + " ".repeat(indent + 4) + contextTry.endLabel + ":"
			str += "\n" + " ".repeat(indent + 2) + "}"
			lf = "\n"
		}
		if (catchBody.size != 0) {
			str += lf + " ".repeat(indent + 2) + "catch(" + (if (catchInRegisterFlag) "$" + catchRegister else catchName) + ") {"
			val contextCatch: ActionExecutionContext = ActionExecutionContext(catchBody, ArrayList(context.cpool), labelCountCatch)
			for (i in 0 until catchBody.size) str += "\n" + " ".repeat(indent + 4) + catchBody[i].toBytecode(indent + 4, contextCatch)
			if (contextCatch.endLabel != null) str += "\n" + " ".repeat(indent + 4) + contextCatch.endLabel + ":"
			str += "\n" + " ".repeat(indent + 2) + "}"
			lf = "\n"
		}
		if (finallyBody.size != 0) {
			str += lf + " ".repeat(indent + 2) + "finally {"
			val contextFinally: ActionExecutionContext = ActionExecutionContext(finallyBody, ArrayList(context.cpool), labelCountFinally)
			for (i in 0 until finallyBody.size) str += "\n" + " ".repeat(indent + 4) + finallyBody[i].toBytecode(indent + 4, contextFinally)
			if (contextFinally.endLabel != null) str += "\n" + " ".repeat(indent + 4) + contextFinally.endLabel + ":"
			str += "\n" + " ".repeat(indent + 2) + "}"
		}
		return str
	}
}
