package com.codeazur.as3swf.data.actions.swf7

import com.codeazur.as3swf.SWFData
import com.codeazur.as3swf.data.actions.Action
import com.codeazur.as3swf.data.actions.ActionExecutionContext
import com.codeazur.as3swf.data.actions.IAction
import java.util.*

class ActionCastOp(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		val CODE = 0x2b
	}

	override fun toString(indent: Int): String = "[ActionCastOp]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "castOp"
}

class ActionDefineFunction2(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x8e
	}

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
	companion object {
		const val CODE = 0x69
	}

	override fun toString(indent: Int): String = "[ActionExtends]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "extends"
}

class ActionImplementsOp(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x2c
	}

	override fun toString(indent: Int): String = "[ActionImplementsOp]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "implementsOp"
}

class ActionThrow(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x2a
	}

	override fun toString(indent: Int): String = "[ActionThrow]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "throw"
}

open class ActionTry(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x8f
	}

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
