package com.soywiz.korge.intellij.editor

import com.esotericsoftware.spine.korge.*
import com.intellij.openapi.project.*
import com.soywiz.korge.box2d.*
import com.soywiz.korge.dragonbones.*
import com.soywiz.korge.ext.swf.*
import com.soywiz.korge.intellij.components.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.*

fun Views.registerIdeaStuff(project: Project) {
    views.registerSwf()
    views.registerDragonBones()
    views.registerSpine()
    views.registerFilterSerialization()
    views.registerBox2dSupportOnce()
    views.ideaProject = project
    views.serializer.box2dWorld = views.stage.getOrCreateBox2dWorld()
}
