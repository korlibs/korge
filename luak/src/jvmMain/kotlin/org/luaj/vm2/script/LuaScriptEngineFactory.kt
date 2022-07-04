/*******************************************************************************
 * Copyright (c) 2008 LuaJ. All rights reserved.
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
package org.luaj.vm2.script

import javax.script.ScriptEngine
import javax.script.ScriptEngineFactory

/**
 * Jsr 223 scripting engine factory.
 *
 * Exposes metadata to support the lua language, and constructs
 * instances of LuaScriptEngine to handl lua scripts.
 */
class LuaScriptEngineFactory : ScriptEngineFactory {
    override fun getEngineName(): String = scriptEngine.get(ScriptEngine.ENGINE).toString()
    override fun getEngineVersion(): String = scriptEngine.get(ScriptEngine.ENGINE_VERSION).toString()
    override fun getExtensions(): List<String> = EXTENSIONS
    override fun getMimeTypes(): List<String> = MIMETYPES
    override fun getNames(): List<String> = NAMES
    override fun getLanguageName(): String = scriptEngine.get(ScriptEngine.LANGUAGE).toString()
    override fun getLanguageVersion(): String = scriptEngine.get(ScriptEngine.LANGUAGE_VERSION).toString()
    override fun getParameter(key: String): Any = scriptEngine.get(key).toString()

    override fun getMethodCallSyntax(obj: String, m: String, vararg args: String): String {
        val sb = StringBuffer()
        sb.append("$obj:$m(")
        val len = args.size
        for (i in 0 until len) {
            if (i > 0) {
                sb.append(',')
            }
            sb.append(args[i])
        }
        sb.append(")")
        return sb.toString()
    }

    override fun getOutputStatement(toDisplay: String): String = "print($toDisplay)"

    override fun getProgram(vararg statements: String): String {
        val sb = StringBuffer()
        val len = statements.size
        for (i in 0 until len) {
            if (i > 0) {
                sb.append('\n')
            }
            sb.append(statements[i])
        }
        return sb.toString()
    }

    override fun getScriptEngine(): ScriptEngine = LuaScriptEngine()

    companion object {
        private val EXTENSIONS = listOf("lua", ".lua")
        private val MIMETYPES = listOf("text/lua", "application/lua")
        private val NAMES = listOf("lua", "luaj")
    }
}
