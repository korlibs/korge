package com.soywiz.korge.tiled

import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.property.*
import com.soywiz.korim.tiles.tiled.*
import com.soywiz.korio.file.*

class TiledMapViewRef() : Container(), ViewLeaf, ViewFileRef by ViewFileRef.Mixin() {
    override suspend fun forceLoadSourceFile(views: Views, currentVfs: VfsFile, sourceFile: String?) {
        baseForceLoadSourceFile(views, currentVfs, sourceFile)
        removeChildren()
        addChild(currentVfs["$sourceFile"].readTiledMap().createView())
    }

    override fun renderInternal(ctx: RenderContext) {
        this.lazyLoadRenderInternal(ctx, this)
        super.renderInternal(ctx)
    }

    @Suppress("unused")
    @ViewProperty
    @ViewPropertyFileRef(["tmx"])
    private var tilemapSourceFile: String? by this::sourceFile
}
