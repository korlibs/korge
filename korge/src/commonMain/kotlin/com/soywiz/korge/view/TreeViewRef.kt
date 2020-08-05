package com.soywiz.korge.view

import com.soywiz.korge.debug.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.ktree.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*

class TreeViewRef() : Container(), KorgeDebugNode, ViewLeaf, ViewFileRef by ViewFileRef.Mixin() {
    override suspend fun forceLoadSourceFile(views: Views, currentVfs: VfsFile, sourceFile: String?) {
        baseForceLoadSourceFile(views, currentVfs, sourceFile)
        removeChildren()
        addChildren((currentVfs["$sourceFile"].readKTree(views) as Container).children.toList())
        scale = 1.0
    }

    override fun renderInternal(ctx: RenderContext) {
        this.lazyLoadRenderInternal(ctx, this)
        super.renderInternal(ctx)
    }

    override fun getDebugProperties(views: Views): EditableNode? = EditableSection("Tree") {
        add(this@TreeViewRef::sourceFile.toEditableProperty(
            kind = EditableStringProperty.Kind.FILE { it.extensionLC == "ktree" },
            views = views
        ))
    }
}
