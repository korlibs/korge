package korlibs.image.format

import korlibs.time.milliseconds
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.color.RgbaArray
import korlibs.image.color.arraycopy
import korlibs.io.stream.SyncStream
import korlibs.io.stream.readBytes
import korlibs.io.stream.readBytesExact
import korlibs.io.stream.readU8
import korlibs.encoding.hex
import korlibs.util.format
import kotlin.math.max
import kotlin.math.min

object GIF : ImageFormat("gif") {
    override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? {
        try {
            val gif = GifDec.gd_open_gif(s.clone())
            return ImageInfo().also {
                it.bitsPerPixel = 32
                it.width = gif.width
                it.height = gif.height
                GifDec.gd_close_gif(gif)
            }
        } catch (e: Throwable) {
            //e.printStackTrace()
            return null
        }
    }

    override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
        val gif = GifDec.gd_open_gif(s.clone())
        val frames = arrayListOf<ImageFrame>()
        while (GifDec.gd_get_frame(gif) >= 1) {
            val out = Bitmap32(gif.width, gif.height, premultiplied = false)
            out.bitmapName = "gif${frames.size}"
            val time = (max(gif.gce.delay, 1) * 10).milliseconds
            try {
                GifDec.gd_render_frame(gif, RgbaArray(out.ints))
                frames.add(ImageFrame(out, time = time, main = (frames.size == 0)))
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        return ImageData(frames, gif.loop_count)
    }

    override fun writeImage(image: ImageData, s: SyncStream, props: ImageEncodingProps) {
        super.writeImage(image, s, props)
    }

    override fun toString(): String {
        return super.toString()
    }
}

// https://github.com/lecram/gifdec/blob/ea1ed06c03dda22a042f1bdc187fedd6cd608e1d/gifdec.c
// https://github.com/lecram/gifdec/compare/ea1ed06c03dda22a042f1bdc187fedd6cd608e1d..1dcbae19363597314f6623010cc80abad4e47f7c
@OptIn(ExperimentalUnsignedTypes::class)
object GifDec {
    class gd_Palette(
        var size: Int = 0,
        val colors: RgbaArray = RgbaArray(0x100) // @TODO: Convert into RgbaArray once working
    )

    // _GifGraphicsControlExtension
    class gd_GCE(
        var delay: Int = 0, // Hundredths of seconds to wait
        var tindex: Int = 0,
        var disposal: Int = 0,
        var input: Int = 0,
        var transparency: Boolean = false
    )

    class gd_GIF(
        var fd: SyncStream,
        var anim_start: Long = 0L,
        var width: Int = 0,
        var height: Int = 0,
        var depth: Int = 0,
        var loop_count: Int = 0,
        var gce: gd_GCE = gd_GCE(),
        var palette: gd_Palette = gd_Palette(),
        var lct: gd_Palette = gd_Palette(),
        var gct: gd_Palette = gd_Palette(),
        var plain_text: ((gif: gd_GIF, tx: Int, ty: Int, tw: Int, th: Int, cw: Int, ch: Int, fg: Int, bg: Int) -> Unit)? = null,
        var comment: ((gif: gd_GIF) -> Unit) ? = null,
        var application: ((gif: gd_GIF, id: ByteArray, auth: ByteArray) -> Unit)? = null,
        var fx: Int = 0,
        var fy: Int = 0,
        var fw: Int = 0,
        var fh: Int = 0,
        var bgindex: Int = 0,
        var canvas: RgbaArray = RgbaArray(0),
        var frame: UByteArray = UByteArray(0)
    )

    class Entry(
        var length: Int = 0,
        var prefix: Int = 0,
        var suffix: Int = 0
    )

    class Table(
        var bulk: Int = 0,
        var nentries: Int = 0,
        var entries: Array<Entry> = emptyArray()
    ) {
        fun resize(count: Int) {
            bulk = count
            entries = Array(count) { entries.getOrNull(it) ?: Entry() }
        }
    }

    fun read_num(fd: SyncStream): Int {
        val a = fd.readU8()
        val b = fd.readU8()
        return a or (b shl 8)
    }

    fun ByteArray.eqbytes(str: String): Boolean {
        for (n in str.indices) if (this[n] != str[n].toByte()) return false
        return true
    }

    fun gd_open_gif(fd: SyncStream): gd_GIF {
        /* Header */
        val sigver1 = fd.readBytes(3)

        if (!sigver1.eqbytes("GIF")) error("invalid signature: ${sigver1.hex}");
        /* Version */
        val sigver2 = fd.readBytes(3)
        if (!sigver2.eqbytes("89a")) error("invalid version: ${sigver2.hex}")
        /* Width x Height */
        val width = read_num(fd);
        val height = read_num(fd);
        /* FDSZ */
        val fdsz = fd.readU8()
        /* Presence of GCT */
        if ((fdsz and 0x80) == 0) {
            error("no global color table")
        }
        /* Color Space's Depth */
        val depth = ((fdsz ushr 4) and 7) + 1;
        /* Ignore Sort Flag. */
        /* GCT Size */
        val gct_sz = 1 shl ((fdsz and 0x07) + 1);
        /* Background Color Index */
        val bgidx = fd.readU8()
        /* Aspect Ratio */
        val aspect = fd.readU8()
        /* Create gd_GIF Structure. */
        val gif = gd_GIF(fd, canvas = RgbaArray(width * height), frame = UByteArray(width * height))
        gif.fd = fd;
        gif.width = width;
        gif.height = height;
        gif.depth = depth;
        /* Read GCT */
        gif.gct.size = gct_sz;
        for (n in 0 until gif.gct.size) {
            val r = fd.readU8()
            val g = fd.readU8()
            val b = fd.readU8()
            gif.gct.colors[n] = RGBA(r, g, b)
        }
        gif.palette = gif.gct;
        gif.bgindex = bgidx;
        if (gif.bgindex != 0) {
            gif.frame.fill(gif.bgindex.toUByte(), 0, gif.width * gif.height)
        }
        gif.palette.colors[gif.bgindex] = Colors.TRANSPARENT
        //val bgcolor = gif.palette.colors[gif.bgindex]
        //if (bgcolor.r != 0 || bgcolor.g != 0 || bgcolor.b != 0) {
        //    gif.canvas.fill(bgcolor, gif.width * gif.height)
        //}
        gif.anim_start = lseek(fd, 0, SEEK_CUR);
        return gif;
    }

    fun discard_sub_blocks(gif: gd_GIF )
    {
        do {
            val size = gif.fd.readU8()
            lseek(gif.fd, size.toLong(), SEEK_CUR);
        } while (size != 0);
    }

    fun read_plain_text_ext(gif: gd_GIF) {
        val plain_text = gif.plain_text
        if (plain_text != null) {
            lseek(gif.fd, 1, SEEK_CUR); /* block size = 12 */
            val tx = read_num(gif.fd);
            val ty = read_num(gif.fd);
            val tw = read_num(gif.fd);
            val th = read_num(gif.fd);
            val cw = gif.fd.readU8()
            val ch = gif.fd.readU8()
            val fg = gif.fd.readU8()
            val bg = gif.fd.readU8()
            val sub_block = lseek(gif.fd, 0, SEEK_CUR);
            plain_text.invoke(gif, tx, ty, tw, th, cw, ch, fg, bg);
            lseek(gif.fd, sub_block, SEEK_SET);
        } else {
            /* Discard plain text metadata. */
            lseek(gif.fd, 13, SEEK_CUR);
        }
        /* Discard plain text sub-blocks. */
        discard_sub_blocks(gif);
    }

    fun read_graphic_control_ext(gif: gd_GIF) {
        /* Discard block size (always 0x04). */
        lseek(gif.fd, 1, SEEK_CUR);
        val rdit = gif.fd.readU8()
        gif.gce.disposal = (rdit ushr 2) and 3;
        gif.gce.input = rdit and 2;
        gif.gce.transparency = (rdit and 1) != 0;
        gif.gce.delay = read_num(gif.fd);
        gif.gce.tindex = gif.fd.readU8()
        /* Skip block terminator. */
        lseek(gif.fd, 1, SEEK_CUR);
    }

    fun read_comment_ext(gif: gd_GIF) {
        val comment = gif.comment
        if (comment != null) {
            val sub_block = lseek(gif.fd, 0, SEEK_CUR);
            comment(gif);
            lseek(gif.fd, sub_block, SEEK_SET);
        }
        /* Discard comment sub-blocks. */
        discard_sub_blocks(gif);
    }

    fun read_application_ext(gif: gd_GIF) {

        /* Discard block size (always 0x0B). */
        lseek(gif.fd, 1, SEEK_CUR);
        /* Application Identifier. */
        val app_id = gif.fd.readBytesExact(8)
        /* Application Authentication Code. */
        val app_auth_code = gif.fd.readBytesExact(3)
        if (app_id.eqbytes("NETSCAPE")) {
            /* Discard block size (0x03) and constant byte (0x01). */
            lseek(gif.fd, 2, SEEK_CUR);
            gif.loop_count = read_num(gif.fd);
            /* Skip block terminator. */
            lseek(gif.fd, 1, SEEK_CUR);
        } else if (gif.application != null) {
            val sub_block = lseek(gif.fd, 0, SEEK_CUR);
            gif.application!!.invoke(gif, app_id, app_auth_code);
            lseek(gif.fd, sub_block, SEEK_SET);
            discard_sub_blocks(gif);
        } else {
            discard_sub_blocks(gif);
        }
    }

    fun read_ext(gif: gd_GIF) {
        val label = gif.fd.readU8()

        when (label) {
            0x01 -> read_plain_text_ext(gif);
            0xF9 -> read_graphic_control_ext(gif);
            0xFE -> read_comment_ext(gif);
            0xFF -> read_application_ext(gif);
            else -> error("unknown extension: %02X".format(label))
        }
    }

    fun new_table(key_size: Int): Table {
        val init_bulk = max(1 shl (key_size + 1), 0x100);
        return Table(
            init_bulk,
            (1 shl key_size) + 2,
            Array(init_bulk) { Entry(1, 0xFFF, it) }
        )
    }

    /* Add table entry. Return value:
     *  0 on success
     *  +1 if key size must be incremented after this addition
     *  -1 if could not realloc table */
    fun add_entry(tablep: Table, length: Int, prefix: Int, suffix: Int): Int {
        var table = tablep
        if (table.nentries == table.bulk) {
            table.resize(table.bulk * 2)
        }
        table.entries[table.nentries] = Entry(length = length, prefix = prefix, suffix = suffix)
        table.nentries++;
        if ((table.nentries and (table.nentries - 1)) == 0)
        return 1;
        return 0;
    }

    data class KeyState(
        var sub_len: Int = 0,
        var shift: Int = 0,
        var byte: Int = 0
    )

    fun get_key(gif: gd_GIF, key_size: Int, s: KeyState): Int {
        var key = 0;
        var bits_read = 0
        while (bits_read < key_size) {
            val rpad = (s.shift.toInt() + bits_read) % 8;
            if (rpad == 0) {
                /* Update byte. */
                if (s.sub_len == 0) {
                    s.sub_len = gif.fd.readU8() /* Must be nonzero! */
                    if (s.sub_len == 0) {
                        return 0x1000
                    }
                }
                s.byte = gif.fd.readU8()
                s.sub_len--
            }
            val frag_size = min(key_size - bits_read, 8 - rpad);
            key = key or (( 0xFFFF and ((s.byte) ushr rpad)) shl bits_read)
            bits_read += frag_size
        }
        /* Clear extra bits to the left. */
        key = key and ((1 shl key_size) - 1);
        s.shift = (s.shift.toInt() + key_size) % 8
        return key;
    }

    /* Compute output index of y-th input line, in frame of height h. */
    fun interlaced_line_index(h: Int, y: Int): Int {
        var y = y
        var p = (h - 1) / 8 + 1;
        if (y < p) return y * 8; /* pass 1 */
        y -= p;
        p = (h - 5) / 8 + 1;
        if (y < p) return y * 8 + 4; /* pass 2 */
        y -= p;
        p = (h - 3) / 4 + 1;
        if (y < p) return y * 4 + 2; /* pass 3 */
        y -= p;
        /* pass 4 */
        return y * 2 + 1;
    }

    /* Decompress image pixels.
     * Return 0 on success or -1 on out-of-memory (w.r.t. LZW code table). */
    fun read_image_data(gif: gd_GIF, interlace: Boolean): Int {
        var str_len: Int = 0
        var table_is_full: Boolean = false
        val byte = gif.fd.readU8()
        var key_size = byte.toInt()
        if (key_size < 2 || key_size > 8) return -1
        val start = lseek(gif.fd, 0, SEEK_CUR);
        discard_sub_blocks(gif);
        val end = lseek(gif.fd, 0, SEEK_CUR);
        lseek(gif.fd, start, SEEK_SET);
        val clear = 1 shl key_size;
        val stop = clear + 1;
        val table = new_table(key_size)
        key_size++;
        var init_key_size = key_size;
        val state = KeyState()
        lateinit var entry: Entry
        state.byte = byte
        var key = get_key(gif, key_size, state); /* clear code */
        var frm_off = 0;
        var ret = 0;
        val frm_size = gif.fw*gif.fh;
        while (frm_off < frm_size) {
            if (key == clear) {
                key_size = init_key_size;
                table.nentries = (1 shl (key_size - 1)) + 2;
                table_is_full = false
            } else if (!table_is_full) {
                ret = add_entry(table, str_len + 1, key, entry.suffix);
                if (ret == -1) {
                    free(table);
                    return -1;
                }
                if (table.nentries == 0x1000) {
                    ret = 0;
                    table_is_full = true
                }
            }
            key = get_key(gif, key_size, state);
            if (key == clear) continue
            if (key == stop || key == 0x1000) break
            if (ret == 1) key_size++;
            entry = table.entries[key.toInt()];
            str_len = entry.length;
            for (i in 0 until str_len) {
                val p = frm_off + entry.length - 1;
                val x = p % gif.fw;
                var y = p / gif.fw;
                if (interlace) {
                    y = interlaced_line_index(gif.fh, y);
                }
                gif.frame[(gif.fy + y) * gif.width + gif.fx + x] = entry.suffix.toUByte()
                if (entry.prefix == 0xFFF) {
                    break;
                } else {
                    entry = table.entries[entry.prefix];
                }
            }
            frm_off += str_len;
            if (key.toInt() < table.nentries - 1 && !table_is_full)
                table.entries[table.nentries - 1].suffix = entry.suffix;
        }
        free(table);
        if (key == stop) {
            state.sub_len = gif.fd.readU8()  /* Must be zero! */
        }
        lseek(gif.fd, end, SEEK_SET);
        return 0;
    }

    /* Read image.
     * Return 0 on success or -1 on out-of-memory (w.r.t. LZW code table). */
    fun read_image(gif: gd_GIF): Int {
        /* Image Descriptor. */
        gif.fx = read_num(gif.fd);
        gif.fy = read_num(gif.fd);

        if (gif.fx >= gif.width || gif.fy >= gif.height) {
            return -1
        }
        gif.fw = read_num(gif.fd);
        gif.fh = read_num(gif.fd);

        gif.fw = min(gif.fw, gif.width - gif.fx);
        gif.fh = min(gif.fh, gif.height - gif.fy);

        val fisrz = gif.fd.readU8()
        val interlace = (fisrz and 0x40) != 0
        /* Ignore Sort Flag. */
        /* Local Color Table? */
        if ((fisrz and 0x80) != 0) {
            /* Read LCT */
            gif.lct.size = 1 shl ((fisrz and 0x07) + 1);
            for (n in 0 until gif.lct.size) {
                val r = gif.fd.readU8()
                val g = gif.fd.readU8()
                val b = gif.fd.readU8()
                gif.lct.colors[n] = RGBA(r, g, b)
            }
            gif.palette = gif.lct;
        } else {
            gif.palette = gif.gct;
        }
        /* Image Data. */
        return read_image_data(gif, interlace);
    }

    fun render_frame_rect(gif: gd_GIF, buffer: RgbaArray) {
        var i = gif.fy * gif.width + gif.fx;
        for (j in 0 until gif.fh) {
            for (k in 0 until gif.fw) {
                val index = gif.frame[(gif.fy + j) * gif.width + gif.fx + k].toInt()
                val color = gif.palette.colors[index];
                if (!gif.gce.transparency || index != gif.gce.tindex) {
                    buffer[i+k] = color
                }
            }
            i += gif.width;
        }
    }

    fun dispose(gif: gd_GIF) {
        when (gif.gce.disposal) {
            2 -> { /* Restore to background color. */
                //val bgcolor = gif.palette.colors[gif.bgindex]
                //gif.palette.colors[gif.bgindex] = Colors.TRANSPARENT_BLACK
                var i = gif.fy * gif.width+gif.fx;
                for (j in 0 until gif.fh) {
                    for (k in 0 until gif.fw) {
                        gif.canvas[i + k] = Colors.TRANSPARENT
                    }
                    i += gif.width
                }
            }
            3 -> { /* Restore to previous, i.e., don't update canvas.*/
            }
            else -> {
                /* Add frame non-transparent pixels to canvas. */
                render_frame_rect(gif, gif.canvas);
            }
        }
    }

    /* Return 1 if got a frame; 0 if got GIF trailer; -1 if error. */
    fun gd_get_frame(gif: gd_GIF): Int {
        var sep: Char

        dispose(gif);
        sep = readChar(gif.fd)
        while (sep != ',') {
            if (sep == ';')
                return 0;
            if (sep == '!')
                read_ext(gif);
            else return -1;
            sep = readChar(gif.fd)
        }
        if (read_image(gif) == -1)
            return -1;
        return 1;
    }

    fun gd_render_frame(gif: gd_GIF, buffer: RgbaArray) {
        arraycopy(gif.canvas, 0, buffer, 0, gif.width * gif.height)
        render_frame_rect(gif, buffer);
    }

    fun gd_is_bgcolor(gif: gd_GIF, color: RGBA): Boolean {
        return gif.palette.colors[gif.bgindex] == color
    }

    fun gd_rewind(gif: gd_GIF) {
        lseek(gif.fd, gif.anim_start.toLong(), SEEK_SET);
    }

    fun gd_close_gif(gif: gd_GIF) {
        close(gif.fd);
        free(gif);
    }

    val SEEK_SET = 0
    val SEEK_CUR = 1

    fun close(fd: SyncStream) {
        fd.close()
    }

    fun free(obj: Any?) {
    }

    fun readChar(fd: SyncStream): Char {
        return fd.readU8().toChar()
    }

    fun lseek(fd: SyncStream, pos: Long, kind: Int): Long {
        when (kind) {
            SEEK_SET -> fd.position = pos.toLong()
            SEEK_CUR -> fd.position += pos.toLong()
        }
        return fd.position
    }
}
