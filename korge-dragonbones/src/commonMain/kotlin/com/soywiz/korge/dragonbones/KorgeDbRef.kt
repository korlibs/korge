package com.soywiz.korge.dragonbones

import com.soywiz.korge.animate.serialization.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korio.file.*
import com.soywiz.korui.*

class KorgeDbRef() : Container(), ViewLeaf, ViewFileRef by ViewFileRef.Mixin() {
    override suspend fun forceLoadSourceFile(views: Views, currentVfs: VfsFile, sourceFile: String?) {
        baseForceLoadSourceFile(views, currentVfs, sourceFile)
        removeChildren()
        val view = currentVfs["$sourceFile"].readDbSkeletonAndAtlas(views.dragonbonsFactory).buildFirstArmatureDisplay(views.dragonbonsFactory)
        if (view != null) {
            addChild(view)
        }
    }

    override fun renderInternal(ctx: RenderContext) {
        this.lazyLoadRenderInternal(ctx, this)
        super.renderInternal(ctx)
    }

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiCollapsibleSection("DragonBones") {
            uiEditableValue(::sourceFile, kind = UiTextEditableValue.Kind.FILE(views.currentVfs) {
                it.extensionLC == "dbbin" || it.baseName.endsWith("_ske.json")
            })
        }
        super.buildDebugComponent(views, container)
    }
}
