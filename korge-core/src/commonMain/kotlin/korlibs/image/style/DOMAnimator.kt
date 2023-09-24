package korlibs.image.style

import korlibs.image.annotation.*
import korlibs.image.vector.format.*
import korlibs.math.interpolation.*
import korlibs.time.*

@KorimExperimental
class DOMAnimator(val dom: DOM) {
    val css get() = dom.css
    var time = 0.seconds

    init {
        dom.listeners.add(object : DOM.DomListener {
            override fun updatedElement(element: DOM.DomElement) {
                updateElement(element as SVG.SvgElement)
            }
        })
    }

    fun update(dt: TimeSpan) {
        time += dt
        // @TODO: Do this properly
        for (rule in css.rulesForIds.values) {
            val idSelector = rule.selector as CSS.IdSelector
            val element = dom.elementsById[idSelector.id] ?: continue
            updateElement(element)
        }
    }

    fun updateElement(element: DOM.DomElement) {
        // @TODO: Other kind of rules that matches the element, and restore default values if required
        //println("element.id=${element.id}")
        val rules = (listOf(css.rulesForIds[element.id]) + element.classNames.map { css.rulesForClassNames[it] }).filterNotNull()
        for (rule in rules) {
            val decls = rule.declarations
            decls.animation?.let { ani ->
                val animation = css.animationsById[ani.name] ?: return@let
                //println("animation = $animation")
                val ratio = (time % ani.duration) / ani.duration
                val results = animation.getAt(ratio.toRatio())
                element.setPropertyInterpolated(results)
            }
            for (decl in decls.declarations) {
                //println("ANIMATE element: $element, decl=$decl")
                element.setProperty(decl.property, decl.expr)
            }
        }
    }
}
