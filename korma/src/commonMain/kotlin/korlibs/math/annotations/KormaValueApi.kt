package korlibs.math.annotations

/**
 * Immutable APIs without interfaces.
 *
 * ```kotlin
 * data class Type(val ...)
 * ```
 *
 * eventually they will be transformed into value classes:
 * ```kotlin
 * value class Type(val ...)
 * ```
 *
 * For now, they force allocations, but once MFVC is ready,
 * this API won't allocate and will replace mutable versions.
 */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class KormaValueApi