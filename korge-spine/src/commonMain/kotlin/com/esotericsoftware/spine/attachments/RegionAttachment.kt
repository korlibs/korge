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

import com.esotericsoftware.spine.SpineRegion

import com.esotericsoftware.spine.Bone
import com.esotericsoftware.spine.utils.*
import com.esotericsoftware.spine.utils.SpineUtils.arraycopy
import com.soywiz.korim.color.*

/** An attachment that displays a textured quadrilateral.
 *
 *
 * See [Region attachments](http://esotericsoftware.com/spine-regions) in the Spine User Guide.  */
class RegionAttachment(name: String) : Attachment(name) {

    private var _region: SpineRegion? = null

    var region: SpineRegion
        get() = _region ?: error("Region was not set before")
        set(region) {
            _region = region
            val uvs = this.uVs
            if (region.rotate) {
                uvs[URX] = region.u
                uvs[URY] = region.v2
                uvs[BRX] = region.u
                uvs[BRY] = region.v
                uvs[BLX] = region.u2
                uvs[BLY] = region.v
                uvs[ULX] = region.u2
                uvs[ULY] = region.v2
            } else {
                uvs[ULX] = region.u
                uvs[ULY] = region.v2
                uvs[URX] = region.u
                uvs[URY] = region.v
                uvs[BRX] = region.u2
                uvs[BRY] = region.v
                uvs[BLX] = region.u2
                uvs[BLY] = region.v2
            }
        }

    /** The name of the texture region for this attachment.  */
    var path: String? = null

    /** The local x translation.  */
    var x: Float = 0.toFloat()

    /** The local y translation.  */
    var y: Float = 0.toFloat()

    /** The local scaleX.  */
    var scaleX = 1f

    /** The local scaleY.  */
    var scaleY = 1f

    /** The local rotation.  */
    var rotation: Float = 0.toFloat()

    /** The width of the region attachment in Spine.  */
    var width: Float = 0.toFloat()

    /** The height of the region attachment in Spine.  */
    var height: Float = 0.toFloat()
    val uVs = FloatArray(8)

    /** For each of the 4 vertices, a pair of `x,y` values that is the local position of the vertex.
     *
     *
     * See [.updateOffset].  */
    val offset = FloatArray(8)

    /** The color to tint the region attachment.  */
    val color = RGBAf(1f, 1f, 1f, 1f)

    /** Calculates the [.offset] using the region settings. Must be called after changing region settings.  */
    fun updateOffset() {
        val width = width
        val height = height
        var localX2 = width / 2
        var localY2 = height / 2
        var localX = -localX2
        var localY = -localY2
        if (region is SpineRegion) {
            val region = this.region as SpineRegion?
            localX += region!!.offsetX / region.originalWidth * width
            localY += region.offsetY / region.originalHeight * height
            if (region.rotate) {
                localX2 -= (region.originalWidth - region.offsetX - region.packedHeight) / region.originalWidth * width
                localY2 -= (region.originalHeight - region.offsetY - region.packedWidth) / region.originalHeight * height
            } else {
                localX2 -= (region.originalWidth - region.offsetX - region.packedWidth) / region.originalWidth * width
                localY2 -= (region.originalHeight - region.offsetY - region.packedHeight) / region.originalHeight * height
            }
        }
        val scaleX = scaleX
        val scaleY = scaleY
        localX *= scaleX
        localY *= scaleY
        localX2 *= scaleX
        localY2 *= scaleY
        val rotation = rotation
        val cos = kotlin.math.cos((SpineUtils.degRad * rotation).toDouble()).toFloat()
        val sin = kotlin.math.sin((SpineUtils.degRad * rotation).toDouble()).toFloat()
        val x = x
        val y = y
        val localXCos = localX * cos + x
        val localXSin = localX * sin
        val localYCos = localY * cos + y
        val localYSin = localY * sin
        val localX2Cos = localX2 * cos + x
        val localX2Sin = localX2 * sin
        val localY2Cos = localY2 * cos + y
        val localY2Sin = localY2 * sin
        val offset = this.offset
        offset[BLX] = localXCos - localYSin
        offset[BLY] = localYCos + localXSin
        offset[ULX] = localXCos - localY2Sin
        offset[ULY] = localY2Cos + localXSin
        offset[URX] = localX2Cos - localY2Sin
        offset[URY] = localY2Cos + localX2Sin
        offset[BRX] = localX2Cos - localYSin
        offset[BRY] = localYCos + localX2Sin
    }

    /** Transforms the attachment's four vertices to world coordinates.
     *
     *
     * See [World transforms](http://esotericsoftware.com/spine-runtime-skeletons#World-transforms) in the Spine
     * Runtimes Guide.
     * @param worldVertices The output world vertices. Must have a length >= `offset` + 8.
     * @param offset The `worldVertices` index to begin writing values.
     * @param stride The number of `worldVertices` entries between the value pairs written.
     */
    fun computeWorldVertices(bone: Bone, worldVertices: FloatArray, offset: Int, stride: Int) {
        var offset = offset
        val vertexOffset = this.offset
        val x = bone.worldX
        val y = bone.worldY
        val a = bone.a
        val b = bone.b
        val c = bone.c
        val d = bone.d
        var offsetX: Float
        var offsetY: Float

        offsetX = vertexOffset[BRX]
        offsetY = vertexOffset[BRY]
        worldVertices[offset] = offsetX * a + offsetY * b + x // br
        worldVertices[offset + 1] = offsetX * c + offsetY * d + y
        offset += stride

        offsetX = vertexOffset[BLX]
        offsetY = vertexOffset[BLY]
        worldVertices[offset] = offsetX * a + offsetY * b + x // bl
        worldVertices[offset + 1] = offsetX * c + offsetY * d + y
        offset += stride

        offsetX = vertexOffset[ULX]
        offsetY = vertexOffset[ULY]
        worldVertices[offset] = offsetX * a + offsetY * b + x // ul
        worldVertices[offset + 1] = offsetX * c + offsetY * d + y
        offset += stride

        offsetX = vertexOffset[URX]
        offsetY = vertexOffset[URY]
        worldVertices[offset] = offsetX * a + offsetY * b + x // ur
        worldVertices[offset + 1] = offsetX * c + offsetY * d + y
    }

    override fun copy(): Attachment {
        val copy = RegionAttachment(name)
        copy.region = region
        copy.path = path
        copy.x = x
        copy.y = y
        copy.scaleX = scaleX
        copy.scaleY = scaleY
        copy.rotation = rotation
        copy.width = width
        copy.height = height
        arraycopy(uVs, 0, copy.uVs, 0, 8)
        arraycopy(offset, 0, copy.offset, 0, 8)
        copy.color.setTo(color)
        return copy
    }

    companion object {
        val BLX = 0
        val BLY = 1
        val ULX = 2
        val ULY = 3
        val URX = 4
        val URY = 5
        val BRX = 6
        val BRY = 7
    }
}
