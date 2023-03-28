package korlibs.image.vector

import korlibs.image.vector.Context2d
import korlibs.image.vector.Drawable

abstract class Chart() : Drawable {
	abstract fun Context2d.renderChart()
}
