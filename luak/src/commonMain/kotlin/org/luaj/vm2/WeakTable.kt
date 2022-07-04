/*******************************************************************************
 * Copyright (c) 2009-2011, 2013 Luaj.org. All rights reserved.
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
package org.luaj.vm2

import org.luaj.vm2.LuaTable.Slot
import org.luaj.vm2.LuaTable.StrongSlot
import org.luaj.vm2.internal.*

/**
 * Subclass of [LuaTable] that provides weak key and weak value semantics.
 *
 *
 * Normally these are not created directly, but indirectly when changing the mode
 * of a [LuaTable] as lua script executes.
 *
 *
 * However, calling the constructors directly when weak tables are required from
 * Java will reduce overhead.
 */
/**
 * Construct a table with weak keys, weak values, or both
 * @param weakkeys true to let the table have weak keys
 * @param weakvalues true to let the table have weak values
 */
class WeakTable(
    private val weakkeys: Boolean,
    private val weakvalues: Boolean,
    private val backing: LuaValue
) : Metatable {

    override fun useWeakKeys(): Boolean = weakkeys
    override fun useWeakValues(): Boolean = weakvalues
    override fun toLuaValue(): LuaValue = backing

    override fun entry(key: LuaValue, value: LuaValue): Slot? {
        var value: LuaValue? = value
        value = value?.strongvalue()
        return when {
            value == null -> null
            weakkeys && !(key.isnumber() || key.isstring() || key.isboolean()) -> when {
                weakvalues && !(value.isnumber() || value.isstring() || value.isboolean()) -> WeakKeyAndValueSlot(key, value, null)
                else -> WeakKeySlot(key, value, null)
            }
            weakvalues && !(value.isnumber() || value.isstring() || value.isboolean()) -> WeakValueSlot(key, value, null)
            else -> LuaTable.defaultEntry(key, value)
        }
    }

    abstract class WeakSlot protected constructor(
        protected var key: Any?,
        protected var value: Any?,
        protected var next: Slot?
    ) : Slot {

        abstract override fun keyindex(hashMask: Int): Int

        abstract fun set(value: LuaValue): Slot

        override fun first(): StrongSlot? {
            val key = strongkey()
            val value = strongvalue()
            return when {
                key != null && value != null -> LuaTable.NormalEntry(key, value)
                else -> {
                    this.key = null
                    this.value = null
                    null
                }
            }
        }

        override fun find(key: LuaValue): StrongSlot? = first()?.find(key)
        override fun keyeq(key: LuaValue?): Boolean = first().let { first -> first != null && first.keyeq(key) }
        override fun rest(): Slot? = next
        // Integer keys can never be weak.
        override fun arraykey(max: Int): Int = 0
        override fun set(target: StrongSlot, value: LuaValue): Slot {
            val key = strongkey()
            return when {
                key != null && target.find(key) != null -> set(value)
                key != null -> {
                    // Our key is still good.
                    next = next!!.set(target, value)
                    this
                }
                else -> // our key was dropped, remove ourselves from the chain.
                    next!!.set(target, value)!!
            }
        }

        override fun add(entry: Slot): Slot? {
            next = if (next != null) next!!.add(entry) else entry
            return if (strongkey() != null && strongvalue() != null) this else next
        }

        override fun remove(target: StrongSlot): Slot {
            val key = strongkey()
            return when {
                key == null -> next!!.remove(target)!!
                target.keyeq(key) -> {
                    this.value = null
                    this
                }
                else -> {
                    next = next!!.remove(target)
                    this
                }
            }
        }

        override fun relink(rest: Slot?): Slot? {
            return if (strongkey() != null && strongvalue() != null) {
                if (rest == null && this.next == null) {
                    this
                } else {
                    copy(rest)
                }
            } else {
                rest
            }
        }

        open fun strongkey(): LuaValue? = key as LuaValue?
        open fun strongvalue(): LuaValue? = value as LuaValue?
        protected abstract fun copy(next: Slot?): WeakSlot
    }

    class WeakKeySlot : WeakSlot {

        private val keyhash: Int

        constructor(key: LuaValue, value: LuaValue, next: Slot?) : super(weaken(key), value, next) {
            keyhash = key.hashCode()
        }

        protected constructor(copyFrom: WeakKeySlot, next: Slot?) : super(copyFrom.key, copyFrom.value, next) {
            this.keyhash = copyFrom.keyhash
        }

        override fun keyindex(mask: Int): Int = LuaTable.hashmod(keyhash, mask)
        override fun set(value: LuaValue): Slot = this.also { this.value = value }
        override fun strongkey(): LuaValue? = strengthen(key)
        override fun copy(rest: Slot?): WeakSlot = WeakKeySlot(this, rest)
    }

    internal class WeakValueSlot : WeakSlot {

        constructor(key: LuaValue, value: LuaValue, next: Slot?) : super(key, weaken(value), next) {}

        protected constructor(copyFrom: WeakValueSlot, next: Slot?) : super(copyFrom.key, copyFrom.value, next) {}

        override fun keyindex(mask: Int): Int = LuaTable.hashSlot(strongkey()!!, mask)

        override fun set(value: LuaValue): Slot {
            this.value = weaken(value)
            return this
        }

        override fun strongvalue(): LuaValue? = strengthen(value)
        override fun copy(next: Slot?): WeakSlot = WeakValueSlot(this, next)
    }

    internal class WeakKeyAndValueSlot : WeakSlot {

        private val keyhash: Int

        constructor(key: LuaValue, value: LuaValue, next: Slot?) : super(weaken(key), weaken(value), next) {
            keyhash = key.hashCode()
        }

        protected constructor(copyFrom: WeakKeyAndValueSlot, next: Slot?) : super(copyFrom.key, copyFrom.value, next) {
            keyhash = copyFrom.keyhash
        }

        override fun keyindex(hashMask: Int): Int = LuaTable.hashmod(keyhash, hashMask)
        override fun set(value: LuaValue): Slot {
            this.value = weaken(value)
            return this
        }

        override fun strongkey(): LuaValue? = strengthen(key)
        override fun strongvalue(): LuaValue? = strengthen(value)
        override fun copy(next: Slot?): WeakSlot = WeakKeyAndValueSlot(this, next)
    }

    /** Internal class to implement weak values.
     * @see WeakTable
     */
    open class WeakValue(value: LuaValue) : LuaValue() {
        var ref: WeakReference<*> = WeakReference(value)

        override fun type(): Int = illegal("type", "weak value")
        override fun typename(): String = illegal("typename", "weak value")
        override fun toString(): String = "weak<" + ref.get() + ">"
        override fun strongvalue(): LuaValue? = ref.get() as LuaValue?
        override fun raweq(rhs: LuaValue): Boolean {
            val o = ref.get()
            return o != null && rhs.raweq((o as LuaValue?)!!)
        }
    }

    /** Internal class to implement weak userdata values.
     * @see WeakTable
     */
    class WeakUserdata(value: LuaValue) : WeakValue(value) {
        private val ob: WeakReference<*> =
            WeakReference(value.touserdata())
        private val mt: LuaValue? = value.getmetatable()

        override fun strongvalue(): LuaValue? {
            val u = ref.get()
            if (u != null) return u as LuaValue?
            val o = ob.get()
            return if (o != null) {
                val ud = LuaValue.userdataOf(o, mt)
                ref = WeakReference(ud)
                ud
            } else {
                null
            }
        }
    }

    override fun wrap(value: LuaValue): LuaValue = if (weakvalues) weaken(value) else value

    override fun arrayget(array: Array<LuaValue?>, index: Int): LuaValue? {
        var value: LuaValue? = array[index]
        if (value != null) {
            value = strengthen(value)
            if (value == null) {
                array[index] = null
            }
        }
        return value
    }

    companion object {

        fun make(weakkeys: Boolean, weakvalues: Boolean): LuaTable {
            val mode: LuaString = when {
                weakkeys && weakvalues -> LuaString.valueOf("kv")
                weakkeys -> LuaString.valueOf("k")
                weakvalues -> LuaString.valueOf("v")
                else -> return LuaValue.tableOf()
            }
            val table = LuaValue.tableOf()
            val mt = LuaValue.tableOf(arrayOf(LuaValue.MODE, mode))
            table.setmetatable(mt)
            return table
        }

        /**
         * Self-sent message to convert a value to its weak counterpart
         * @param value value to convert
         * @return [LuaValue] that is a strong or weak reference, depending on type of `value`
         */
        protected fun weaken(value: LuaValue): LuaValue = when (value.type()) {
            LuaValue.TFUNCTION, LuaValue.TTHREAD, LuaValue.TTABLE -> WeakValue(value)
            LuaValue.TUSERDATA -> WeakUserdata(value)
            else -> value
        }

        /**
         * Unwrap a LuaValue from a WeakReference and/or WeakUserdata.
         * @param ref reference to convert
         * @return LuaValue or null
         * @see .weaken
         */
        protected fun strengthen(ref: Any?): LuaValue? {
            var ref = ref
            if (ref is WeakReference<*>) ref = ref.get()
            return if (ref is WeakValue) ref.strongvalue() else ref as LuaValue?
        }
    }
}
