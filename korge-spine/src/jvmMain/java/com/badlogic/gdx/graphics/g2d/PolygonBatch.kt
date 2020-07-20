package com.badlogic.gdx.graphics.g2d;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Matrix4;

public interface PolygonBatch extends Batch {
    default void setColor (Color tint) {
    }

    default void setColor(float r, float g, float b, float a) {
    }

    default void setPackedColor(float packedColor) {
    }

    default Color getColor() {
        return null;
    }

    default float getPackedColor() {
        return 0f;
    }

    default void draw (Texture texture, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
    }

    default void draw(Texture texture, float x, float y, float width, float height, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
    }

    default void draw(Texture texture, float x, float y, int srcX, int srcY, int srcWidth, int srcHeight) {
    }

    default void draw(Texture texture, float x, float y, float width, float height, float u, float v, float u2, float v2) {
    }

    default void draw(Texture texture, float x, float y) {
    }

    default void draw(Texture texture, float x, float y, float width, float height) {
    }

    default void draw(TextureRegion region, float x, float y) {
    }

    default void draw(TextureRegion region, float x, float y, float width, float height) {
    }

    default void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
    }

    default void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation, boolean clockwise) {
    }

    default void draw (TextureRegion region, float width, float height, Affine2 transform) {
    }

    default void flush() {
    }

    default void disableBlending() {
    }

    default void enableBlending() {
    }

    default void dispose() {
    }

    default Matrix4 getProjectionMatrix() {
        return null;
    }

    default Matrix4 getTransformMatrix() {
        return null;
    }

    default void setProjectionMatrix(Matrix4 projection) {
    }

    default void setTransformMatrix(Matrix4 transform) {
    }

    default void setShader (ShaderProgram newShader) {
    }

    default ShaderProgram getShader() {
        return null;
    }

    default boolean isBlendingEnabled() {
        return false;
    }

    default boolean isDrawing() {
        return false;
    }
}
