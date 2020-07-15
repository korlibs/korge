package com.soywiz.kds

import org.objectweb.asm.*
import kotlin.reflect.*
import kotlin.test.*

//@RunWith(PowerMockRunner::class)
// Maybe using agents? http://web.archive.org/web/20110427020009/http://blogs.captechconsulting.com/blog/david-tiller/not-so-secret-java-agents-part-1
// https://stackoverflow.com/questions/2737285/is-there-a-way-to-obtain-the-bytecode-for-a-class-at-runtime
class DoubleArrayListExtTest {
    @Test
    fun test() {
        val ci = ClassInfo<DoubleArrayList>()
        assertEquals(2, ci.getMethodMethodCalls("get", "(I)Ljava/lang/Double;").size)
        assertEquals(1, ci.getMethodMethodCalls("get", "(I)Ljava/lang/Object;").size)
        assertEquals(0, ci.getMethodMethodCalls("getAt", "(I)D").size)
    }

    class ClassInfo(val bytecode: ByteArray) {
        companion object {
            inline operator fun <reified T> invoke(): ClassInfo = invoke(T::class)
            operator fun invoke(clazz: KClass<*>): ClassInfo =
                ClassInfo(clazz.java.classLoader.getResourceAsStream("${clazz.java.canonicalName.replace('.', '/')}.class")?.readBytes() ?: error("Can't get class bytecode"))
        }

        fun getMethodMethodCalls(methodName: String, methodDescriptor: String): List<MethodCall> {
            val cr = ClassReader(bytecode)
            val calls = arrayListOf<MethodCall>()
            var methodFound = false
            cr.accept(object : ClassVisitor(Opcodes.ASM8) {
                override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                    //println("methodName: $methodName == $name")
                    //println("methodDescriptor: $methodDescriptor == $descriptor")
                    if (methodName == name && methodDescriptor == descriptor) {
                        methodFound = true
                        return object : MethodVisitor(Opcodes.ASM8) {
                            override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean) {
                                calls += MethodCall(owner, name, descriptor)
                                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
                            }
                        }
                    }
                    return null
                }
            }, 0)
            if (!methodFound) error("Can't find method $methodName$methodDescriptor")
            return calls
        }
    }

    data class MethodCall(val owner: String, val name: String, val descriptor: String){
        override fun toString(): String = "$owner$name$descriptor"
    }

    private fun getVersion(): Int {
        var version = System.getProperty("java.version")
        if (version.startsWith("1.")) {
            version = version.substring(2, 3)
        } else {
            val dot = version.indexOf(".")
            if (dot != -1) {
                version = version.substring(0, dot)
            }
        }
        return version.toInt()
    }
}
