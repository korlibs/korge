package com.badlogic.gdx.graphics.g2d;

import com.badlogic.gdx.graphics.Texture;

public interface Batch {
    default void begin() {
    }

    default void end() {
    }

    default int getBlendSrcFunc() {
        return 0;
    }

    default int getBlendDstFunc() {
        return 0;
    }

    default int getBlendSrcFuncAlpha() {
        return 0;
    }

    default int getBlendDstFuncAlpha() {
        return 0;
    }

    default void setBlendFunctionSeparate(int blendSrc, int blendDst, int blendSrcAlpha, int blendDstAlpha) {
    }

    default void setBlendFunction(int source, int dest) {
    }

    default void draw(Texture texture, float[] vertices, int index0, int index1) {
    }
}
