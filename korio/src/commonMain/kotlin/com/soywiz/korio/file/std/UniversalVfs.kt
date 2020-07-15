package com.soywiz.korio.file.std

import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*

object UniversalVfs {
	operator fun invoke(uri: String, providers: UniSchemaProviders, base: VfsFile? = null): VfsFile {
		return when {
			URL.isAbsolute(uri) -> {
				val uriUri = URL(uri)
				val builder = providers.providers[uriUri.scheme]
				if (builder != null) {
					builder.provider(uriUri)
				} else {
					invalidOp("Unsupported scheme '${uriUri.scheme}'")
				}
			}
			(base != null) -> base[uri]
			else -> localCurrentDirVfs[uri]
		}
	}
}

class UniSchema(val name: String, val provider: (URL) -> VfsFile)

class UniSchemaProviders(val providers: Map<String, UniSchema>) {
	constructor(providers: Iterable<UniSchema>) : this(providers.associateBy { it.name })
	constructor(vararg providers: UniSchema) : this(providers.associateBy { it.name })
}

var defaultUniSchema = UniSchemaProviders(
	UniSchema("http") { UrlVfs(it) },
	UniSchema("https") { UrlVfs(it) },
	UniSchema("file") { rootLocalVfs[it.path] }
)

fun registerUniSchema(schema: UniSchema) {
	defaultUniSchema += schema
}

inline fun <T> registerUniSchemaTemporarily(schema: UniSchema, callback: () -> T): T {
	val old = defaultUniSchema
	defaultUniSchema += schema
	try {
		return callback()
	} finally {
		defaultUniSchema -= schema
	}
}

//fun registerUniSchema(name: String, provider: (URL) -> VfsFile) = registerUniSchema(UniSchema(name, provider))
//inline fun <T> registerUniSchemaTemporarily(name: String, noinline provider: (URL) -> VfsFile, callback: () -> T) = registerUniSchemaTemporarily(UniSchema(name, provider), callback)

operator fun UniSchemaProviders.plus(other: UniSchemaProviders) = UniSchemaProviders(this.providers + other.providers)
operator fun UniSchemaProviders.plus(other: UniSchema) = UniSchemaProviders(this.providers + mapOf(other.name to other))

operator fun UniSchemaProviders.minus(other: UniSchemaProviders): UniSchemaProviders = UniSchemaProviders(this.providers - other.providers.keys)
operator fun UniSchemaProviders.minus(other: UniSchema) = UniSchemaProviders(this.providers - other.name)

// @TODO: Make general
val String.uniVfs get() = UniversalVfs(this, defaultUniSchema)

fun String.uniVfs(providers: UniSchemaProviders, base: VfsFile? = null): VfsFile =
	UniversalVfs(this, providers, base)
