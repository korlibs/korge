package com.soywiz.korio.hash

import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.internal.*
import com.soywiz.korio.stream.*
import com.soywiz.krypto.*

suspend fun VfsFile.hash(algo: HasherFactory) = openRead().use { hash(algo) }
suspend fun AsyncStream.hash(algo: HasherFactory): Hash = bytesTempPool.alloc { temp -> algo.hash(temp) { read(it) } }
fun SyncStream.hash(algo: HasherFactory): Hash = bytesTempPool.alloc { temp -> algo.hash(temp) { read(it) } }

suspend fun VfsFile.md5() = hash(MD5)
suspend fun AsyncStream.md5() = hash(MD5)
fun SyncStream.md5() = hash(MD5)

suspend fun VfsFile.sha1() = hash(SHA1)
suspend fun AsyncStream.sha1() = hash(SHA1)
fun SyncStream.sha1() = hash(SHA1)
