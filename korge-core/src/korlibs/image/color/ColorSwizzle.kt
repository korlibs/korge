package korlibs.image.color

fun RGBA.swizzle(comps: String): RGBA = RGBA(
    getComponent(comps.getOrElse(0) { 'r' }),
    getComponent(comps.getOrElse(1) { 'g' }),
    getComponent(comps.getOrElse(2) { 'b' }),
    getComponent(comps.getOrElse(3) { 'a' })
)
