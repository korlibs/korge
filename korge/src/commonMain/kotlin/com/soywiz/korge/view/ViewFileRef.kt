package com.soywiz.korge.view

import com.soywiz.korge.render.RenderContext
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.VfsFile

interface ViewFileRef {
    var sourceTreeLoaded: Boolean
    var sourceFile: String?
    suspend fun ViewFileRef.baseForceLoadSourceFile(views: Views, currentVfs: VfsFile, sourceFile: String?)
    suspend fun forceLoadSourceFile(views: Views, currentVfs: VfsFile = views.currentVfs, sourceFile: String? = null)
    fun lazyLoadRenderInternal(ctx: RenderContext, view: ViewFileRef)

    class Mixin() : ViewFileRef {
        override var sourceTreeLoaded: Boolean = true
        override var sourceFile: String? = null
            set(value) {
                //println("SET sourceFile=$value")
                sourceTreeLoaded = false
                field = value
            }

        override suspend fun ViewFileRef.baseForceLoadSourceFile(views: Views, currentVfs: VfsFile, sourceFile: String?) {
            //println("### Trying to load sourceImage=$sourceImage")
            this.sourceFile = sourceFile
            sourceTreeLoaded = true
        }

        override suspend fun forceLoadSourceFile(views: Views, currentVfs: VfsFile, sourceFile: String?) {
            TODO("Must override this")
        }

        override fun lazyLoadRenderInternal(ctx: RenderContext, view: ViewFileRef) {
            if (sourceTreeLoaded) return
            lazyLoadRenderInternalActually(ctx, view)
        }

        fun lazyLoadRenderInternalActually(ctx: RenderContext, view: ViewFileRef) {
            if (!sourceTreeLoaded && sourceFile != null) {
                sourceTreeLoaded = true
                launchImmediately(ctx.coroutineContext) {
                    view.forceLoadSourceFile(ctx.views!!, sourceFile = sourceFile)
                }
            }
        }
    }
}
