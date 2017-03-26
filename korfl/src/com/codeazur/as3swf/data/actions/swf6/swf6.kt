package com.codeazur.as3swf.data.actions.swf6

import com.codeazur.as3swf.data.actions.Action
import com.codeazur.as3swf.data.actions.ActionExecutionContext
import com.codeazur.as3swf.data.actions.IAction

class ActionEnumerate2(code: Int, length: Int, pos: Int) : Action(code, length, pos) {
	companion object {
		val CODE = 0x55
	}

	override fun toString(indent: Int): String = "[ActionEnumerate2]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "enumerate2"
}

class ActionGreater(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x67
	}

	override fun toString(indent: Int): String = "[ActionGreater]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "greater"
}

class ActionInstanceOf(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		val CODE = 0x54
	}

	override fun toString(indent: Int): String = "[ActionInstanceOf]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "instanceOf"
}

class ActionStrictEquals(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		const val CODE = 0x66
	}

	override fun toString(indent: Int): String = "[ActionStrictEquals]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "strictEquals"
}

class ActionStringGreater(code: Int, length: Int, pos: Int) : Action(code, length, pos), IAction {
	companion object {
		val CODE = 0x68
	}

	override fun toString(indent: Int): String = "[ActionStringGreater]"
	override fun toBytecode(indent: Int, context: ActionExecutionContext): String = toBytecodeLabel(indent) + "stringGreater"
}
