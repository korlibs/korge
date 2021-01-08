@file:Suppress("PackageDirectoryMismatch", "EXPERIMENTAL_API_USAGE", "unused", "UNUSED_PARAMETER",
    "RedundantSuspendModifier", "RedundantVisibilityModifier"
)
package com.soywiz.korge.particle

import com.soywiz.korio.file.VfsFile

@Deprecated(
    message = "Need to migrate to readParticleEmitter()",
    replaceWith = ReplaceWith("readParticleEmitter()"),
    level = DeprecationLevel.ERROR
)
public suspend inline fun VfsFile.readParticle(): ParticleEmitter {
    throw Error("migrate to readParticle")
}

