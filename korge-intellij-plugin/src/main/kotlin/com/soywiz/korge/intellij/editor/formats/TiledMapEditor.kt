package com.soywiz.korge.intellij.editor.formats

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.tiled.createView
import com.soywiz.korge.tiled.readTiledMap
import com.soywiz.korge.view.Container
import com.soywiz.korio.file.VfsFile

suspend fun Scene.tiledMapEditor(file: VfsFile) {
    val tiled = file.readTiledMap()
    sceneView += tiled.createView()
    views.setVirtualSize(tiled.pixelWidth, tiled.pixelHeight)
}
