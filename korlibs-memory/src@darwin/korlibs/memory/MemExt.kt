@file:OptIn(ExperimentalForeignApi::class)

package korlibs.memory

import kotlinx.cinterop.*

//private val emptyPinnedBoolean = BooleanArray(1).pin()
private val emptyAddressByte = ByteArray(1).pin().addressOf(0)
private val emptyAddressShort = ShortArray(1).pin().addressOf(0)
private val emptyAddressInt = IntArray(1).pin().addressOf(0)
private val emptyAddressFloat = FloatArray(1).pin().addressOf(0)
private val emptyAddressDouble = DoubleArray(1).pin().addressOf(0)
private val emptyAddressLong = LongArray(1).pin().addressOf(0)

private val emptyAddressUByte = UByteArray(1).pin().addressOf(0)
private val emptyAddressUShort = UShortArray(1).pin().addressOf(0)
private val emptyAddressUInt = UIntArray(1).pin().addressOf(0)
private val emptyAddressULong = ULongArray(1).pin().addressOf(0)

// Missing CharArray? (equivalent to UShort) and BooleanArray (equivalent to Byte)
//val Pinned<BooleanArray>.startAddressOf: CPointer<BooleanVar> get() = if (this.get().isNotEmpty()) this.addressOf(0) else emptyPinnedBoolean.addressOf(0)
//val Pinned<CharArray>.startAddressOf: CPointer<UShortVar> get() = if (this.get().isNotEmpty()) this.addressOf(0) else emptyPinnedBoolean.addressOf(0)

// @TODO: Performance penalty due to requiring calling `this.get()` to get the size of the array. Optimal solution should be provided my Kotlin/Native.

/** Returns the address of the beginning of this array or if it is empty another valid non-null address */
public val Pinned<ByteArray>.startAddressOf: CPointer<ByteVar> get() = if (this.get().isNotEmpty()) this.addressOf(0) else emptyAddressByte
/** Returns the address of the beginning of this array or if it is empty another valid non-null address */
public val Pinned<ShortArray>.startAddressOf: CPointer<ShortVar> get() = if (this.get().isNotEmpty()) this.addressOf(0) else emptyAddressShort
/** Returns the address of the beginning of this array or if it is empty another valid non-null address */
public val Pinned<IntArray>.startAddressOf: CPointer<IntVar> get() = if (this.get().isNotEmpty()) this.addressOf(0) else emptyAddressInt
/** Returns the address of the beginning of this array or if it is empty another valid non-null address */
public val Pinned<FloatArray>.startAddressOf: CPointer<FloatVar> get() = if (this.get().isNotEmpty()) this.addressOf(0) else emptyAddressFloat
/** Returns the address of the beginning of this array or if it is empty another valid non-null address */
public val Pinned<DoubleArray>.startAddressOf: CPointer<DoubleVar> get() = if (this.get().isNotEmpty()) this.addressOf(0) else emptyAddressDouble
/** Returns the address of the beginning of this array or if it is empty another valid non-null address */
public val Pinned<LongArray>.startAddressOf: CPointer<LongVar> get() = if (this.get().isNotEmpty()) this.addressOf(0) else emptyAddressLong
/** Returns the address of the beginning of this array or if it is empty another valid non-null address */
public val Pinned<UByteArray>.startAddressOf: CPointer<UByteVar> get() = if (this.get().isNotEmpty()) this.addressOf(0) else emptyAddressUByte
/** Returns the address of the beginning of this array or if it is empty another valid non-null address */
public val Pinned<UShortArray>.startAddressOf: CPointer<UShortVar> get() = if (this.get().isNotEmpty()) this.addressOf(0) else emptyAddressUShort
/** Returns the address of the beginning of this array or if it is empty another valid non-null address */
public val Pinned<UIntArray>.startAddressOf: CPointer<UIntVar> get() = if (this.get().isNotEmpty()) this.addressOf(0) else emptyAddressUInt
/** Returns the address of the beginning of this array or if it is empty another valid non-null address */
public val Pinned<ULongArray>.startAddressOf: CPointer<ULongVar> get() = if (this.get().isNotEmpty()) this.addressOf(0) else emptyAddressULong
