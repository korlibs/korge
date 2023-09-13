package korlibs.math.annotations

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
