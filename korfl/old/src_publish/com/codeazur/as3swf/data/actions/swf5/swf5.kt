package com.codeazur.as3swf.data.actions.swf5

import com.codeazur.as3swf.SWFData
import com.codeazur.as3swf.data.actions.Action
import com.codeazur.as3swf.data.actions.ActionExecutionContext
import com.codeazur.as3swf.data.actions.IAction
import java.util.*

class ActionAdd2(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x47
	}

	override fun toString(indent: Int): String = "[ActionAdd2]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "add2"
}

class ActionBitAnd(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x60
	}

	override fun toString(indent: Int): String = "[ActionBitAnd]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "bitAnd"
}

class ActionBitLShift(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x63
	}

	override fun toString(indent: Int): String = "[ActionBitLShift]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "bitLShift"
}

class ActionBitOr(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x61
	}

	override fun toString(indent: Int): String = "[ActionBitOr]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "bitOr"
}

class ActionBitRShift(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x64
	}

	override fun toString(indent: Int): String = "[ActionBitRShift]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "bitRShift"
}

class ActionBitURShift(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x65
	}

	override fun toString(indent: Int): String = "[ActionBitURShift]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "bitURShift"
}

class ActionBitXor(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x62
	}

	override fun toString(indent: Int): String = "[ActionBitXor]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "bitXor"
}

class ActionCallFunction(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x3d
	}

	override fun toString(indent: Int): String = "[ActionCallFunction]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "callFunction"
}

class ActionCallMethod(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x52
	}

	override fun toString(indent: Int): String = "[ActionCallMethod]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "callMethod"
}

class ActionConstantPool(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x88
	}

	var constants = ArrayList<String>()

	override fun parse(data: SWFData): Unit {
		val count = data.readUI16()
		for (i in 0 until count) {
			constants.add(data.readString())
		}
	}

	override fun publish(data: SWFData): Unit {
		val body = SWFData()
		body.writeUI16(constants.size)
		for (i in 0 until constants.size) {
			body.writeString(constants[i])
		}
		write(data, body)
	}

	override fun clone(): IAction {
		val action = ActionConstantPool(code, length, pos)
		for (i in 0 until constants.size) {
			action.constants.add(constants[i])
		}
		return action
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
	companion object {
		const val CODE = 0x51
	}

	override fun toString(indent: Int): String = "[ActionDecrement]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "decrement"
}

open class ActionDefineFunction(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x9b
	}

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

	override fun publish(data: SWFData): Unit {
		val body = SWFData()
		body.writeString(functionName)
		body.writeUI16(functionParams.size)
		for (i in 0 until functionParams.size) {
			body.writeString(functionParams[i])
		}
		val bodyActions = SWFData()
		for (i in 0 until functionBody.size) {
			bodyActions.writeACTIONRECORD(functionBody[i])
		}
		body.writeUI16(bodyActions.length)
		write(data, body)
		data.writeBytes(bodyActions)
	}

	override fun clone(): IAction {
		val action = ActionDefineFunction(code, length, pos)
		action.functionName = functionName
		for (i in 0 until functionParams.size) {
			action.functionParams.add(functionParams[i])
		}
		for (i in 0 until functionBody.size) {
			action.functionBody.add(functionBody[i].clone())
		}
		return action
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
	companion object {
		const val CODE = 0x3c
	}

	override fun toString(indent: Int): String = "[ActionDefineLocal]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "defineLocal"
}

class ActionDefineLocal2(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x41
	}

	override fun toString(indent: Int): String = "[ActionDefineLocal2]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "defineLocal2"
}

class ActionDelete(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x3a
	}

	override fun toString(indent: Int): String = "[ActionDelete]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "delete"
}

class ActionDelete2(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x3b
	}

	override fun toString(indent: Int): String = "[ActionDelete2]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "delete2"
}

class ActionEnumerate(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x46
	}

	override fun toString(indent: Int): String = "[ActionEnumerate]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "enumerate"
}

class ActionEquals2(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x49
	}

	override fun toString(indent: Int): String = "[ActionEquals2]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "equals2"
}

class ActionGetMember(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x4e
	}

	override fun toString(indent: Int): String = "[ActionGetMember]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "getMember"
}

class ActionIncrement(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x50
	}

	override fun toString(indent: Int): String = "[ActionIncrement]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "increment"
}

class ActionInitArray(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x42
	}

	override fun toString(indent: Int): String = "[ActionInitArray]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "initArray"
}

class ActionInitObject(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x43
	}

	override fun toString(indent: Int): String = "[ActionInitObject]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "initObject"
}

class ActionLess2(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x48
	}

	override fun toString(indent: Int): String = "[ActionLess2]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "less2"
}

class ActionModulo(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x3f
	}

	override fun toString(indent: Int): String = "[ActionModulo]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "modulo"
}

class ActionNewMethod(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x53
	}

	override fun toString(indent: Int): String = "[ActionNewMethod]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "newMethod"
}

class ActionNewObject(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x40
	}

	override fun toString(indent: Int): String = "[ActionNewObject]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "newObject"
}

class ActionPushDuplicate(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x4c
	}

	override fun toString(indent: Int): String = "[ActionPushDuplicate]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "pushDuplicate"
}

class ActionReturn(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x3e
	}

	override fun toString(indent: Int): String = "[ActionReturn]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "return"
}

class ActionSetMember(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x4f
	}

	override fun toString(indent: Int): String = "[ActionSetMember]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "setMember"
}

class ActionStackSwap(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x4d
	}

	override fun toString(indent: Int): String {
		return "[ActionStackSwap]"
	}

	override fun toBytecode(indent: Int, context: ActionExecutionContext): String {
		return toBytecodeLabel(indent) + "stackSwap"
	}
}

class ActionStoreRegister(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x87
	}

	var registerNumber: Int = 0

	override fun parse(data: SWFData): Unit {
		registerNumber = data.readUI8()
	}

	override fun publish(data: SWFData): Unit {
		val body = SWFData()
		body.writeUI8(registerNumber)
		write(data, body)
	}

	override fun clone(): IAction {
		val action = ActionStoreRegister(code, length, pos)
		action.registerNumber = registerNumber
		return action
	}

	override fun toString(indent: Int): String = "[ActionStoreRegister] RegisterNumber: " + registerNumber
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "store $" + registerNumber
}

class ActionTargetPath(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x45
	}

	override fun toString(indent: Int): String = "[ActionTargetPath]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "targetPath"
}

class ActionToNumber(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x4a
	}

	override fun toString(indent: Int): String = "[ActionToNumber]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "toNumber"
}

class ActionToString(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x4b
	}

	override fun toString(indent: Int): String = "[ActionToString]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "toString"
}

class ActionTypeOf(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x44
	}

	override fun toString(indent: Int): String = "[ActionTypeOf]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "typeOf"
}

open class ActionWith(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x94
	}

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

	override fun publish(data: SWFData): Unit {
		val body = SWFData()
		val bodyActions = SWFData()
		for (i in 0 until withBody.size) bodyActions.writeACTIONRECORD(withBody[i])
		body.writeUI16(bodyActions.length)
		body.writeBytes(bodyActions)
		write(data, body)
	}

	override fun clone(): IAction {
		val action = ActionWith(code, length, pos)
		for (i in 0 until withBody.size) action.withBody.add(withBody[i].clone())
		return action
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
