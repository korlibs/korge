package com.soywiz.korge.resources

import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.resourcesVfs

annotation class Resource()

fun resource(
    @Resource
    //@Language("resource") // @TODO: This doesn't work on Kotlin Common. reported already on Kotlin issue tracker
    //language=resource
    resource: String
): VfsFile = resourcesVfs[resource]
