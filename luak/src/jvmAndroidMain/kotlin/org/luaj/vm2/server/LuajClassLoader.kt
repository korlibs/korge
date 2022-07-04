/*******************************************************************************
 * Copyright (c) 2015 Luaj.org. All rights reserved.
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
package org.luaj.vm2.server

import java.io.*

/**
 * Class loader that can be used to launch a lua script in a Java VM that has a
 * unique set of classes for org.luaj classes.
 * <P>
 * *Note: This class is experimental and subject to change in future versions.*
</P> * <P>
 * By using a custom class loader per script, it allows the script to have
 * its own set of globals, including static values such as shared metatables
 * that cannot access lua values from other scripts because their classes are
 * loaded from different class loaders.  Thus normally unsafe libraries such
 * as luajava can be exposed to scripts in a server environment using these
 * techniques.
</P> * <P>
 * All classes in the package "org.luaj.vm2." are considered user classes, and
 * loaded into this class loader from their bytes in the class path. Other
 * classes are considered systemc classes and loaded via the system loader. This
 * class set can be extended by overriding [.isUserClass].
</P> * <P>
 * The [Launcher] interface is loaded as a system class by exception so
 * that the caller may use it to launch lua scripts.
</P> * <P>
 * By default [.NewLauncher] creates a subclass of [Launcher] of
 * type [DefaultLauncher] which creates debug globals, runs the script,
 * and prints the return values. This behavior can be changed by supplying a
 * different implementation class to [.NewLauncher] which must
 * extend [Launcher].
 *
 * @see Launcher
 *
 * @see .NewLauncher
 * @see .NewLauncher
 * @see DefaultLauncher
 *
 * @since luaj 3.0.1
</P> */
class LuajClassLoader : ClassLoader() {

    /** Local cache of classes loaded by this loader.  */
    internal var classes: MutableMap<String, Class<*>> = HashMap()


    override fun loadClass(classname: String): Class<*> {
        if (classes.containsKey(classname))
            return classes[classname]!!
        return if (!isUserClass(classname)) super.findSystemClass(classname) else loadAsUserClass(classname)
    }


    private fun loadAsUserClass(classname: String): Class<*> {
        val path = classname.replace('.', '/') + ".class"
        val `is` = getResourceAsStream(path)
        if (`is` != null) {
            try {
                val baos = ByteArrayOutputStream()
                val b = ByteArray(1024)
                var n = 0
                while (run { n = `is`.read(b); n } >= 0) baos.write(b, 0, n)
                val bytes = baos.toByteArray()
                val result = super.defineClass(classname, bytes, 0, bytes.size)
                classes[classname] = result
                return result
            } catch (e: IOException) {
                throw ClassNotFoundException("Read failed: $classname: $e")
            }

        }
        throw ClassNotFoundException("Not found: $classname")
    }

    companion object {

        /** String describing the luaj packages to consider part of the user classes  */
        internal val luajPackageRoot = "org.luaj.vm2."

        /** String describing the Launcher interface to be considered a system class  */
        internal val launcherInterfaceRoot = Launcher::class.java.name

        /**
         * Construct a [Launcher] instance that will load classes in
         * its own [LuajClassLoader] using a user-supplied implementation class
         * that implements [Launcher].
         * <P>
         * The [Launcher] that is returned will be a pristine luaj vm
         * whose classes are loaded into this loader including static variables
         * such as shared metatables, and should not be able to directly access
         * variables from other Launcher instances.
         *
         * @return instance of type 'launcher_class' that can be used to launch scripts.
         * @com.soywiz.luak.compat.java.Throws InstantiationException
         * @com.soywiz.luak.compat.java.Throws IllegalAccessException
         * @com.soywiz.luak.compat.java.Throws ClassNotFoundException
        </P> */

        @JvmOverloads
        fun NewLauncher(launcher_class: Class<out Launcher> = DefaultLauncher::class.java): Launcher =
            LuajClassLoader().loadAsUserClass(launcher_class.name).newInstance() as Launcher

        /**
         * Test if a class name should be considered a user class and loaded
         * by this loader, or a system class and loaded by the system loader.
         * @param classname Class name to test.
         * @return true if this should be loaded into this class loader.
         */

        fun isUserClass(classname: String): Boolean =
            classname.startsWith(luajPackageRoot) && !classname.startsWith(launcherInterfaceRoot)
    }
}
/**
 * Construct a default [Launcher] instance that will load classes in
 * its own [LuajClassLoader] using the default implementation class
 * [DefaultLauncher].
 * <P>
 * The [Launcher] that is returned will be a pristine luaj vm
 * whose classes are loaded into this loader including static variables
 * such as shared metatables, and should not be able to directly access
 * variables from other Launcher instances.
 *
 * @return [Launcher] instance that can be used to launch scripts.
 * @com.soywiz.luak.compat.java.Throws InstantiationException
 * @com.soywiz.luak.compat.java.Throws IllegalAccessException
 * @com.soywiz.luak.compat.java.Throws ClassNotFoundException
</P> */
