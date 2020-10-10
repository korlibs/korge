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

package com.esotericsoftware.spine.attachments

import com.esotericsoftware.spine.*

import com.soywiz.korim.atlas.*

/** An [AttachmentLoader] that configures attachments using texture regions from an [Atlas].
 *
 *
 * See [Loading skeleton data](http://esotericsoftware.com/spine-loading-skeleton-data#JSON-and-binary-data) in the
 * Spine Runtimes Guide.  */
class AtlasAttachmentLoader(private val atlas: Atlas) : AttachmentLoader {
    private val regions = HashMap<String, SpineRegion>()

    private fun findRegion(path: String): SpineRegion? {
        return regions.getOrPut(path) {
            val entry = atlas.tryGetEntryByName(path) ?: error("Can't find '$path' in atlas")
            SpineRegion(entry)
        }
    }
    override fun newRegionAttachment(skin: Skin, name: String, path: String): RegionAttachment? {
        val region = findRegion(path)
                ?: throw RuntimeException("Region not found in atlas: $path (region attachment: $name)")
        val attachment = RegionAttachment(name)
        attachment.region = region
        return attachment
    }

    override fun newMeshAttachment(skin: Skin, name: String, path: String): MeshAttachment? {
        val region = findRegion(path)
                ?: throw RuntimeException("Region not found in atlas: $path (mesh attachment: $name)")
        val attachment = MeshAttachment(name)
        attachment.region = region
        return attachment
    }

    override fun newBoundingBoxAttachment(skin: Skin, name: String): BoundingBoxAttachment? {
        return BoundingBoxAttachment(name)
    }

    override fun newClippingAttachment(skin: Skin, name: String): ClippingAttachment? {
        return ClippingAttachment(name)
    }

    override fun newPathAttachment(skin: Skin, name: String): PathAttachment? {
        return PathAttachment(name)
    }

    override fun newPointAttachment(skin: Skin, name: String): PointAttachment? {
        return PointAttachment(name)
    }
}
