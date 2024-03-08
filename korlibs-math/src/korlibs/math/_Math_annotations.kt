@file:Suppress("PackageDirectoryMismatch")

package korlibs.math.annotations

@DslMarker
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class KorDslMarker

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@DslMarker
annotation class ViewDslMarker

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@DslMarker
annotation class RootViewDslMarker

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@DslMarker
annotation class VectorDslMarker

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class KormaExperimental(val reason: String = "")

//@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
/**
 * Mutable APIs follow the following convention:
 *
 * ```kotlin
 * interface IType { val ... }
 * class MType : IType(override var ...) : IType
 * ```
 *
 * Then in usage places:
 *
 * ```kotlin
 * fun doSomethingWith(a: IType, out: MType = MType()): MType
 * ```
 *
 * This convention supports allocation-free APIs by being able to preallocate instances and passing them as the output.
 */
annotation class KormaMutableApi
