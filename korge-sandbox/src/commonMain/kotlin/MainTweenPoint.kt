import com.soywiz.klock.seconds
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.anchor
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.circle
import com.soywiz.korge.view.container
import com.soywiz.korge.view.graphics
import com.soywiz.korge.view.scale
import com.soywiz.korge.view.xy
import com.soywiz.korim.color.Colors
import com.soywiz.korim.vector.StrokeInfo
import com.soywiz.korim.vector.toStrokeShape
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.bezier.Bezier
import com.soywiz.korma.geom.bezier.Curve
import com.soywiz.korma.geom.bezier.Curves
import com.soywiz.korma.geom.bezier.getPoints
import com.soywiz.korma.geom.bezier.toCurves
import com.soywiz.korma.geom.shape.buildVectorPath
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korma.geom.vector.circle
import com.soywiz.korma.geom.vector.getCurves
import com.soywiz.korma.interpolation.Easing

suspend fun Stage.mainTweenPoint() {
    container {
        this.scale(2.0)

        val circle = circle(64.0).xy(200.0, 200.0).anchor(Anchor.CENTER)
        val path = buildVectorPath {
            //circle(200, 200, 100)
            moveTo(200.0, 200.0)
            lineTo(400.0, 100.0)
            quadTo(400.0, 400.0, 200.0, 200.0)
        }
        val curves = path.getCurves()
        graphics(path.toStrokeShape(Colors.RED, thickness = 2.0))
        while (true) {
            tween(circle::pos[path, false], time = 1.0.seconds, easing = Easing.LINEAR)
            //circle.xy(0.0, 0.0)
        }
    }
}
