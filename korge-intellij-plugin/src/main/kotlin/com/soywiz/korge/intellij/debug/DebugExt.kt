package com.soywiz.korge.intellij.debug

import com.sun.jdi.*
import java.io.*

// @TODO: com.intellij.debugger.impl.DebuggerUtilsImpl.readBytesArray could be much faster with less allocations by using this method. Make a PR?
fun ArrayReference.convertToLocalBytes(thread: ThreadReference? = null): kotlin.ByteArray {
	/*
	val base64Class = this.virtualMachine().getRemoteClass(Base64::class.java, thread) ?: error("Can't find Base64 class")
	val encoder = base64Class.invoke("getEncoder", listOf(), thread = thread) as ObjectReference
	val str = encoder.invoke("encodeToString", listOf(this), thread = thread) as StringReference
	return Base64.getDecoder().decode(str.value())
	*/
	return DebuggerUtilsImplExt.fastReadBytesArray(this, thread) ?: error("Couldn't load ByteArray")
}

fun ObjectReference.debugToLocalInstanceViaSerialization(thread: ThreadReference? = null): Any? {
	return this.debugSerialize(thread).debugDeserialize()
}

inline fun <reified T> Type?.instanceOf(): Boolean = instanceOf(T::class.java)
fun Type?.instanceOf(clazz: Class<*>): Boolean = instanceOf(clazz.name)

fun Type?.instanceOf(name: String): Boolean {
	val type = this ?: return false
	if (type is ClassType) {
		//println("TYPE: ${type.signature()} ${type.name()} :: ${Bitmap::class.java.name}")
		if (type.name() == name) return true
		if (type.superclass().instanceOf(name)) return true
		if (type.allInterfaces().any { it.name() == name }) return true
	}
	return false
}

fun ObjectReference.debugSerialize(thread: ThreadReference? = null): ByteArray {
	val vm = this.virtualMachine()

	val baosClass = vm.getRemoteClass(ByteArrayOutputStream::class.java, thread) ?: error("Cant't find ByteArrayOutputStream")
	val baosClassConstructor =
		baosClass.methods().firstOrNull { it.isConstructor && it.arguments().size == 0 } ?: error("Can't find ByteArrayOutputStream constructor")
	val baos = baosClass.newInstance(thread ?: vm.anyThread(), baosClassConstructor, listOf(), ClassType.INVOKE_SINGLE_THREADED)

	val oosClass = vm.getRemoteClass(ObjectOutputStream::class.java, thread) ?: error("Can't find ObjectOutputStream")
	val oosClassConstructor =
		oosClass.methods().firstOrNull { it.isConstructor && it.arguments().size == 1 } ?: error("Can't find ObjectOutputStream constructor")
	val oos = oosClass.newInstance(thread ?: vm.anyThread(), oosClassConstructor, listOf(baos), ClassType.INVOKE_SINGLE_THREADED)

	oos.invoke("writeObject", listOf(this), thread = thread)

	return (baos.invoke("toByteArray", listOf(), thread = thread) as ArrayReference).convertToLocalBytes(thread)
}

fun ByteArray.debugDeserialize(): Any? = ObjectInputStream(this.inputStream()).readObject()

fun VirtualMachine.getRemoteClass(clazz: Class<*>, thread: ThreadReference? = null): ClassType? = getRemoteClass(clazz.name, thread)

fun VirtualMachine.getRemoteClass(clazz: String, thread: ThreadReference? = null): ClassType? {
	val clazzType = classesByName("java.lang.Class").firstOrNull() ?: error("Can't find java.lang.Class")
	val clazzClassType = (clazzType as? ClassType?) ?: error("Invalid java.lang.Class")
	val realClazz = clazzClassType.invoke("forName", listOf(mirrorOf(clazz)), thread = thread) as ClassObjectReference
	return realClazz.reflectedType() as ClassType?
}

fun VirtualMachine.anyThread() = allThreads().first()

fun ClassType.invoke(methodName: String, args: List<Value>, signature: String? = null, thread: ThreadReference? = null): Value {
	try {
		val method = if (signature != null) this.methodsByName(methodName, signature).first() else this.methodsByName(methodName).first()
		return this.invokeMethod(thread ?: this.virtualMachine().anyThread(), method, args, ClassType.INVOKE_SINGLE_THREADED)
	} catch (e: IncompatibleThreadStateException) {
		throw e
	}
}

fun ObjectReference.invoke(methodName: String, args: List<Value> = listOf(), signature: String? = null, thread: ThreadReference? = null): Value {
	try {
		val method =
			if (signature != null) this.referenceType().methodsByName(methodName, signature).first() else this.referenceType().methodsByName(methodName).first()
		return this.invokeMethod(thread ?: this.virtualMachine().anyThread(), method, args, ClassType.INVOKE_SINGLE_THREADED)
	} catch (e: IncompatibleThreadStateException) {
		throw e
	}
}

fun ClassType.getField(fieldName: String): Value = this.getValue(this.fieldByName(fieldName))
fun ObjectReference.getField(fieldName: String): Value = this.getValue(this.referenceType().fieldByName(fieldName))

fun Value.int(): Int? = if (this is PrimitiveValue) this.intValue() else null
fun Value.int(default: Int): Int = if (this is PrimitiveValue) this.intValue() else default

fun Value.bool(): Boolean? = if (this is PrimitiveValue) this.booleanValue() else null
fun Value.bool(default: Boolean): Boolean = if (this is PrimitiveValue) this.booleanValue() else default

/*
inline fun <reified T> Value.asLocalType() = asLocalType(T::class.java)

fun <T> Value.asLocalType(clazz: Class<T>): T {
	return ProxyFactory().also {
		it.superclass = clazz
	}.create(emptyArray(), arrayOf()) { self, thisMethod, proceed, args ->
		println("CALLING $args")
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	} as T
}
*/
