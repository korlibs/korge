package com.esotericsoftware.spine.assets

import kotlin.reflect.*

class AssetManager {
    fun <T : Any> get(atlasName: String?, textureAtlasClass: KClass<T>?): T {
        throw NotImplementedError()
    }
}
