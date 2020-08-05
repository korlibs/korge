package com.soywiz.korge.view

import com.soywiz.korge.debug.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.ktree.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*

class TreeViewRef : Container(), KorgeDebugNode, ViewLeaf {
    private var sourceTreeLoaded: Boolean = false
    var sourceFile: String? = null
        set(value) {
            sourceTreeLoaded = false
            field = value
        }

    suspend fun forceLoadSourceFile(views: Views, currentVfs: VfsFile = views.currentVfs, sourceFile: String? = null) {
        //println("### Trying to load sourceImage=$sourceImage")
        this.sourceFile = sourceFile
        sourceTreeLoaded = true
        removeChildren()
        addChildren((currentVfs["$sourceFile"].readKTree(views) as Container).children.toList())
        scale = 1.0
    }

    override fun renderInternal(ctx: RenderContext) {
        if (!sourceTreeLoaded && sourceFile != null) {
            sourceTreeLoaded = true
            launchImmediately(ctx.coroutineContext) {
                forceLoadSourceFile(ctx.views!!, sourceFile = sourceFile)
            }
        }
        super.renderInternal(ctx)
    }

    override fun getDebugProperties(views: Views): EditableNode? = EditableSection("Tree") {
        add(this@TreeViewRef::sourceFile.toEditableProperty(
            kind = EditableStringProperty.Kind.FILE { it.extensionLC == "ktree" },
            views = views
        ))
    }
}
