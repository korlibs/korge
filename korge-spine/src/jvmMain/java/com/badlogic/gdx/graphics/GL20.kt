package com.badlogic.gdx.graphics;

public interface GL20 {
    int GL_SRC_ALPHA = 0;
    int GL_ONE = 1;
    int GL_ONE_MINUS_SRC_ALPHA = 2;
    int GL_DST_COLOR = 3;
    int GL_ONE_MINUS_SRC_COLOR = 4;

    int GL_POINTS = 0;
    int GL_LINES = 1;
    int GL_TRIANGLES = 2;
    int GL_BLEND = 0;

    void glEnable(int glBlend);

    void glBlendFunc(int srcFunc, int glOneMinusSrcAlpha);

    void glDepthMask(boolean b);

    void glDisable(int glBlend);

    void glBlendFuncSeparate(int blendSrcFunc, int blendDstFunc, int blendSrcFuncAlpha, int blendDstFuncAlpha);
}
