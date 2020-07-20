package com.badlogic.gdx.graphics.glutils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector2;

public class ShapeRenderer {
    public void begin(ShapeType filled) {
    }

    public void end() {
    }

    public void setColor(Color boneOriginColor) {

    }

    public void rectLine(float worldX, float worldY, float x, float y, float v) {

    }

    public void rectLine(Vector2 temp1, Vector2 temp2, float v) {

    }

    public void x(float x, float y, float v) {

    }

    public void line(float vertex, float vertex1, float vertex2, float vertex3) {

    }

    public void triangle(float vertex, float vertex1, float vertex2, float vertex3, float vertex4, float vertex5) {

    }

    public void rect(float minX, float minY, float width, float height) {

    }

    public void polygon(float[] items, int i, int size) {

    }

    public void curve(float x1, float y1, float cx1, float cy1, float cx2, float cy2, float x2, float y2, int i) {

    }

    public void circle(float worldX, float worldY, float v, int i) {

    }


    public enum ShapeType {
        Point(GL20.GL_POINTS), Line(GL20.GL_LINES), Filled(GL20.GL_TRIANGLES);

        private final int glType;

        ShapeType (int glType) {
            this.glType = glType;
        }

        public int getGlType () {
            return glType;
        }
    }
}
