package com.soywiz.korge.gradle.util

import org.jetbrains.kotlin.daemon.common.*
import java.security.*

fun ByteArray.md5String(): String {
	return MessageDigest.getInstance("MD5").digest(this).toHexString()
}
