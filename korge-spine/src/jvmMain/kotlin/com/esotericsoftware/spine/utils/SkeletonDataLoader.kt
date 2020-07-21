/******************************************************************************
 * Spine Runtimes License Agreement
 * Last updated January 1, 2020. Replaces all prior versions.
 *
 * Copyright (c) 2013-2020, Esoteric Software LLC
 *
 * Integration of the Spine Runtimes into software or otherwise creating
 * derivative works of the Spine Runtimes is permitted under the terms and
 * conditions of Section 2 of the Spine Editor License Agreement:
 * http://esotericsoftware.com/spine-editor-license
 *
 * Otherwise, it is permitted to integrate the Spine Runtimes into software
 * or otherwise create derivative works of the Spine Runtimes (collectively,
 * "Products"), provided that each user of the Products must obtain their own
 * Spine Editor license and redistribution of the Products in any form must
 * include this license and copyright notice.
 *
 * THE SPINE RUNTIMES ARE PROVIDED BY ESOTERIC SOFTWARE LLC "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL ESOTERIC SOFTWARE LLC BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES,
 * BUSINESS INTERRUPTION, OR LOSS OF USE, DATA, OR PROFITS) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THE SPINE RUNTIMES, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.esotericsoftware.spine.utils

import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.JArray

import com.esotericsoftware.spine.SkeletonBinary
import com.esotericsoftware.spine.SkeletonData
import com.esotericsoftware.spine.SkeletonJson
import com.esotericsoftware.spine.attachments.AtlasAttachmentLoader
import com.esotericsoftware.spine.attachments.AttachmentLoader

/** An asset loader to create and load skeleton data. The data file is assumed to be binary if it ends with `.skel`,
 * otherwise JSON is assumed. The [SkeletonDataParameter] can provide a texture atlas name or an [AttachmentLoader].
 * If neither is provided, a texture atlas name based on the skeleton file name with an `.atlas` extension is used.
 * When a texture atlas name is used, the texture atlas is loaded by the asset manager as a dependency.
 *
 *
 * Example:
 *
 * <pre>
 * // Load skeleton.json and skeleton.atlas:
 * assetManager.load("skeleton.json", SkeletonData.class);
 * // Or specify the atlas/AttachmentLoader and scale:
 * assetManager.setLoader(SkeletonData.class, new SkeletonDataLoader(new InternalFileHandleResolver()));
 * SkeletonDataParameter parameter = new SkeletonDataParameter("skeleton2x.atlas", 2);
 * assetManager.load("skeleton.json", SkeletonData.class, parameter);
</pre> *
 */
class SkeletonDataLoader(resolver: FileHandleResolver) : AsynchronousAssetLoader<SkeletonData, SkeletonDataLoader.SkeletonDataParameter>(resolver) {
    private var skeletonData: SkeletonData? = null

    /** @param parameter May be null.
     */
    fun loadAsync(manager: AssetManager, fileName: String, file: FileHandle, parameter: SkeletonDataParameter?) {
        var scale = 1f
        var attachmentLoader: AttachmentLoader? = null
        if (parameter != null) {
            scale = parameter.scale
            if (parameter.attachmentLoader != null)
                attachmentLoader = parameter.attachmentLoader
            else if (parameter.atlasName != null)
                attachmentLoader = AtlasAttachmentLoader(manager.get(parameter.atlasName, TextureAtlas::class.java))
        }
        if (attachmentLoader == null)
            attachmentLoader = AtlasAttachmentLoader(manager.get(file.pathWithoutExtension()!! + ".atlas", TextureAtlas::class.java))

        if (file.extension()!!.equals("skel", ignoreCase = true)) {
            val skeletonBinary = SkeletonBinary(attachmentLoader)
            skeletonBinary.scale = scale
            skeletonData = skeletonBinary.readSkeletonData(file)
        } else {
            val skeletonJson = SkeletonJson(attachmentLoader)
            skeletonJson.scale = scale
            skeletonData = skeletonJson.readSkeletonData(file)
        }
    }

    /** @param parameter May be null.
     */
    fun loadSync(manager: AssetManager, fileName: String, file: FileHandle, parameter: SkeletonDataParameter): SkeletonData? {
        val skeletonData = this.skeletonData
        this.skeletonData = null
        return skeletonData
    }

    /** @param parameter May be null.
     */
    fun getDependencies(fileName: String, file: FileHandle, parameter: SkeletonDataParameter?): JArray<AssetDescriptor>? {
        if (parameter == null) return null
        if (parameter.attachmentLoader != null) return null
        val dependencies = JArray<AssetDescriptor>()
        dependencies.add(AssetDescriptor(parameter.atlasName, TextureAtlas::class.java))
        return dependencies
    }

    class SkeletonDataParameter : AssetLoaderParameters<SkeletonData> {
        var atlasName: String? = null
        var attachmentLoader: AttachmentLoader? = null
        var scale = 1f

        constructor() {}

        constructor(atlasName: String) {
            this.atlasName = atlasName
        }

        constructor(atlasName: String, scale: Float) {
            this.atlasName = atlasName
            this.scale = scale
        }

        constructor(attachmentLoader: AttachmentLoader) {
            this.attachmentLoader = attachmentLoader
        }

        constructor(attachmentLoader: AttachmentLoader, scale: Float) {
            this.attachmentLoader = attachmentLoader
            this.scale = scale
        }
    }
}
