package korlibs.memory.dyn

import com.sun.jna.*
import java.lang.reflect.*
import kotlin.reflect.*

actual inline fun <reified T : Function<*>> DynamicLibrary.func(name: String?): DynamicFunLibraryNotNull<T> = DynamicFun<T>(this, name, T::class, typeOf<T>())
//public fun <T : Function<*>> DynamicLibrary.funcNull(name: String? = null): DynamicFunLibraryNull<T> = DynamicFunLibraryNull<T>(this, name)

//actual inline fun <reified T : Function<*>> DynamicLibrary.sfunc(name: String? = null): DynamicFunLibrary<T>
//actual inline fun <reified T : Function<*>> DynamicLibrary.sfuncNull(name: String? = null): DynamicFunLibraryNull<T>


@OptIn(ExperimentalStdlibApi::class)
public open class DynamicFun<T : Function<*>>(
    library: DynamicSymbolResolver,
    name: String? = null,
    val clazz: KClass<T>,
    val funcType: KType
) : DynamicFunLibraryNotNull<T>(library, name) {
    fun Type.getFinalClass(): Class<*> {
        return when (this) {
            is Class<*> -> this
            is ParameterizedType  -> this.rawType.getFinalClass()
            else -> TODO("$this")
        }
    }

    override fun getValue(obj: Any?, property: KProperty<*>): KPointerTT<KFunctionTT<T>> {
        val rname = name ?: property.name
        val symbol: KPointer = library.getSymbol(rname) ?: error("Can't find symbol '$rname'")
        val func = com.sun.jna.Function.getFunction(symbol.ptr)
        val jtype = funcType.arguments.last().type!!.javaType
        val retType: Class<*> = jtype.getFinalClass()
        val classLoader = this::class.java.classLoader
        val interfaces = arrayOf(clazz.java)

        return KPointerTT<KFunctionTT<T>>(Pointer.createConstant(0), KFunctionTT(when {
            retType.isAssignableFrom(Void::class.java) -> Proxy.newProxyInstance(classLoader, interfaces) { _, _, args -> func.invokeVoid(convertArgs(args)) }
            retType.isAssignableFrom(Unit::class.java) -> Proxy.newProxyInstance(classLoader, interfaces) { _, _, args -> func.invokeVoid(convertArgs(args)) }
            retType.isAssignableFrom(Double::class.java) -> Proxy.newProxyInstance(classLoader, interfaces) { _, _, args -> func.invokeDouble(convertArgs(args)) }
            retType.isAssignableFrom(Float::class.java) -> Proxy.newProxyInstance(classLoader, interfaces) { _, _, args -> func.invokeFloat(convertArgs(args)) }
            retType.isAssignableFrom(Int::class.java) -> Proxy.newProxyInstance(classLoader, interfaces) { _, _, args -> func.invokeInt(convertArgs(args)) }
            retType.isAssignableFrom(Pointer::class.java) -> Proxy.newProxyInstance(classLoader, interfaces) { _, _, args -> func.invokePointer(convertArgs(args)) }
            retType.isAssignableFrom(KPointerTT::class.java) -> Proxy.newProxyInstance(classLoader, interfaces) { _, _, args -> KPointerTT<KPointed>(func.invokePointer(convertArgs(args))) }
            retType.isAssignableFrom(KPointer::class.java) -> Proxy.newProxyInstance(classLoader, interfaces) { _, _, args -> KPointer(func.invokePointer(convertArgs(args)).address) }
            else -> Proxy.newProxyInstance(classLoader, interfaces) { _, _, args -> func.invokeDouble(args) }
        } as T))
    }

    fun convertArgs(args: Array<Any?>): Array<Any?> {
        val out = args.copyOf()
        for (n in 0 until out.size) {
            val it = out[n]
            out[n] = when (it) {
                is KPointerTT<*> -> it.ptr
                is KPointer -> it.ptr
                else -> it
            }
        }
        return out
    }
}
