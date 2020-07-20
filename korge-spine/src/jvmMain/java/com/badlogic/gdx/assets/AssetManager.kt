package com.badlogic.gdx.assets

class AssetManager {
    fun <T> get(atlasName: String?, textureAtlasClass: Class<T>?): T {
        throw NotImplementedError()
    }
}
