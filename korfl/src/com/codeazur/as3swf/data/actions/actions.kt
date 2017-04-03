package com.codeazur.as3swf.data.actions

import com.codeazur.as3swf.SWFData
import java.util.*

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
	var endLabel: String? = null

	init {
		for (action in actions) {
			if (action is IActionBranch) {
				if (action.branchIndex == -1) {
					endLabel = "L" + (labelCount + 1)
					break
				}
			}
		}
	}
}

class ActionUnknown(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	override fun parse(data: SWFData) {
		if (length > 0) {
			data.skipBytes(length)
		}
	}

	override fun toString(indent: Int): String {
		return "[????] Code: " + code.toString(16) + ", Length: " + length
	}
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
