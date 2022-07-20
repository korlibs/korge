package com.soywiz.korge.tiled

import com.soywiz.korge.debug.UiTextEditableValue
import com.soywiz.korge.debug.uiCollapsibleSection
import com.soywiz.korge.debug.uiEditableValue
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.ViewFileRef
import com.soywiz.korge.view.ViewLeaf
import com.soywiz.korge.view.Views
import com.soywiz.korim.tiles.tiled.readTiledMap
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.extensionLC
import com.soywiz.korui.UiContainer

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

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiCollapsibleSection("TiledMap") {
            uiEditableValue(::sourceFile, kind = UiTextEditableValue.Kind.FILE(views.currentVfs) {
                it.extensionLC == "tmx"
            })
        }
        super.buildDebugComponent(views, container)
    }
}
