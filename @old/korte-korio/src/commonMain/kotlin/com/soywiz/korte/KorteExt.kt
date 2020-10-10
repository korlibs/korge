package com.soywiz.korte

import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*

fun TemplateProvider(vfs: VfsFile): TemplateProvider = object : TemplateProvider {
	override suspend fun get(template: String): String? = runIgnoringExceptions { vfs[template].readString() }
}

fun VfsFile.toTemplateProvider() = TemplateProvider(this)

fun Templates(
	root: VfsFile,
	includes: VfsFile = root,
	layouts: VfsFile = root,
	config: TemplateConfig = TemplateConfig(),
	cache: Boolean = true
) = Templates(root.toTemplateProvider(), includes.toTemplateProvider(), layouts.toTemplateProvider(), config, cache)

fun TemplateConfigWithTemplates.root(root: VfsFile, includes: VfsFile = root, layouts: VfsFile = root) =
	root(root.toTemplateProvider(), includes.toTemplateProvider(), layouts.toTemplateProvider())

