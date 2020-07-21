package com.esotericsoftware.spine.rendering

import com.esotericsoftware.spine.graphics.*
import com.esotericsoftware.spine.utils.Vector2

class ShapeRenderer {
    fun begin(filled: ShapeType?) {}
    fun end() {}
    fun setColor(boneOriginColor: Color?) {}
    fun rectLine(worldX: Float, worldY: Float, x: Float, y: Float, v: Float) {}
    fun rectLine(temp1: Vector2?, temp2: Vector2?, v: Float) {}
    fun x(x: Float, y: Float, v: Float) {}
    fun line(vertex: Float, vertex1: Float, vertex2: Float, vertex3: Float) {}
    fun triangle(vertex: Float, vertex1: Float, vertex2: Float, vertex3: Float, vertex4: Float, vertex5: Float) {}
    fun rect(minX: Float, minY: Float, width: Float, height: Float) {}
    fun polygon(items: FloatArray?, i: Int, size: Int) {}
    fun curve(x1: Float, y1: Float, cx1: Float, cy1: Float, cx2: Float, cy2: Float, x2: Float, y2: Float, i: Int) {}
    fun circle(worldX: Float, worldY: Float, v: Float, i: Int) {}
    enum class ShapeType(val glType: Int) {
        Point(GL20.GL_POINTS), Line(GL20.GL_LINES), Filled(GL20.GL_TRIANGLES);
    }
}
