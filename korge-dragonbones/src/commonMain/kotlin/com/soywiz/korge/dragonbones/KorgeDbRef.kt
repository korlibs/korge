package com.soywiz.korge.dragonbones

import com.soywiz.korge.animate.serialization.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korio.file.*

class KorgeDbRef() : Container(), KorgeDebugNode, ViewLeaf, ViewFileRef by ViewFileRef.Mixin() {
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

    override fun getDebugProperties(views: Views): EditableNode? = EditableSection("DragonBones") {
        add(this@KorgeDbRef::sourceFile.toEditableProperty(
            kind = EditableStringProperty.Kind.FILE { it.extensionLC == "dbbin" || it.baseName.endsWith("_ske.json") },
            views = views
        ))
    }
}
