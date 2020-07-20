package com.badlogic.gdx.assets;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import kotlin.NotImplementedError;

public class AssetManager {
    public <T> T get(String atlasName, Class<T> textureAtlasClass) {
        throw new NotImplementedError();
    }
}
