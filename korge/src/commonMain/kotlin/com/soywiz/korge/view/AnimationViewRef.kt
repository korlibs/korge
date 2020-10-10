package com.soywiz.korge.view

import com.soywiz.korge.animate.serialization.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.render.*
import com.soywiz.korio.file.*
import com.soywiz.korui.*

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
        container.uiCollapsableSection("SWF") {
            uiEditableValue(::sourceFile, kind = UiTextEditableValue.Kind.FILE(views.currentVfs) {
                it.extensionLC == "swf" || it.extensionLC == "ani"
            })
        }
        super.buildDebugComponent(views, container)
    }
}
