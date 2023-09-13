package korlibs.io.dynamic

import java.lang.reflect.*

internal actual object DynamicInternal : DynApi {
    class JavaPackage(val name: String)

    override val global: Any? = JavaPackage("")

    private fun tryGetField(clazz: Class<*>, name: String): Field? {
        val field = runCatching { clazz.getDeclaredField(name) }.getOrNull()
        return when {
            field != null -> field.apply { isAccessible = true }
            clazz.superclass != null -> return tryGetField(clazz.superclass, name)
            else -> null
        }
    }

    private fun tryGetMethod(clazz: Class<*>, name: String, args: Array<out Any?>?): Method? {
        val methods = (clazz.interfaces + clazz).flatMap { it.allDeclaredMethods.filter { it.name == name } }
        val method = when (methods.size) {
            0 -> null
            1 -> methods.first()
            else -> {
                if (args != null) {
                    val methodsSameArity = methods.filter { it.parameterTypes.size == args.size }
                    val argTypes = args.map { if (it == null) null else it::class.javaObjectType }
                    methodsSameArity.firstOrNull {
                        it.parameterTypes.toList().zip(argTypes).all {
                            (it.second == null) || it.first.kotlin.javaObjectType.isAssignableFrom(it.second)
                        }
                    }
                } else {
                    methods.first()
                }
            }
        }
        return when {
            method != null -> method.apply { isAccessible = true }
            clazz.superclass != null -> return tryGetMethod(clazz.superclass, name, args)
            else -> null
        }
    }

    override fun get(instance: Any?, key: String): Any? = getBase(instance, key, doThrow = false)

    override fun set(instance: Any?, key: String, value: Any?) {
        if (instance == null) return

        val static = instance is Class<*>
        val clazz: Class<*> = if (static) instance as Class<*> else instance.javaClass

        val method = tryGetMethod(clazz, "set${key.capitalize()}", null)
        if (method != null) {
            method.invoke(if (static) null else instance, value)
            return
        }
        val field = tryGetField(clazz, key)
        if (field != null) {
            field.set(if (static) null else instance, value)
            return
        }
    }

    override fun getOrThrow(instance: Any?, key: String): Any? {
        return getBase(instance, key, doThrow = true)
    }

    override fun invoke(instance: Any?, key: String, args: Array<out Any?>): Any? {
        return invokeBase(instance, key, args, doThrow = false)
    }

    override fun invokeOrThrow(instance: Any?, key: String, args: Array<out Any?>): Any? {
        return invokeBase(instance, key, args, doThrow = true)
    }

    fun getBase(instance: Any?, key: String, doThrow: Boolean): Any? {
        if (instance == null) {
            if (doThrow) error("Can't get '$key' on null")
            return null
        }

        val static = instance is Class<*>
        val clazz: Class<*> = if (static) instance as Class<*> else instance.javaClass

        if (instance is JavaPackage) {
            val path = "${instance.name}.$key".trim('.')
            return try {
                java.lang.Class.forName(path)
            } catch (e: ClassNotFoundException) {
                JavaPackage(path)
            }
        }
        val method = tryGetMethod(clazz, "get${key.capitalize()}", null)
        if (method != null) {
            return method.invoke(if (static) null else instance)
        }
        val field = tryGetField(clazz, key)
        if (field != null) {
            return field.get(if (static) null else instance)
        }
        if (doThrow) {
            error("Can't find suitable fields or getters for '$key'")
        }
        return null
    }

    fun invokeBase(instance: Any?, key: String, args: Array<out Any?>, doThrow: Boolean): Any? {
        if (instance == null) {
            if (doThrow) error("Can't invoke '$key' on null")
            return null
        }
        val method = tryGetMethod(if (instance is Class<*>) instance else instance.javaClass, key, args)
        if (method == null) {
            if (doThrow) error("Can't find method '$key' on ${instance::class}")
            return null
        }
        return try {
            method.invoke(if (instance is Class<*>) null else instance, *args)
        } catch (e: InvocationTargetException) {
            throw e.targetException ?: e
        }
    }
}

private val Class<*>.allDeclaredFields: List<Field>
    get() = this.declaredFields.toList() + (this.superclass?.allDeclaredFields?.toList() ?: listOf<Field>())

private fun Class<*>.isSubtypeOf(that: Class<*>) = that.isAssignableFrom(this)

private val Class<*>.allDeclaredMethods: List<Method>
    get() = this.declaredMethods.toList() + (this.superclass?.allDeclaredMethods?.toList() ?: listOf<Method>())
