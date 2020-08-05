package com.soywiz.korge.tiled

import com.soywiz.korge.animate.serialization.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korio.file.*

class TiledMapViewRef() : Container(), KorgeDebugNode, ViewLeaf, ViewFileRef by ViewFileRef.Mixin() {
    override suspend fun forceLoadSourceFile(views: Views, currentVfs: VfsFile, sourceFile: String?) {
        baseForceLoadSourceFile(views, currentVfs, sourceFile)
        removeChildren()
        addChild(currentVfs["$sourceFile"].readTiledMap().createView())
    }

    override fun renderInternal(ctx: RenderContext) {
        this.lazyLoadRenderInternal(ctx, this)
        super.renderInternal(ctx)
    }

    override fun getDebugProperties(views: Views): EditableNode? = EditableSection("TiledMap") {
        add(this@TiledMapViewRef::sourceFile.toEditableProperty(
            kind = EditableStringProperty.Kind.FILE { it.extensionLC == "tmx" },
            views = views
        ))
    }
}
