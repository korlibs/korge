package com.soywiz.korio


/*
class LocalVfsProviderJs : LocalVfsProvider() {
	override fun invoke(): LocalVfs = object : LocalVfs() {
		suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
			val stat = fstat(path)
			val handle = open(path, "r")

			return object : AsyncStreamBase() {
				suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
					val data = read(handle, position.toDouble(), len.toDouble())
					arraycopy(data, 0, buffer, offset, data.size)
					return data.size
				}

				suspend override fun getLength(): Long = stat.size.toLong()
				suspend override fun close(): Unit = close(handle)
			}.toAsyncStream()
		}

		suspend override fun stat(path: String): VfsStat = try {
			val stat = fstat(path)
			createExistsStat(path, isDirectory = stat.isDirectory, size = stat.size.toLong())
		} catch (t: Throwable) {
			createNonExistsStat(path)
		}

		suspend override fun list(path: String): AsyncSequence<VfsFile> {
			val emitter = AsyncSequenceEmitter<VfsFile>()
			val fs = jsRequire("fs")
			//console.methods["log"](path)
			fs.call("readdir", path, jsFunctionRaw2 { err, files ->
				//console.methods["log"](err)
				//console.methods["log"](files)
				for (n in 0 until files["length"].toInt()) {
					val file = files[n].toJavaString()
					//println("::$file")
					emitter(file("$path/$file"))
				}
				emitter.close()
			})
			return emitter.toSequence()
		}

		suspend override fun watch(path: String, handler: (VfsFileEvent) -> Unit): Closeable = withCoroutineContext {
			val fs = jsRequire("fs")
			val watcher = fs.call("watch", path, jsObject("persistent" to true, "recursive" to true), jsFunctionRaw2 { eventType, filename ->
				spawnAndForget(this@withCoroutineContext) {
					val et = eventType.toJavaString()
					val fn = filename.toJavaString()
					val f = file("$path/$fn")
					//println("$et, $fn")
					when (et) {
						"rename" -> {
							val kind = if (f.exists()) VfsFileEvent.Kind.CREATED else VfsFileEvent.Kind.DELETED
							handler(VfsFileEvent(kind, f))
						}
						"change" -> {
							handler(VfsFileEvent(VfsFileEvent.Kind.MODIFIED, f))
						}
						else -> {
							println("Unhandled event: $et")
						}
					}
				}
			})

			return@withCoroutineContext Closeable { watcher.call("close") }
		}

		override fun toString(): String = "LocalVfs"
	}


	suspend fun open(path: String, mode: String): JsDynamic = korioSuspendCoroutine { c ->
		val fs = jsRequire("fs")
		fs.call("open", path, mode, jsFunctionRaw2 { err, fd ->
			if (err != null) {
				c.resumeWithException(RuntimeException("Error ${err.toJavaString()} opening $path"))
			} else {
				c.resume(fd!!)
			}
		})
	}

	suspend fun read(fd: JsDynamic?, position: Double, len: Double): ByteArray = Promise.create { c ->
		val fs = jsRequire("fs")
		val buffer = jsNew("Buffer", len)
		fs.call("read", fd, buffer, 0, len, position, jsFunctionRaw3 { err, bytesRead, buffer ->
			if (err != null) {
				c.reject(RuntimeException("Error ${err.toJavaString()} opening ${fd.toJavaString()}"))
			} else {
				val u8array = jsNew("Int8Array", buffer, 0, bytesRead)
				val out = ByteArray(bytesRead.toInt())
				out.asJsDynamic().call("setArraySlice", 0, u8array)
				c.resolve(out)
			}
		})
	}

	suspend fun close(fd: Any): Unit = Promise.create { c ->
		val fs = jsRequire("fs")
		fs.call("close", fd, jsFunctionRaw2 { err, fd ->
			if (err != null) {
				c.reject(RuntimeException("Error ${err.toJavaString()} closing file"))
			} else {
				c.resolve(Unit)
			}
		})
	}



	suspend fun fstat(path: String): JsStat = Promise.create { c ->
		// https://nodejs.org/api/fs.html#fs_class_fs_stats
		val fs = jsRequire("fs")
		//fs.methods["exists"](path, jsFunctionRaw1 { jsexists ->
		//	val exists = jsexists.toBool()
		//	if (exists) {
		fs.call("stat", path, jsFunctionRaw2 { err, stat ->
			//console.methods["log"](stat)
			if (err != null) {
				c.reject(RuntimeException("Error ${err.toJavaString()} opening $path"))
			} else {
				val out = JsStat(stat["size"].toDouble())
				out.isDirectory = stat.call("isDirectory").toBool()
				c.resolve(out)
			}
		})
		//	} else {
		//		c.resumeWithException(RuntimeException("File '$path' doesn't exists"))
		//	}
		//})
	}
}

class ResourcesVfsProviderJs : ResourcesVfsProvider() {
	override fun invoke(): Vfs {
		return EmbededResourceListing(if (OS.isNodejs) {
			LocalVfs(getCWD())
		} else {
			UrlVfs(getBaseUrl())
		}.jail())
	}

	private fun getCWD(): String = global["process"].call("cwd").toJavaString()

	private fun getBaseUrl(): String {
		var baseHref = document["location"]["href"].call("replace", jsRegExp("/[^\\/]*$"), "")
		val bases = document.call("getElementsByTagName", "base")
		if (bases["length"].toInt() > 0) baseHref = bases[0]["href"]
		return baseHref.toJavaString()
	}
}

@Suppress("unused")
private class EmbededResourceListing(parent: VfsFile) : Vfs.Decorator(parent) {
	val nodeVfs = NodeVfs()

	init {
		for (asset in jsGetAssetStats()) {
			val info = PathInfo(asset.path.trim('/'))
			val folder = nodeVfs.rootNode.access(info.folder, createFolders = true)
			folder.createChild(info.basename, isDirectory = false).data = asset.size
		}
	}

	suspend override fun stat(path: String): VfsStat {
		try {
			val n = nodeVfs.rootNode[path]
			return createExistsStat(path, n.isDirectory, n.data as Long)
		} catch (t: Throwable) {
			return createNonExistsStat(path)
		}
	}

	suspend override fun list(path: String): AsyncSequence<VfsFile> = withCoroutineContext {
		asyncGenerate(this@withCoroutineContext) {
			for (item in nodeVfs.rootNode[path]) {
				yield(file("$path/${item.name}"))
			}
		}
	}

	override fun toString(): String = "ResourcesVfs[$parent]"
}
*/