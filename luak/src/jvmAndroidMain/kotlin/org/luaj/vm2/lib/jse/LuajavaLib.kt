/*******************************************************************************
 * Copyright (c) 2009 Luaj.org. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.luaj.vm2.lib.jse


import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.VarArgFunction
import java.lang.reflect.*

/**
 * Subclass of [LibFunction] which implements the features of the luajava package.
 *
 *
 * Luajava is an approach to mixing lua and java using simple functions that bind
 * java classes and methods to lua dynamically.  The API is documented on the
 * [luajava](http://www.keplerproject.org/luajava/) documentation pages.
 *
 *
 *
 * Typically, this library is included as part of a call to
 * [org.luaj.vm2.lib.jse.JsePlatform.standardGlobals]
 * <pre> `Globals globals = JsePlatform.standardGlobals();
 * System.out.println( globals.get("luajava").get("bindClass").call( LuaValue.valueOf("java.lang.System") ).invokeMethod("currentTimeMillis") );
` *  </pre>
 *
 *
 * To instantiate and use it directly,
 * link it into your globals table via [Globals.load] using code such as:
 * <pre> `Globals globals = new Globals();
 * globals.load(new JseBaseLib());
 * globals.load(new PackageLib());
 * globals.load(new LuajavaLib());
 * globals.load(
 * "sys = luajava.bindClass('java.lang.System')\n"+
 * "print ( sys:currentTimeMillis() )\n", "main.lua" ).call();
` *  </pre>
 *
 *
 *
 * The `luajava` library is available
 * on all JSE platforms via the call to [org.luaj.vm2.lib.jse.JsePlatform.standardGlobals]
 * and the luajava api's are simply invoked from lua.
 * Because it makes extensive use of Java's reflection API, it is not available
 * on JME, but can be used in Android applications.
 *
 *
 * This has been implemented to match as closely as possible the behavior in the corresponding library in C.
 *
 * @see LibFunction
 *
 * @see org.luaj.vm2.lib.jse.JsePlatform
 * //@see org.luaj.vm2.lib.jme.JmePlatform
 *
 * @see LuaC
 *
 * @see CoerceJavaToLua
 *
 * @see CoerceLuaToJava
 *
 * @see [http://www.keplerproject.org/luajava/manual.html.luareference](http://www.keplerproject.org/luajava/manual.html.luareference)
 */
class LuajavaLib : VarArgFunction() {

    override fun invoke(args: Varargs): Varargs {
        try {
            when (opcode) {
                INIT -> {
                    // LuaValue modname = args.arg1();
                    val env = args.arg(2)
                    val t = LuaTable()
                    bind(t, { LuajavaLib() }, NAMES, BINDCLASS)
                    env["luajava"] = t
                    env["package"]["loaded"]["luajava"] = t
                    return t
                }
                BINDCLASS -> {
                    val clazz = classForName(args.checkjstring(1))
                    return JavaClass.forClass(clazz)
                }
                NEWINSTANCE, NEW -> {
                    // get constructor
                    val c = args.checkvalue(1)
                    val clazz =
                        if (opcode == NEWINSTANCE) classForName(c.tojstring()) else c.checkuserdata(Class::class) as Class<*>?
                    val consargs = args.subargs(2)
                    return JavaClass.forClass(clazz!!).getConstructor()!!.invoke(consargs)
                }

                CREATEPROXY -> {
                    val niface = args.narg() - 1
                    if (niface <= 0)
                        throw LuaError("no interfaces")
                    val lobj = args.checktable(niface + 1)

                    // get the interfaces
                    val ifaces = Array(niface) { classForName(args.checkjstring(it + 1)) }

                    // create the invocation handler
                    val handler = ProxyInvocationHandler(lobj!!)

                    // create the proxy object
                    val proxy = Proxy.newProxyInstance(javaClass.classLoader, ifaces, handler)

                    // return the proxy
                    return LuaValue.userdataOf(proxy)
                }
                LOADLIB -> {
                    // get constructor
                    val classname = args.checkjstring(1)
                    val methodname = args.checkjstring(2)
                    val clazz = classForName(classname)
                    val method = clazz.getMethod(methodname!!, *arrayOf())
                    val result = method.invoke(clazz, *arrayOf())
                    return result as? LuaValue ?: LuaValue.NIL
                }
                else -> throw LuaError("not yet supported: $this")
            }
        } catch (e: LuaError) {
            throw e
        } catch (ite: InvocationTargetException) {
            throw LuaError(ite.targetException)
        } catch (e: Exception) {
            throw LuaError(e)
        }

    }

    // load classes using app loader to allow luaj to be used as an extension

    protected fun classForName(name: String?): Class<*> {
        return Class.forName(name, true, ClassLoader.getSystemClassLoader())
    }

    class ProxyInvocationHandler constructor(private val lobj: LuaValue) : InvocationHandler {


        override fun invoke(proxy: Any, method: Method, args: kotlin.Array<Any>?): Any? {
            val name = method.name
            val func = lobj[name]
            if (func.isnil())
                return null
            val isvarargs = method.modifiers and METHOD_MODIFIERS_VARARGS != 0
            var n = if (args != null) args!!.size else 0
            val v: kotlin.Array<LuaValue>
            if (isvarargs) {
                val o = args!![--n]
                val m = java.lang.reflect.Array.getLength(o)
                v = arrayOfNulls<LuaValue>(n + m) as kotlin.Array<LuaValue>
                for (i in 0 until n) v[i] = CoerceJavaToLua.coerce(args!![i])
                for (i in 0 until m) v[i + n] = CoerceJavaToLua.coerce(java.lang.reflect.Array.get(o, i))
            } else {
                v = Array<LuaValue>(n) { CoerceJavaToLua.coerce(args!![it]) }
            }
            val result = func.invoke(v).arg1()
            return CoerceLuaToJava.coerce(result, method.returnType)
        }
    }

    companion object {

        internal const val INIT = 0
        internal const val BINDCLASS = 1
        internal const val NEWINSTANCE = 2
        internal const val NEW = 3
        internal const val CREATEPROXY = 4
        internal const val LOADLIB = 5

        @kotlin.jvm.JvmField
        internal val NAMES = arrayOf("bindClass", "newInstance", "new", "createProxy", "loadLib")

        internal const val METHOD_MODIFIERS_VARARGS = 0x80
    }

}
