@file:Suppress("RedundantVisibilityModifier", "unused", "PackageDirectoryMismatch", "UNUSED_PARAMETER",
    "RemoveSetterParameterType"
)

package com.soywiz.korge.view.filter

/**
 * Migrate from version 1.15.1 to 2.0.0
 */
@property:Deprecated(
    message = "Need to migrate to blendRation",
    replaceWith = ReplaceWith("ratio"),
    level = DeprecationLevel.ERROR
)
var TransitionFilter.blendRatio: Double
    get() = throw Error("migrate to blendRatio")
    set(value: Double) = throw Error("migrate to blendRatio")
