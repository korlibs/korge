package korlibs.inject

import java.lang.reflect.*
import kotlin.reflect.*

@Target(AnnotationTarget.CLASS)
annotation class Prototype

@Target(AnnotationTarget.CLASS)
annotation class Singleton

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class Optional

fun AsyncInjector.jvmAutomapping(): AsyncInjector = this.apply {
	this.fallbackProvider = { kclazz, ctx -> AsyncInjector.jvmFallback(this, kclazz, ctx) }
}

suspend fun AsyncInjector.Companion.jvmFallback(
    injector: AsyncInjector,
    kclazz: KClass<*>,
    ctx: AsyncInjector.RequestContext
): AsyncObjectProvider<*> {
    return fallback(kclazz, ctx)
}

fun AsyncInjector.jvmRemoveMappingsByClassName(classNames: Set<String>) {
    val classes = providersByClass.keys.filter { it.qualifiedName in classNames }
    for (clazz in classes) providersByClass.remove(clazz)
    parent?.jvmRemoveMappingsByClassName(classNames)
}

private suspend fun fallback(
    kclazz: KClass<*>,
    ctx: AsyncInjector.RequestContext
): AsyncObjectProvider<*> {
	val clazz = kclazz.java

	val isPrototype = clazz.getAnnotation(Prototype::class.java) != null
	val isSingleton = clazz.getAnnotation(Singleton::class.java) != null
	val isAsyncFactoryClass = clazz.getAnnotation(AsyncFactoryClass::class.java) != null


	val generator: suspend AsyncInjector.() -> Any? = {
		try {
			// @TODO: Performance: Cache all this!
			// Use: ClassFactory and stuff

			val loaderClass = clazz.getAnnotation(AsyncFactoryClass::class.java)
			val actualClass = loaderClass?.clazz?.java ?: clazz
			if (actualClass.isInterface || Modifier.isAbstract(actualClass.modifiers)) throw IllegalArgumentException("Can't instantiate abstract or interface: $actualClass in $ctx")
			val constructor = actualClass.declaredConstructors.firstOrNull()
					?: throw IllegalArgumentException("No available constructor for $clazz")
			val out = arrayListOf<Any?>()
			val allInstances = arrayListOf<Any?>()

			for ((paramType, annotations) in constructor.parameterTypes.zip(constructor.parameterAnnotations)) {
				var isOptional = false

				val i = if (annotations.isNotEmpty()) {
					val i = this.child()
					for (annotation in annotations) {
						when (annotation) {
							is Optional -> isOptional = true
							else -> i.mapInstance(annotation.annotationClass as KClass<Any>, annotation as Any)
						}
					}
					i
				} else {
					this
				}
				if (isOptional) {
					out.add(if (i.has(paramType.kotlin)) i.getOrNull(paramType.kotlin, ctx) else null)
				} else {
					out.add(
						i.getOrNull(paramType.kotlin, ctx) ?: throw AsyncInjector.NotMappedException(
							paramType.kotlin,
							actualClass.kotlin,
							ctx
						)
					)
				}
			}
			allInstances.addAll(out)
			constructor.isAccessible = true
			val instance = constructor.newInstance(*out.toTypedArray())

			if (instance is AsyncDependency) instance.init()

			for (createdInstance in allInstances) {
				if (createdInstance is InjectedHandler) {
					createdInstance.injectedInto(instance)
				}
			}

			if (loaderClass != null) {
				(instance as AsyncFactory<Any?>).create()
			} else {
				instance
			}
		} catch (e: Throwable) {
			println("$this error while creating '$clazz': (${e.message}):")
			e.printStackTrace()
			throw e
		}
	}

	return when {
		isPrototype -> PrototypeAsyncObjectProvider(generator)
		isSingleton -> SingletonAsyncObjectProvider(generator)
		isAsyncFactoryClass -> FactoryAsyncObjectProvider(generator as suspend AsyncInjector.() -> AsyncFactory<Any?>)
		else -> PrototypeAsyncObjectProvider(generator)
	}
}
