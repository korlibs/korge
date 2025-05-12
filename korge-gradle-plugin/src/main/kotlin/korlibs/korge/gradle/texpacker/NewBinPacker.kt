package korlibs.korge.gradle.texpacker

import kotlin.math.*

// Based on https://github.com/soimy/maxrects-packer/
object NewBinPacker {
    open class IBinPackerData {
        var allowRotation: Boolean? = null
        var tag: String? = null
    }

    open class Rectangle(
        width: Int = 0,
        height: Int = 0,
        x: Int = 0,
        y: Int = 0,
        rot: Boolean = false,
        allowRotation: Boolean? = null,
        val name: String? = null,
        val raw: Any? = null,
    ) {
        val right: Int get() = x + width
        val bottom: Int get() = y + height

        override fun toString(): String = "Rectangle($x, $y, $width, $height)[rot=$rot]"

        override fun hashCode(): Int {
            return width * 1 + height * 3 + x * 7 + y * 11 + (if (rot) 1 else 0) + (if (allowRotation == true) 3333 else 0)
        }

        var hash: Int? = null

        /**
         * Oversized tag on rectangle which is bigger than packer itself.
         *
         * @type {boolean}
         * @memberof Rectangle
         */
        var oversized: Boolean = false


        /**
         * Get the area (w * h) of the rectangle
         *
         * @returns {number}
         * @memberof Rectangle
         */
        fun area(): Int {
            return this.width * this.height; }

        /**
         * Test if the given rectangle collide with this rectangle.
         *
         * @param {Rectangle} rect
         * @returns {boolean}
         * @memberof Rectangle
         */
        fun collide(rect: Rectangle): Boolean {
            return (
                rect.x < this.x + this.width &&
                    rect.x + rect.width > this.x &&
                    rect.y < this.y + this.height &&
                    rect.y + rect.height > this.y
                )
        }

        companion object {
            /**
             * Test if two given rectangle collide each other
             *
             * @static
             * @param {Rectangle} first
             * @param {Rectangle} second
             * @returns
             * @memberof Rectangle
             */
            fun Collide(first: Rectangle, second: Rectangle): Boolean {
                return first.collide(second); }

            /**
             * Test if the first rectangle contains the second one
             *
             * @static
             * @param {Rectangle} first
             * @param {Rectangle} second
             * @returns
             * @memberof Rectangle
             */
            fun Contain(first: Rectangle, second: Rectangle): Boolean {
                return first.contain(second); }

        }

        /**
         * Test if this rectangle contains the given rectangle.
         *
         * @param {Rectangle} rect
         * @returns {boolean}
         * @memberof Rectangle
         */
        fun contain(rect: Rectangle): Boolean {
            return (rect.x >= this.x && rect.y >= this.y &&
                rect.x + rect.width <= this.x + this.width && rect.y + rect.height <= this.y + this.height)
        }

        var width: Int = width
            set(value) {
                if (field == value) return
                field = value
                _dirty++
            }

        var height: Int = height
            set(value) {
                if (field == value) return
                field = value
                _dirty++
            }

        var x: Int = x
            set(value) {
                if (field == value) return
                field = value
                _dirty++
            }

        var y: Int = y
            set(value) {
                if (field == value) return
                field = value
                _dirty++
            }

        protected var _rot: Boolean = rot

        /**
         * If the rectangle is rotated
         *
         * @type {boolean}
         * @memberof Rectangle
         */
        var rot: Boolean
            get() {
                return this._rot; }
            /**
             * Set the rotate tag of the rectangle.
             *
             * note: after `rot` is set, `width/height` of this rectangle is swaped.
             *
             * @memberof Rectangle
             */
            set(value) {
                if (this._allowRotation == false) return

                if (this._rot != value) {
                    val tmp = this.width
                    this.width = this.height
                    this.height = tmp
                    this._rot = value
                    this._dirty++
                }

            }

        protected var _allowRotation: Boolean? = allowRotation

        /**
         * If the rectangle allow rotation
         *
         * @type {boolean}
         * @memberof Rectangle
         */
        var allowRotation: Boolean?
            get() {
                return this._allowRotation; }
            /**
             * Set the allowRotation tag of the rectangle.
             *
             * @memberof Rectangle
             */
            set(value) {
                if (this._allowRotation !== value) {
                    this._allowRotation = value
                    this._dirty++
                }
            }

        protected var _data: IBinPackerData? = null

        var data: IBinPackerData?
            get() = this._data
            set(value) {
                if (value === null || value === this._data) return
                this._data = value
                // extract allowRotation settings
                if (value.allowRotation != null) {
                    this._allowRotation = value.allowRotation
                }
                this._dirty++
            }

        protected var _dirty: Int = 0
        val dirty: Boolean get() = this._dirty > 0
        fun setDirty(value: Boolean = true): Unit {
            this._dirty = if (value) this._dirty + 1 else 0
        }

        var tag: String? = null
    }

    interface IBin {
        val width: Int
        val height: Int
        val maxWidth: Int
        val maxHeight: Int
        val freeRects: MutableList<Rectangle>
        val rects: List<Rectangle>
        val options: IOption
        var data: IBinPackerData?
        var tag: String?
        //[propName: String]: Any
    }

    data class MBin(
        override val width: Int,
        override val height: Int,
        override val maxWidth: Int,
        override val maxHeight: Int,
        override val freeRects: MutableList<Rectangle> = arrayListOf(),
        override val rects: List<Rectangle> = listOf(),
        override val options: IOption,
        override var data: IBinPackerData? = null,
        override var tag: String? = null,
    ) : IBin


    abstract class Bin : IBin {
        abstract fun add(rect: Rectangle): Rectangle?
        abstract fun add(width: Int, height: Int, data: IBinPackerData?): Rectangle?
        abstract fun reset(deepReset: Boolean = false, resetOption: Boolean = false): Unit
        abstract fun repack(): List<Rectangle>?

        override var data: IBinPackerData? = null
        override var tag: String? = null

        protected var _dirty: Int = 0

        val dirty: Boolean get() = this._dirty > 0 || this.rects.any { it.dirty }
        /**
         * Set bin dirty status
         *
         * @memberof Bin
         */
        fun setDirty(value: Boolean = true): Unit {
            this._dirty = if (value) this._dirty + 1 else 0
            if (!value) {
                for (rect in this.rects) {
                    rect.setDirty(false)
                }
            }
        }

        abstract fun clone(): Bin
    }

    class OversizedElementBin private constructor(
        override var width: Int,
        override var height: Int,
        override var data: IBinPackerData? = null,
        val dummy: Unit
    ) : Bin() {
        override val maxWidth: Int = width
        override val maxHeight: Int = height
        override var options: IOption = IOption(smart = false, pot = false, square = false)
        override var rects: MutableList<Rectangle> = arrayListOf()
        override var freeRects: MutableList<Rectangle> = arrayListOf()

        constructor(rect: Rectangle, data: IBinPackerData? = null) : this(rect.width, rect.height, data, dummy = Unit) {
            rect.oversized = true
            this.rects.add(rect)
        }

        constructor (width: Int, height: Int, data: IBinPackerData?) : this(Rectangle(width, height), data)

        override fun add(rect: Rectangle): Rectangle? = null
        override fun add(width: Int, height: Int, data: IBinPackerData?): Rectangle? = null

        override fun reset(deepReset: Boolean, resetOption: Boolean) {
            // nothing to do here
        }

        override fun repack(): List<Rectangle>? {
            return null; }

        override fun clone(): Bin {
            val clonedBin: OversizedElementBin = OversizedElementBin(this.rects[0])
            return clonedBin
        }
    }

    class MaxRectsBin(
        override val maxWidth: Int = EDGE_MAX_VALUE,
        override val maxHeight: Int = EDGE_MAX_VALUE,
        val padding: Int = 0,
        override var options: IOption = IOption(
            smart = true,
            pot = true,
            square = true,
            allowRotation = false,
            tag = false,
            exclusiveTag = true,
            border = 0,
            logic = PACKING_LOGIC.MAX_EDGE
        )
    ) : Bin() {
        override var width: Int = if (this.options.smart) 0 else maxWidth
        override var height: Int = if (this.options.smart) 0 else maxHeight
        var border: Int = this.options.border
        override var freeRects: MutableList<Rectangle> = arrayListOf(
            Rectangle(
                this.maxWidth + this.padding - this.border * 2,
                this.maxHeight + this.padding - this.border * 2,
                this.border,
                this.border
            )
        )
        override var rects: MutableList<Rectangle> = arrayListOf()
        private var verticalExpand: Boolean = false
        private var stage: Rectangle = Rectangle(this.width, this.height)

        override fun add(rect: Rectangle): Rectangle? {
            // Check if rect.tag match bin.tag, if bin.tag not defined, it will accept any rect
            val tag = rect.data?.tag ?: rect.tag
            if (this.options.tag && this.options.exclusiveTag && this.tag !== tag) return null
            val result = this.place(rect)
            if (result != null) this.rects.add(result)
            return result
        }

        override fun add(width: Int, height: Int, data: IBinPackerData?): Rectangle? {
            // Check if data.tag match bin.tag, if bin.tag not defined, it will accept any rect
            if (this.options.tag && this.options.exclusiveTag) {
                if (data != null && this.tag !== data.tag) return null
                if (data == null && this.tag != null) return null
            }
            val rect = Rectangle(width, height)
            rect.data = data
            rect.setDirty(false)
            val result = this.place(rect)
            if (result != null) this.rects.add(result)
            return result
        }

        override fun repack(): List<Rectangle>? {
            val unpacked: MutableList<Rectangle> = arrayListOf()
            this.reset()
            // re-sort rects from big to small
            this.rects.sortWith(Comparator { a, b ->
                val result = Math.max(b.width, b.height) - Math.max(a.width, a.height)
                if (result == 0 && a.hash != null && b.hash != null) {
                    if (a.hash!! > b.hash!!) -1 else 1
                } else {
                    result
                }
            })
            for (rect in this.rects) {
                if (this.place(rect) == null) {
                    unpacked.add(rect)
                }
            }
            for (rect in unpacked) this.rects.removeAt(this.rects.indexOf(rect))
            return if (unpacked.size > 0) unpacked else null
        }

        override fun reset(deepReset: Boolean, resetOption: Boolean): Unit {
            if (deepReset) {
                if (this.data != null) this.data = null
                if (this.tag != null) this.tag = null
                this.rects = arrayListOf()
                if (resetOption) {
                    this.options = IOption(
                        smart = true,
                        pot = true,
                        square = true,
                        allowRotation = false,
                        tag = false,
                        border = 0
                    )
                }
            }
            this.width = if (this.options.smart) 0 else this.maxWidth
            this.height = if (this.options.smart) 0 else this.maxHeight
            this.border = if (this.options.border != 0) this.options.border else 0
            this.freeRects = arrayListOf(
                Rectangle(
                    this.maxWidth + this.padding - this.border * 2,
                    this.maxHeight + this.padding - this.border * 2,
                    this.border,
                    this.border
                )
            )
            this.stage = Rectangle(this.width, this.height)
            this._dirty = 0
        }

        override fun clone(): MaxRectsBin {
            var clonedBin: MaxRectsBin = MaxRectsBin(this.maxWidth, this.maxHeight, this.padding, this.options)
            for (rect in this.rects) {
                clonedBin.add(rect)
            }
            return clonedBin
        }

        private fun place(rect: Rectangle): Rectangle? {
            // recheck if tag matched
            var tag = rect.data?.tag ?: rect.tag
            if (this.options.tag && this.options.exclusiveTag && this.tag !== tag) return null

            val node: Rectangle?
            val allowRotation: Boolean = rect.allowRotation ?: this.options.allowRotation
            node = this.findNode(rect.width + this.padding, rect.height + this.padding, allowRotation)

            if (node != null) {
                this.updateBinSize(node)
                var numRectToProcess = this.freeRects.size
                var i: Int = 0
                while (i < numRectToProcess) {
                    if (this.splitNode(this.freeRects[i], node)) {
                        this.freeRects.removeAt(i)
                        numRectToProcess--
                        i--
                    }
                    i++
                }
                this.pruneFreeList()
                this.verticalExpand = this.width > this.height
                rect.x = node.x
                rect.y = node.y
                if (rect.rot == null) rect.rot = false
                rect.rot = if (node.rot) !rect.rot else rect.rot
                this._dirty++
                return rect
            } else if (!this.verticalExpand) {
                if (this.updateBinSize(
                        Rectangle(
                            rect.width + this.padding, rect.height + this.padding,
                            this.width + this.padding - this.border, this.border
                        )
                    ) || this.updateBinSize(
                        Rectangle(
                            rect.width + this.padding, rect.height + this.padding,
                            this.border, this.height + this.padding - this.border
                        )
                    )
                ) {
                    return this.place(rect)
                }
            } else {
                if (this.updateBinSize(
                        Rectangle(
                            rect.width + this.padding, rect.height + this.padding,
                            this.border, this.height + this.padding - this.border
                        )
                    ) || this.updateBinSize(
                        Rectangle(
                            rect.width + this.padding, rect.height + this.padding,
                            this.width + this.padding - this.border, this.border
                        )
                    )
                ) {
                    return this.place(rect)
                }
            }
            return null
        }

        private fun findNode(width: Int, height: Int, allowRotation: Boolean): Rectangle? {
            var score: Int = Int.MAX_VALUE
            var bestNode: Rectangle? = null
            for (r in this.freeRects) {
                if (r.width >= width && r.height >= height) {
                    val areaFit = when (this.options.logic) {
                        PACKING_LOGIC.MAX_AREA -> r.width * r.height - width * height
                        else -> Math.min(r.width - width, r.height - height)
                    }
                    if (areaFit < score) {
                        bestNode = Rectangle(width, height, r.x, r.y)
                        score = areaFit
                    }
                }

                if (!allowRotation) continue

                // Continue to test 90-degree rotated rectangle
                if (r.width >= height && r.height >= width) {
                    val areaFit = when (this.options.logic) {
                        PACKING_LOGIC.MAX_AREA -> r.width * r.height - height * width
                        else -> Math.min(r.height - width, r.width - height)
                    }
                    if (areaFit < score) {
                        bestNode = Rectangle(height, width, r.x, r.y, true) // Rotated node
                        score = areaFit
                    }
                }
            }
            return bestNode
        }

        private fun splitNode(freeRect: Rectangle, usedNode: Rectangle): Boolean {
            // Test if usedNode intersect with freeRect
            if (!freeRect.collide(usedNode)) return false

            // Do vertical split
            if (usedNode.x < freeRect.x + freeRect.width && usedNode.x + usedNode.width > freeRect.x) {
                // New node at the top side of the used node
                if (usedNode.y > freeRect.y && usedNode.y < freeRect.y + freeRect.height) {
                    val newNode: Rectangle = Rectangle(freeRect.width, usedNode.y - freeRect.y, freeRect.x, freeRect.y)
                    this.freeRects.add(newNode)
                }
                // New node at the bottom side of the used node
                if (usedNode.y + usedNode.height < freeRect.y + freeRect.height) {
                    val newNode = Rectangle(
                        freeRect.width,
                        freeRect.y + freeRect.height - (usedNode.y + usedNode.height),
                        freeRect.x,
                        usedNode.y + usedNode.height
                    )
                    this.freeRects.add(newNode)
                }
            }

            // Do Horizontal split
            if (usedNode.y < freeRect.y + freeRect.height &&
                usedNode.y + usedNode.height > freeRect.y
            ) {
                // New node at the left side of the used node.
                if (usedNode.x > freeRect.x && usedNode.x < freeRect.x + freeRect.width) {
                    val newNode = Rectangle(usedNode.x - freeRect.x, freeRect.height, freeRect.x, freeRect.y)
                    this.freeRects.add(newNode)
                }
                // New node at the right side of the used node.
                if (usedNode.x + usedNode.width < freeRect.x + freeRect.width) {
                    val newNode = Rectangle(
                        freeRect.x + freeRect.width - (usedNode.x + usedNode.width),
                        freeRect.height,
                        usedNode.x + usedNode.width,
                        freeRect.y
                    )
                    this.freeRects.add(newNode)
                }
            }
            return true
        }

        private fun pruneFreeList() {
            // Go through each pair of freeRects and remove any rects that is redundant
            var i: Int = 0
            var j: Int = 0
            var len: Int = this.freeRects.size
            while (i < len) {
                j = i + 1
                var tmpRect1 = this.freeRects[i]
                while (j < len) {
                    var tmpRect2 = this.freeRects[j]
                    if (tmpRect2.contain(tmpRect1)) {
                        this.freeRects.removeAt(i)
                        i--
                        len--
                        break
                    }
                    if (tmpRect1.contain(tmpRect2)) {
                        this.freeRects.removeAt(j)
                        j--
                        len--
                    }
                    j++
                }
                i++
            }
        }

        private fun updateBinSize(node: Rectangle): Boolean {
            if (!this.options.smart) return false
            if (this.stage.contain(node)) return false
            var tmpWidth: Int = Math.max(this.width, node.x + node.width - this.padding + this.border)
            var tmpHeight: Int = Math.max(this.height, node.y + node.height - this.padding + this.border)
            //println("updateBinSize: $tmpWidth, $tmpHeight : $node")
            if (this.options.allowRotation) {
                // do extra test on rotated node whether it's a better choice
                val rotWidth: Int = Math.max(this.width, node.x + node.height - this.padding + this.border)
                val rotHeight: Int = Math.max(this.height, node.y + node.width - this.padding + this.border)
                if (rotWidth * rotHeight < tmpWidth * tmpHeight) {
                    tmpWidth = rotWidth
                    tmpHeight = rotHeight
                }
            }
            if (this.options.pot) {
                tmpWidth = 2.0.pow(ceil(log2(tmpWidth.toDouble()))).toInt()
                tmpHeight = 2.0.pow(ceil(log2(tmpHeight.toDouble()))).toInt()
                //println("tmpWidth=$tmpWidth, tmpHeight=$tmpHeight")
            }
            if (this.options.square) {
                val max = Math.max(tmpWidth, tmpHeight)
                tmpWidth = max
                tmpHeight = max
            }
            if (tmpWidth > this.maxWidth + this.padding || tmpHeight > this.maxHeight + this.padding) {
                return false
            }
            this.expandFreeRects(tmpWidth + this.padding, tmpHeight + this.padding)
            this.width = tmpWidth; this.stage.width = tmpWidth
            this.height = tmpHeight; this.stage.height = tmpHeight
            return true
        }

        private fun expandFreeRects(width: Int, height: Int) {
            for (freeRect in this.freeRects) {
                if (freeRect.x + freeRect.width >= Math.min(this.width + this.padding - this.border, width)) {
                    freeRect.width = width - freeRect.x - this.border
                }
                if (freeRect.y + freeRect.height >= Math.min(this.height + this.padding - this.border, height)) {
                    freeRect.height = height - freeRect.y - this.border
                }
            }
            this.freeRects.add(
                Rectangle(
                    width - this.width - this.padding,
                    height - this.border * 2,
                    this.width + this.padding - this.border,
                    this.border
                )
            )
            this.freeRects.add(
                Rectangle(
                    width - this.border * 2,
                    height - this.height - this.padding,
                    this.border,
                    this.height + this.padding - this.border
                )
            )
            this.freeRects = this.freeRects.filter { freeRect ->
                !(freeRect.width <= 0 || freeRect.height <= 0 || freeRect.x < this.border || freeRect.y < this.border)
            }.toMutableList()
            this.pruneFreeList()
        }
    }

    const val EDGE_MAX_VALUE: Int = 4096
    const val EDGE_MIN_VALUE: Int = 128

    enum class PACKING_LOGIC { MAX_AREA, MAX_EDGE }

    /**
     * Options for MaxRect Packer
     *
     * @property {boolean} options.smart Smart sizing packer (default is true)
     * @property {boolean} options.pot use power of 2 sizing (default is true)
     * @property {boolean} options.square use square size (default is false)
     * @property {boolean} options.allowRotation allow rotation packing (default is false)
     * @property {boolean} options.tag allow auto grouping based on `rect.tag` (default is false)
     * @property {boolean} options.exclusiveTag tagged rects will have dependent bin, if set to `false`, packer will try to put tag rects into the same bin (default is true)
     * @property {boolean} options.border atlas edge spacing (default is 0)
     * @property {PACKING_LOGIC} options.logic MAX_AREA or MAX_EDGE based sorting logic (default is MAX_EDGE)
     * @export
     * @interface Option
     */
    data class IOption(
        val smart: Boolean = true,
        val pot: Boolean = true,
        val square: Boolean = false,
        val allowRotation: Boolean = false,
        val tag: Boolean = false,
        val exclusiveTag: Boolean = true,
        val border: Int = 0,
        val logic: PACKING_LOGIC = PACKING_LOGIC.MAX_EDGE,
    )

    /**
     * Creates an instance of MaxRectsPacker.
     *
     * @param {number} width of the output atlas (default is 4096)
     * @param {number} height of the output atlas (default is 4096)
     * @param {number} padding between glyphs/images (default is 0)
     * @param {IOption} [options={}] (Optional) packing options
     * @memberof MaxRectsPacker
     */
    class MaxRectsPacker(
        val width: Int = EDGE_MAX_VALUE,
        val height: Int = EDGE_MAX_VALUE,
        val padding: Int = 0,
        /**
         * The Bin array added to the packer
         *
         * @type {Bin[]}
         * @memberof MaxRectsPacker
         */
        /**
         * Options for MaxRect Packer
         *
         * @property {boolean} options.smart Smart sizing packer (default is true)
         * @property {boolean} options.pot use power of 2 sizing (default is true)
         * @property {boolean} options.square use square size (default is false)
         * @property {boolean} options.allowRotation allow rotation packing (default is false)
         * @property {boolean} options.tag allow auto grouping based on `rect.tag` (default is false)
         * @property {boolean} options.exclusiveTag tagged rects will have dependent bin, if set to `false`, packer will try to put tag rects into the same bin (default is true)
         * @property {boolean} options.border atlas edge spacing (default is 0)
         * @property {PACKING_LOGIC} options.logic MAX_AREA or MAX_EDGE based sorting logic (default is MAX_EDGE)
         * @export
         * @interface Option
         */
        var options: IOption = IOption(
            smart = true,
            pot = true,
            square = false,
            allowRotation = false,
            tag = false,
            exclusiveTag = true,
            border = 0,
            logic = PACKING_LOGIC.MAX_EDGE
        )
    ) {
        var bins = arrayListOf<Bin>()

        /**
         * Add a bin/rectangle object extends Rectangle to packer
         *
         * @template T Generic type extends Rectangle interface
         * @param {T} rect the rect object add to the packer bin
         * @memberof MaxRectsPacker
         */
        fun add(rect: Rectangle): Rectangle {
            if (rect.width > this.width || rect.height > this.height) {
                this.bins.add(OversizedElementBin(rect))
            } else {
                val added = this.bins.drop(this._currentBinIndex).firstOrNull { it.add(rect) != null }
                if (added == null) {
                    val bin = MaxRectsBin(this.width, this.height, this.padding, this.options)
                    val tag = rect.data?.tag ?: rect.tag
                    if (this.options.tag && tag != null) bin.tag = tag
                    bin.add(rect)
                    this.bins.add(bin)
                }
            }
            return rect
        }
        /**
         * Add a bin/rectangle object with data to packer
         *
         * @param {number} width of the input bin/rectangle
         * @param {number} height of the input bin/rectangle
         * @param {*} data custom data object
         * @memberof MaxRectsPacker
         */
        fun add(width: Int, height: Int, data: IBinPackerData?): Any {
            val rect = Rectangle(width, height)
            rect.data = data

            if (rect.width > this.width || rect.height > this.height) {
                this.bins.add(OversizedElementBin(rect))
            } else {
                var added = this.bins.slice(this._currentBinIndex).firstOrNull { it.add(rect) != null }
                if (added == null) {
                    var bin = MaxRectsBin(this.width, this.height, this.padding, this.options)
                    if (this.options.tag && rect.data?.tag != null) bin.tag = rect.data?.tag
                    bin.add(rect)
                    this.bins.add(bin)
                }
            }
            return rect
        }

        /**
         * Add an Array of bins/rectangles to the packer.
         *
         * `Javascript`: Any object has property: { width, height, ... } is accepted.
         *
         * `Typescript`: object shall extends `MaxrectsPacker.Rectangle`.
         *
         * note: object has `hash` property will have more stable packing result
         *
         * @param {Rectangle[]} rects Array of bin/rectangles
         * @memberof MaxRectsPacker
         */
        fun addArray(rects: List<Rectangle>) {
            if (!this.options.tag || this.options.exclusiveTag) {
                // if not using tag or using exclusiveTag, old approach
                for (rect in this.sort(rects, this.options.logic)) {
                    this.add(rect)
                }
            } else {
                // sort rects by tags first
                if (rects.isEmpty()) return
                val rects = rects.toMutableList()
                rects.sortWith(Comparator { a, b ->
                    val aTag = if (a.data?.tag != null) a.data!!.tag else if (a.tag != null) a.tag else null
                    val bTag = if (b.data?.tag != null) b.data!!.tag else if (b.tag != null) b.tag else null
                    if (bTag === null) -1 else if (aTag === null) 1 else if (bTag > aTag) -1 else 1
                })

                // iterate all bins to find the first bin which can place rects with same tag
                //
                var currentTag: String? = null
                var currentIdx: Int = 0
                val targetBin = this.bins.slice(this._currentBinIndex).find { bin ->
                    val testBin = bin.clone()
                    for (i in currentIdx until rects.size) {
                        val rect = rects[i]
                        val tag = when {
                            rect.data != null && rect.data!!.tag != null -> rect.data?.tag
                            rect.tag != null -> rect.tag
                            else -> null
                        }

                        // initialize currentTag
                        if (i == 0) currentTag = tag

                        if (tag != currentTag) {
                            // all current tag memeber tested successfully
                            currentTag = tag
                            // do addArray()
                            for (r in this.sort(rects.slice(currentIdx, i), this.options.logic)) {
                                bin.add(r)
                            }
                            currentIdx = i

                            // recrusively addArray() with remaining rects
                            this.addArray(rects.drop(i))
                            return@find true
                        }

                        // remaining untagged rect will use normal addArray()
                        if (tag == null) {
                            // do addArray()
                            for (r in this.sort(rects.drop(i), this.options.logic)) {
                                this.add(r)
                            }
                            currentIdx = rects.size
                            // end test
                            return@find true
                        }

                        // still in the same tag group
                        if (testBin.add(rect) === null) {
                            // add the rects that could fit into the bins already
                            // do addArray()
                            for (r in this.sort(rects.slice(currentIdx, i), this.options.logic)) {
                                bin.add(r)
                            }
                            currentIdx = i

                            // current bin cannot contain all tag members
                            // procceed to test next bin
                            return@find false
                        }
                    }

                    // all rects tested
                    // do addArray() to the remaining tag group
                    for (r in this.sort(rects.drop(currentIdx), this.options.logic)) bin.add(r)
                    return@find true
                }

                // create a bin if no current bin fit
                if (targetBin == null) {
                    val rect = rects[currentIdx]
                    val bin = MaxRectsBin(this.width, this.height, this.padding, this.options)
                    val tag = rect.data?.tag ?: rect.tag
                    if (this.options.tag && this.options.exclusiveTag && tag != null) bin.tag = tag
                    this.bins.add(bin)
                    // Add the rect to the newly created bin
                    bin.add(rect)
                    currentIdx++
                    this.addArray(rects.drop(currentIdx))
                }
            }
        }

        /**
         * Reset entire packer to initial states, keep settings
         *
         * @memberof MaxRectsPacker
         */
        fun reset(): Unit {
            this.bins = arrayListOf()
            this._currentBinIndex = 0
        }

        /**
         * Repack all elements inside bins
         *
         * @param {boolean} [quick=true] quick repack only dirty bins
         * @returns {void}
         * @memberof MaxRectsPacker
         */
        fun repack(quick: Boolean = true): Unit {
            if (quick) {
                var unpack: MutableList<Rectangle> = arrayListOf()
                for (bin in this.bins) {
                    if (bin.dirty) {
                        var up = bin.repack()
                        if (up != null) unpack.addAll(up)
                    }
                }
                this.addArray(unpack)
                return
            }
            if (!this.dirty) return
            val allRects = this.rects
            this.reset()
            this.addArray(allRects)
        }

        /**
         * Stop adding element to the current bin and return a bin.
         *
         * note: After calling `next()` all elements will no longer added to previous bins.
         *
         * @returns {Bin}
         * @memberof MaxRectsPacker
         */
        fun next(): Int {
            this._currentBinIndex = this.bins.size
            return this._currentBinIndex
        }

        /**
         * Load bins to the packer, overwrite exist bins
         *
         * @param {MaxRectsBin[]} bins MaxRectsBin objects
         * @memberof MaxRectsPacker
         */
        fun load(bins: List<IBin>) {
            for ((index, bin) in bins.withIndex()) {
                if (bin.maxWidth > this.width || bin.maxHeight > this.height) {
                    this.bins.add(OversizedElementBin(bin.width, bin.height, null))
                } else {
                    val newBin = MaxRectsBin(this.width, this.height, this.padding, bin.options)
                    newBin.freeRects.clear()
                    for (r in bin.freeRects) {
                        newBin.freeRects.add(Rectangle(r.width, r.height, r.x, r.y))
                    }
                    newBin.width = bin.width
                    newBin.height = bin.height
                    if (bin.tag != null) newBin.tag = bin.tag
                    this.bins[index] = newBin
                }
            }
        }

        /**
         * Output current bins to save
         *
         * @memberof MaxRectsPacker
         */
        fun save(): List<IBin> {
            val saveBins: MutableList<IBin> = arrayListOf()
            for (bin in this.bins) {
                val saveBin: IBin = MBin(
                    width = bin.width,
                    height = bin.height,
                    maxWidth = bin.maxWidth,
                    maxHeight = bin.maxHeight,
                    freeRects = arrayListOf(),
                    rects = arrayListOf(),
                    options = bin.options,
                    tag = bin.tag
                )
                for (r in bin.freeRects) {
                    saveBin.freeRects.add(
                        Rectangle(
                            x = r.x,
                            y = r.y,
                            width = r.width,
                            height = r.height
                        )
                    )
                }
                saveBins.add(saveBin)
            }
            return saveBins
        }

        /**
         * Sort the given rects based on longest edge or surface area.
         *
         * If rects have the same sort value, will sort by second key `hash` if presented.
         *
         * @private
         * @param {List<Rectangle>} rects
         * @param {PACKING_LOGIC} [logic=PACKING_LOGIC.MAX_EDGE] sorting logic, "area" or "edge"
         * @returns
         * @memberof MaxRectsPacker
         */
        private fun sort(rects: List<Rectangle>, logic: PACKING_LOGIC = PACKING_LOGIC.MAX_EDGE): List<Rectangle> {
            return rects.toList().sortedWith(Comparator { a, b ->
                val result = when {
                    logic === PACKING_LOGIC.MAX_EDGE -> Math.max(b.width, b.height) - Math.max(a.width, a.height)
                    else -> b.width * b.height - a.width * a.height
                }
                if (result == 0 && a.hash != null && b.hash != null) {
                    if (a.hash!! > b.hash!!) -1 else 1
                } else {
                    result
                }
            })
        }

        private var _currentBinIndex: Int = 0
        /**
         * Return current functioning bin index, perior to this wont accept any elements
         *
         * @readonly
         * @type {number}
         * @memberof MaxRectsPacker
         */
        val currentBinIndex: Int get() = this._currentBinIndex

        /**
         * Returns dirty status of all child bins
         *
         * @readonly
         * @type {boolean}
         * @memberof MaxRectsPacker
         */
        val dirty: Boolean get() = this.bins.any { it.dirty }

        /**
         * Return all rectangles in this packer
         *
         * @readonly
         * @type {List<Rectangle>}
         * @memberof MaxRectsPacker
         */
        val rects: List<Rectangle> get() = this.bins.flatMap { it.rects }
    }

    private fun <T> List<T>.slice(index: Int): List<T> = drop(index)
    private fun <T> List<T>.slice(start: Int, end: Int): List<T> = slice(start until end)
}
