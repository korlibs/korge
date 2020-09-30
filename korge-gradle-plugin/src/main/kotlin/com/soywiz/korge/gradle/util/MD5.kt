package com.soywiz.korge.gradle.util

import java.security.*

fun ByteArray.md5String(): String {
	return MessageDigest.getInstance("MD5").digest(this).hex
}
