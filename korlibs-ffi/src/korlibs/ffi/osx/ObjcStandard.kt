package korlibs.ffi.osx

import korlibs.ffi.*
import kotlin.text.toCharArray

fun NSObject.Companion.cast(value: Any): NSObject {
    return when (value) {
        is NSObject -> value
        //is String -> NSString(value)
        is Int -> NSNumber(value)
        is Long -> NSNumber(value)
        is Double -> NSNumber(value)
        //is Boolean -> CFBoolean(value)
        else -> TODO("Unsupported value '$value' to be cast to NSObject")
    }
}

open class NSString(id: Long) : NSObject(id) {
    constructor() : this("")
    constructor(id: Long?) : this(id ?: 0L)
    constructor(str: String) : this(OBJ_CLASS.msgSendRef("alloc").msgSend("initWithCharacters:length:", str.toCharArray(), str.length))

    //val length: Int get() = ObjectiveC.object_getIvar(this.id, LENGTH_ivar).toInt()
    val length: Int get() = this.msgSend("length").toInt()

    val cString: String
        get() {
            val length = this.length
            val ba = ByteArray(length + 1)
            msgSend("getCString:maxLength:encoding:", ba, length + 1, 4)
            val str = ba.decodeToString()
            return str.substring(0, str.length - 1)
        }

    override fun toString(): String = cString

    companion object : NSClass("NSString") {
        val LENGTH_ivar = FFIObjc.class_getProperty(OBJ_CLASS.id, "length")
    }
}

open class NSDictionary(id: Long) : NSObject(id) {
    constructor() : this(NSClass("NSDictionary").alloc().msgSend("init"))
    val count: Long by objProp("count", "Q16@0:8")
    val objectForKey: (objectForKey: ID) -> ID by objFunc("objectForKey:", "@24@0:8@16")
}

class NSMutableDictionary(id: Long) : NSDictionary(id) {
    constructor() : this(NSClass("NSMutableDictionary").alloc().msgSend("init"))
    fun setValue(value: NSObject, forKey: NSObject) = msgSendVoid("setValue:forKey:", value.id, forKey.id)
    fun getValue(key: NSObject): NSObject = NSObject(msgSend("valueForKey:", key.id))
    operator fun set(key: NSObject, value: NSObject) = setValue(value, key)
    operator fun set(key: Any, value: Any) = run { this[NSObject.cast(key)] = NSObject.cast(value) }
    operator fun get(key: Any): NSObject = getValue(NSObject.cast(key))
}

class NSNumber private constructor(id: Long) : NSObject(id) {
    constructor(value: Int) : this(NSClass("NSNumber").alloc().msgSend("initWithInt:", value))
    constructor(value: Double) : this(NSClass("NSNumber").alloc().msgSend("initWithDouble:", value))
    constructor(value: Long, unit: Unit = Unit) : this(NSClass("NSNumber").alloc().msgSend("initWithLong:", value))
    val boolValue: Boolean get() = msgSendInt("boolValue") != 0
    val intValue: Int get() = msgSendInt("intValue")
    val longValue: Long get() = msgSend("longValue")
    //val doubleValue: Double get() = msgSendDouble("doubleValue")
}

class NSArray(val id: ObjcRef) : AbstractList<Long>() {
    val count: Int get() = id.msgSendInt("count")
    override val size: Int get() = count
    override operator fun get(index: Int): Long = id.msgSend("objectAtIndex:", index)
    override fun toString(): String = "NSArray(${toList()})"
}

object CoreFoundation : FFILib("/System/Library/Frameworks/CoreFoundation.framework/Versions/A/CoreFoundation") {
    val dlopen: (path: String, mode: Int) -> FFIPointer? by func()
}
