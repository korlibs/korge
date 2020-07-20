package com.badlogic.gdx.graphics.g2d;

import com.badlogic.gdx.graphics.Texture;
import kotlin.NotImplementedError;

public class TextureRegion {
    public float getU() {
        throw new NotImplementedError();
    }

    public float getV() {
        throw new NotImplementedError();
    }

    public float getU2() {
        throw new NotImplementedError();
    }

    public float getV2() {
        throw new NotImplementedError();
    }

    public Texture getTexture() {
        throw new NotImplementedError();
    }

    public float getRegionWidth() {
        return 0;
    }

    public float getRegionHeight() {
        return 0;
    }
}
