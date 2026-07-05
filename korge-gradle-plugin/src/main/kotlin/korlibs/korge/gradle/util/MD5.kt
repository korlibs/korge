package korlibs.korge.gradle.util

import java.security.MessageDigest

fun ByteArray.md5String(): String {
	return MessageDigest.getInstance("MD5").digest(this).hex
}
