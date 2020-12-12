package com.soywiz.korge.intellij.editor

import com.soywiz.korge.resources.*
import com.soywiz.korge.scene.*
import com.soywiz.korinject.*
import kotlin.reflect.*

fun createModule(root: Any? = null, block: suspend EditorScene.() -> Unit): Module {
    return EditorModule(root, {
        try {
            block()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    })
}

class EditorModule(val root: Any? = null, val block: suspend EditorScene.() -> Unit) : Module() {
    //override val editableNode: EditableNode? get() = editableNode
    override val mainScene: KClass<out Scene> get() = EditorScene::class

    override suspend fun AsyncInjector.configure() {
        get<ResourcesRoot>().mount("/", KorgeFileEditorProvider.pluginResurcesVfs.root)
        mapInstance(BlockToExecute(block))
    }
}
