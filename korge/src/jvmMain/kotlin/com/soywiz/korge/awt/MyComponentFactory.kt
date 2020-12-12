package com.soywiz.korge.awt

import com.soywiz.korge.debug.*
import com.soywiz.korge.particle.*
import com.soywiz.korge.resources.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.tiled.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Image
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korinject.*
import com.soywiz.korio.file.*
import com.soywiz.korma.geom.vector.*
import java.awt.*
import java.util.*
import javax.swing.*

var myComponentFactory = MyComponentFactory()

open class MyComponentFactory {
    open fun <T> list(array: List<T>) = JList(Vector(array))

    open fun scrollPane(view: Component): JScrollPane =
            JScrollPane(view, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)

    open fun <T> comboBox(array: Array<T>): JComboBox<T> = JComboBox<T>()

    open fun tabbedPane(tabPlacement: Int, tabLayoutPolicy: Int): JTabbedPane = JTabbedPane(tabPlacement, tabLayoutPolicy)

    open fun chooseFile(views: Views?, filter: (VfsFile) -> Boolean): VfsFile? {
        TODO()
    }

    open fun chooseColor(value: RGBA, views: Views?): RGBA? {
        TODO()
    }

    open fun createModule(block: suspend Scene.() -> Unit): Module {
        TODO()
    }

    data class ViewFactory(val name: String, val build: () -> View)

    open fun getViewFactories(views: Views): List<ViewFactory> = ArrayList<ViewFactory>().also { list ->
        list.add(ViewFactory("Image") { Image(Bitmaps.white).apply { setSize(100.0, 100.0) } })
        list.add(ViewFactory("VectorImage") { VectorImage.createDefault().apply { setSize(100.0, 100.0) } })
        list.add(ViewFactory("SolidRect") { SolidRect(100, 100, Colors.WHITE) })
        list.add(ViewFactory("Ellipse") { Ellipse(50.0, 50.0, Colors.WHITE).center() })
        list.add(ViewFactory("Container") { Container() })
        list.add(ViewFactory("TreeViewRef") { TreeViewRef() })
        list.add(ViewFactory("ParticleEmitter") { ParticleEmitterView(ParticleEmitter()) })
        list.add(ViewFactory("AnimationViewRef") { AnimationViewRef() })
        list.add(ViewFactory("TiledMapViewRef") { TiledMapViewRef() })
        list.add(ViewFactory("9-Patch") { NinePatchEx(NinePatchBitmap32(Bitmap32(62, 62))) })
        for (registration in views.serializer.registrationsExt) {
            list.add(ViewFactory(registration.name) { registration.factory() })
        }
    }

    open fun createPopupMenu(): JPopupMenu = JPopupMenu()
    open fun createSeparator(): JSeparator = JSeparator()
    open fun createMenuItem(text: String, mnemonic: Int? = null, icon: Icon? = null): JMenuItem = when {
        mnemonic != null -> JMenuItem(text, mnemonic)
        icon != null -> JMenuItem(text, icon)
        else -> JMenuItem(text)
    }
}
