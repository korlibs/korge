package korlibs.io.hash

import korlibs.io.async.use
import korlibs.io.internal.bytesTempPool
import korlibs.io.stream.AsyncInputOpenable
import korlibs.io.stream.AsyncInputStream
import korlibs.io.stream.SyncInputStream
import korlibs.io.stream.read
import korlibs.crypto.Hash
import korlibs.crypto.HasherFactory
import korlibs.crypto.MD5
import korlibs.crypto.SHA1

suspend fun AsyncInputOpenable.hash(algo: HasherFactory) = openRead().use { hash(algo) }
suspend fun AsyncInputStream.hash(algo: HasherFactory): Hash = bytesTempPool.alloc { temp -> algo.digest(temp) { read(it) } }
fun SyncInputStream.hash(algo: HasherFactory): Hash = bytesTempPool.alloc { temp -> algo.digest(temp) { read(it) } }

suspend fun AsyncInputOpenable.md5() = hash(MD5)
suspend fun AsyncInputStream.md5() = hash(MD5)
fun SyncInputStream.md5() = hash(MD5)

suspend fun AsyncInputOpenable.sha1() = hash(SHA1)
suspend fun AsyncInputStream.sha1() = hash(SHA1)
fun SyncInputStream.sha1() = hash(SHA1)
