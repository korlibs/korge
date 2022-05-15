package com.soywiz.korge.view

import com.soywiz.korge.animate.serialization.readAnimation
import com.soywiz.korge.debug.UiTextEditableValue
import com.soywiz.korge.debug.uiCollapsibleSection
import com.soywiz.korge.debug.uiEditableValue
import com.soywiz.korge.render.RenderContext
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.extensionLC
import com.soywiz.korui.UiContainer

class AnimationViewRef() : Container(), ViewLeaf, ViewFileRef by ViewFileRef.Mixin() {
    override suspend fun forceLoadSourceFile(views: Views, currentVfs: VfsFile, sourceFile: String?) {
        baseForceLoadSourceFile(views, currentVfs, sourceFile)
        removeChildren()
        addChild(currentVfs["$sourceFile"].readAnimation(views).createMainTimeLine())
    }

    override fun renderInternal(ctx: RenderContext) {
        this.lazyLoadRenderInternal(ctx, this)
        super.renderInternal(ctx)
    }

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiCollapsibleSection("SWF") {
            uiEditableValue(::sourceFile, kind = UiTextEditableValue.Kind.FILE(views.currentVfs) {
                it.extensionLC == "swf" || it.extensionLC == "ani"
            })
        }
        super.buildDebugComponent(views, container)
    }
}
