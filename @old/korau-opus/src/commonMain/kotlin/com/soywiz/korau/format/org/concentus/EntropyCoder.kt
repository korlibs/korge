/* Copyright (c) 2001-2011 Timothy B. Terriberry
   Ported to Java by Logan Stromberg

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions
   are met:

   - Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

   - Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

   - Neither the name of Internet Society, IETF or IETF Trust, nor the
   names of specific contributors, may be used to endorse or promote
   products derived from this software without specific prior written
   permission.

   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
   ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
   A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
   OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
   EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
   PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
   PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
   LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
   NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
   SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.soywiz.korau.format.org.concentus

import com.soywiz.kmem.*
import com.soywiz.korau.format.org.concentus.internal.*
import com.soywiz.korio.lang.*

/*A range decoder.
This is an entropy decoder based upon \cite{Mar79}, which is itself a
rediscovery of the FIFO arithmetic code introduced by \cite{Pas76}.
It is very similar to arithmetic encoding, except that encoding is done with
digits in any base, instead of with bits, and so it is faster when using
larger bases (i.e.: a byte).
The author claims an average waste of $\frac{1}{2}\log_b(2b)$ bits, where $b$
is the base, longer than the theoretical optimum, but to my knowledge there
is no published justification for this claim.
This only seems true when using near-infinite precision arithmetic so that
the process is carried out with no rounding errors.

An excellent description of implementation details is available at
http://www.arturocampos.com/ac_range.html
A recent work \cite{MNW98} which proposes several changes to arithmetic
encoding for efficiency actually re-discovers many of the principles
behind range encoding, and presents a good theoretical analysis of them.

End of stream is handled by writing out the smallest number of bits that
ensures that the stream will be correctly decoded regardless of the value of
any subsequent bits.
ec_tell() can be used to determine how many bits were needed to decode
all the symbols thus far; other data can be packed in the remaining bits of
the input buffer.
@PHDTHESIS{Pas76,
author="Richard Clark Pasco",
title="Source coding algorithms for fast data compression",
school="Dept. of Electrical Engineering, Stanford University",
address="Stanford, CA",
month=May,
year=1976
}
@INPROCEEDINGS{Mar79,
author="Martin, G.N.N.",
title="Range encoding: an algorithm for removing redundancy from a digitised
message",
booktitle="Video & Data Recording Conference",
year=1979,
address="Southampton",
month=Jul
}
@ARTICLE{MNW98,
author="Alistair Moffat and Radford Neal and Ian H. Witten",
title="Arithmetic Coding Revisited",
journal="{ACM} Transactions on Information Systems",
year=1998,
volume=16,
number=3,
pages="256--294",
month=Jul,
URL="http://www.stanford.edu/class/ee398a/handouts/papers/Moffat98ArithmCoding.pdf"
}*/
internal class EntropyCoder {

    private val EC_WINDOW_SIZE = 32

    ///*The number of bits to use for the range-coded part of uint integers.*/
    private val EC_UINT_BITS = 8

    /*The number of bits to output at a time.*/
    private val EC_SYM_BITS = 8

    /*The total number of bits in each of the state registers.*/
    private val EC_CODE_BITS = 32

    /*The maximum symbol value.*/
    private val EC_SYM_MAX: Long = 0x000000FF

    /*Bits to shift by to move a symbol into the high-order position.*/
    private val EC_CODE_SHIFT = 0x00000017

    /*Carry bit of the high-order range symbol.*/
    private val EC_CODE_TOP = 0x80000000L

    /*Low-order bit of the high-order range symbol.*/
    private val EC_CODE_BOT: Long = 0x00800000

    /*The number of bits available for the last, partial symbol in the code field.*/
    private val EC_CODE_EXTRA = 0x00000007

    //////////////// Coder State ////////////////////

    /*POINTER to Buffered input/output.*/
    private var buf: ByteArray? = null
    private var buf_ptr: Int = 0

    /*The size of the buffer.*/
    var storage: Int = 0

    /*The offset at which the last byte containing raw bits was read/written.*/
    var end_offs: Int = 0

    /*Bits that will be read from/written at the end.*/
    var end_window: Long = 0

    /*Number of valid bits in end_window.*/
    var nend_bits: Int = 0

    /*The total number of whole bits read/written.
      This does not include partial bits currently in the range coder.*/
    var nbits_total: Int = 0

    /*The offset at which the next range coder byte will be read/written.*/
    var offs: Int = 0

    /*The number of values in the current range.*/
    var rng: Long = 0

    /*In the decoder: the difference between the top of the current range and
       the input value, minus one.
      In the encoder: the low end of the current range.*/
    var `val`: Long = 0

    /*In the decoder: the saved normalization factor from ec_decode().
      In the encoder: the number of oustanding carry propagating symbols.*/
    var ext: Long = 0

    /*A buffered input/output symbol, awaiting carry propagation.*/
    var rem: Int = 0

    /*Nonzero if an error occurred.*/
    var _error: Int = 0

    val _buffer: ByteArray
        get() {
            val convertedBuf = ByteArray(this.storage)
			arraycopy(this.buf!!, this.buf_ptr, convertedBuf, 0, this.storage)
            return convertedBuf
        }

    init {
        Reset()
    }

    fun Reset() {
        buf = null
        buf_ptr = 0
        storage = 0
        end_offs = 0
        end_window = 0
        nend_bits = 0
        offs = 0
        rng = 0
        `val` = 0
        ext = 0
        rem = 0
        _error = 0
    }

    fun Assign(other: EntropyCoder) {
        this.buf = other.buf
        this.buf_ptr = other.buf_ptr
        this.storage = other.storage
        this.end_offs = other.end_offs
        this.end_window = other.end_window
        this.nend_bits = other.nend_bits
        this.nbits_total = other.nbits_total
        this.offs = other.offs
        this.rng = other.rng
        this.`val` = other.`val`
        this.ext = other.ext
        this.rem = other.rem
        this._error = other._error
    }

    fun write_buffer(data: ByteArray, data_ptr: Int, target_offset: Int, size: Int) {
        arraycopy(data, data_ptr, this.buf!!, this.buf_ptr + target_offset, size)
    }

    fun read_byte(): Int {
        return if (this.offs < this.storage) Inlines.SignedByteToUnsignedInt(this.buf!![buf_ptr + this.offs++]) else 0
    }

    fun read_byte_from_end(): Int {
        return if (this.end_offs < this.storage)
            Inlines.SignedByteToUnsignedInt(this.buf!![buf_ptr + (this.storage - ++this.end_offs)])
        else
            0
    }

    fun write_byte(_value: Long): Int {
        if (this.offs + this.end_offs >= this.storage) {
            return -1
        }
        this.buf!![buf_ptr + this.offs++] = (_value and 0xFF).toByte()
        return 0
    }

    fun write_byte_at_end(_value: Long): Int {
        if (this.offs + this.end_offs >= this.storage) {
            return -1
        }

        this.buf!![buf_ptr + (this.storage - ++this.end_offs)] = (_value and 0xFF).toByte()
        return 0
    }

    /// <summary>
    /// Normalizes the contents of val and rng so that rng lies entirely in the high-order symbol.
    /// </summary>
    /// <param name="this"></param>
    fun dec_normalize() {
        /*If the range is too small, rescale it and input some bits.*/
        while (this.rng <= EC_CODE_BOT) {
            var sym: Int
            this.nbits_total += EC_SYM_BITS
            this.rng = Inlines.CapToUInt32(this.rng shl EC_SYM_BITS)

            /*Use up the remaining bits from our last symbol.*/
            sym = this.rem

            /*Read the next value from the input.*/
            this.rem = read_byte()

            /*Take the rest of the bits we need from this new symbol.*/
            sym = sym shl EC_SYM_BITS or this.rem shr EC_SYM_BITS - EC_CODE_EXTRA

            /*And subtract them from val, capped to be less than EC_CODE_TOP.*/
            this.`val` =
                    Inlines.CapToUInt32((this.`val` shl EC_SYM_BITS) + (EC_SYM_MAX and sym.inv().toLong()) and EC_CODE_TOP - 1)
        }
    }

    fun dec_init(_buf: ByteArray, _buf_ptr: Int, _storage: Int) {
        this.buf = _buf
        this.buf_ptr = _buf_ptr
        this.storage = _storage
        this.end_offs = 0
        this.end_window = 0
        this.nend_bits = 0
        /*This is the offset from which ec_tell() will subtract partial bits.
          The final value after the ec_dec_normalize() call will be the same as in
           the encoder, but we have to compensate for the bits that are added there.*/
        this.nbits_total = EC_CODE_BITS + 1 - (EC_CODE_BITS - EC_CODE_EXTRA) / EC_SYM_BITS * EC_SYM_BITS
        this.offs = 0
        this.rng = (1 shl EC_CODE_EXTRA).toLong()
        this.rem = read_byte()
        this.`val` = Inlines.CapToUInt32(this.rng - 1 - (this.rem shr EC_SYM_BITS - EC_CODE_EXTRA).toLong())
        this._error = 0
        /*Normalize the interval.*/
        dec_normalize()
    }

    fun decode(_ft: Long): Long {
        var _ft = _ft
        _ft = Inlines.CapToUInt32(_ft)
        this.ext = Inlines.CapToUInt32(this.rng / _ft)
        val s = Inlines.CapToUInt32(this.`val` / this.ext)
        return Inlines.CapToUInt32(_ft - Inlines.EC_MINI(Inlines.CapToUInt32(s + 1), _ft))
    }

    fun decode_bin(_bits: Int): Long {
        this.ext = this.rng shr _bits
        val s = Inlines.CapToUInt32(this.`val` / this.ext)
        return Inlines.CapToUInt32(
            Inlines.CapToUInt32(1L shl _bits) - Inlines.EC_MINI(
                Inlines.CapToUInt32(s + 1),
                1L shl _bits
            )
        )
    }

    fun dec_update(_fl: Long, _fh: Long, _ft: Long) {
        var _fl = _fl
        var _fh = _fh
        var _ft = _ft
        _fl = Inlines.CapToUInt32(_fl)
        _fh = Inlines.CapToUInt32(_fh)
        _ft = Inlines.CapToUInt32(_ft)
        val s = Inlines.CapToUInt32(this.ext * (_ft - _fh))
        this.`val` = this.`val` - s
        this.rng = if (_fl > 0) Inlines.CapToUInt32(this.ext * (_fh - _fl)) else this.rng - s
        dec_normalize()
    }

    /// <summary>
    /// The probability of having a "one" is 1/(1<<_logp).
    /// </summary>
    /// <param name="this"></param>
    /// <param name="_logp"></param>
    /// <returns></returns>
    fun dec_bit_logp(_logp: Long): Int {
        val r: Long
        val d: Long
        val s: Long
        val ret: Int
        r = this.rng
        d = this.`val`
        s = r shr _logp.toInt()
        ret = if (d < s) 1 else 0
        if (ret == 0) {
            this.`val` = Inlines.CapToUInt32(d - s)
        }
        this.rng = if (ret != 0) s else r - s
        dec_normalize()
        return ret
    }

    fun dec_icdf(_icdf: ShortArray, _ftb: Int): Int {
        var t: Long
        var s = this.rng
        val d = this.`val`
        val r = s shr _ftb
        var ret = -1
        do {
            t = s
            s = Inlines.CapToUInt32(r * _icdf[++ret])
        } while (d < s)
        this.`val` = Inlines.CapToUInt32(d - s)
        this.rng = Inlines.CapToUInt32(t - s)
        dec_normalize()
        return ret
    }

    fun dec_icdf(_icdf: ShortArray, _icdf_offset: Int, _ftb: Int): Int {
        var t: Long
        var s = this.rng
        val d = this.`val`
        val r = s shr _ftb
        var ret = _icdf_offset - 1
        do {
            t = s
            s = Inlines.CapToUInt32(r * _icdf[++ret])
        } while (d < s)
        this.`val` = Inlines.CapToUInt32(d - s)
        this.rng = Inlines.CapToUInt32(t - s)
        dec_normalize()
        return ret - _icdf_offset
    }

    fun dec_uint(_ft: Long): Long {
        var _ft = _ft
        _ft = Inlines.CapToUInt32(_ft)
        val ft: Long
        val s: Long
        var ftb: Int
        /*In order to optimize EC_ILOG(), it is undefined for the value 0.*/
        Inlines.OpusAssert(_ft > 1)
        _ft--
        ftb = Inlines.EC_ILOG(_ft)
        if (ftb > EC_UINT_BITS) {
            val t: Long
            ftb -= EC_UINT_BITS
            ft = Inlines.CapToUInt32((_ft shr ftb) + 1)
            s = Inlines.CapToUInt32(decode(ft))
            dec_update(s, s + 1, ft)
            t = Inlines.CapToUInt32(s shl ftb or dec_bits(ftb).toLong())
            if (t <= _ft) {
                return t
            }
            this._error = 1
            return _ft
        } else {
            _ft++
            s = Inlines.CapToUInt32(decode(_ft))
            dec_update(s, s + 1, _ft)
            return s
        }
    }

    fun dec_bits(_bits: Int): Int {
        var window: Long
        var available: Int
        val ret: Int
        window = this.end_window
        available = this.nend_bits
        if (available < _bits) {
            do {
                window = Inlines.CapToUInt32(window or ((read_byte_from_end() shl available).toLong()))
                available += EC_SYM_BITS
            } while (available <= EC_WINDOW_SIZE - EC_SYM_BITS)
        }
        ret = -0x1 and ((window and ((1 shl _bits) - 1).toLong()).toInt())
        window = window shr _bits
        available = available - _bits
        this.end_window = Inlines.CapToUInt32(window)
        this.nend_bits = available
        this.nbits_total = this.nbits_total + _bits
        return ret
    }

    /// <summary>
    /// Outputs a symbol, with a carry bit.
    /// If there is a potential to propagate a carry over several symbols, they are
    /// buffered until it can be determined whether or not an actual carry will
    /// occur.
    /// If the counter for the buffered symbols overflows, then the stream becomes
    /// undecodable.
    /// This gives a theoretical limit of a few billion symbols in a single packet on
    /// 32-bit systems.
    /// The alternative is to truncate the range in order to force a carry, but
    /// requires similar carry tracking in the decoder, needlessly slowing it down.
    /// </summary>
    /// <param name="this"></param>
    /// <param name="_c"></param>
    fun enc_carry_out(_c: Int) {
        if (_c.toLong() != EC_SYM_MAX) {
            /*No further carry propagation possible, flush buffer.*/
            val carry: Int
            carry = _c shr EC_SYM_BITS

            /*Don't output a byte on the first write.
              This compare should be taken care of by branch-prediction thereafter.*/
            if (this.rem >= 0) {
                this._error = this._error or write_byte(Inlines.CapToUInt32(this.rem + carry))
            }

            if (this.ext > 0) {
                val sym: Long
                sym = EC_SYM_MAX + carry and EC_SYM_MAX
                do {
                    this._error = this._error or write_byte(sym)
                } while (--this.ext > 0)
            }

            this.rem = _c and EC_SYM_MAX.toInt()
        } else {
            this.ext++
        }
    }

    fun enc_normalize() {
        /*If the range is too small, output some bits and rescale it.*/
        while (this.rng <= EC_CODE_BOT) {
            enc_carry_out((this.`val` shr EC_CODE_SHIFT).toInt())
            /*Move the next-to-high-order symbol into the high-order position.*/
            this.`val` = Inlines.CapToUInt32(this.`val` shl EC_SYM_BITS and EC_CODE_TOP - 1)
            this.rng = Inlines.CapToUInt32(this.rng shl EC_SYM_BITS)
            this.nbits_total += EC_SYM_BITS
        }
    }

    fun enc_init(_buf: ByteArray, buf_ptr: Int, _size: Int) {
        this.buf = _buf
        this.buf_ptr = buf_ptr
        this.end_offs = 0
        this.end_window = 0
        this.nend_bits = 0
        /*This is the offset from which ec_tell() will subtract partial bits.*/
        this.nbits_total = EC_CODE_BITS + 1
        this.offs = 0
        this.rng = Inlines.CapToUInt32(EC_CODE_TOP)
        this.rem = -1
        this.`val` = 0
        this.ext = 0
        this.storage = _size
        this._error = 0
    }

    fun encode(_fl: Long, _fh: Long, _ft: Long) {
        var _fl = _fl
        var _fh = _fh
        var _ft = _ft
        _fl = Inlines.CapToUInt32(_fl)
        _fh = Inlines.CapToUInt32(_fh)
        _ft = Inlines.CapToUInt32(_ft)
        val r = Inlines.CapToUInt32(this.rng / _ft)
        if (_fl > 0) {
            this.`val` += Inlines.CapToUInt32(this.rng - r * (_ft - _fl))
            this.rng = Inlines.CapToUInt32(r * (_fh - _fl))
        } else {
            this.rng = Inlines.CapToUInt32(this.rng - r * (_ft - _fh))
        }

        enc_normalize()
    }

    fun encode_bin(_fl: Long, _fh: Long, _bits: Int) {
        var _fl = _fl
        var _fh = _fh
        _fl = Inlines.CapToUInt32(_fl)
        _fh = Inlines.CapToUInt32(_fh)
        val r = Inlines.CapToUInt32(this.rng shr _bits)
        if (_fl > 0) {
            this.`val` = Inlines.CapToUInt32(this.`val` + Inlines.CapToUInt32(this.rng - r * ((1 shl _bits) - _fl)))
            this.rng = Inlines.CapToUInt32(r * (_fh - _fl))
        } else {
            this.rng = Inlines.CapToUInt32(this.rng - r * ((1 shl _bits) - _fh))
        }

        enc_normalize()
    }

    /*The probability of having a "one" is 1/(1<<_logp).*/
    fun enc_bit_logp(_val: Int, _logp: Int) {
        var r = this.rng
        val l = this.`val`
        val s = r shr _logp
        r -= s
        if (_val != 0) {
            this.`val` = Inlines.CapToUInt32(l + r)
        }

        this.rng = if (_val != 0) s else r
        enc_normalize()
    }

    fun enc_icdf(_s: Int, _icdf: ShortArray, _ftb: Int) {
        val r = Inlines.CapToUInt32(this.rng shr _ftb)
        if (_s > 0) {
            this.`val` = this.`val` + Inlines.CapToUInt32(this.rng - Inlines.CapToUInt32(r * _icdf[_s - 1]))
            this.rng = r * Inlines.CapToUInt32(_icdf[_s - 1] - _icdf[_s])
        } else {
            this.rng = Inlines.CapToUInt32(this.rng - r * _icdf[_s])
        }
        enc_normalize()
    }

    fun enc_icdf(_s: Int, _icdf: ShortArray, icdf_ptr: Int, _ftb: Int) {
        val r = Inlines.CapToUInt32(this.rng shr _ftb)
        if (_s > 0) {
            this.`val` = this.`val` + Inlines.CapToUInt32(this.rng - Inlines.CapToUInt32(r * _icdf[icdf_ptr + _s - 1]))
            this.rng = Inlines.CapToUInt32(r * Inlines.CapToUInt32(_icdf[icdf_ptr + _s - 1] - _icdf[icdf_ptr + _s]))
        } else {
            this.rng = Inlines.CapToUInt32(this.rng - r * _icdf[icdf_ptr + _s])
        }
        enc_normalize()
    }

    fun enc_uint(_fl: Long, _ft: Long) {
        var _fl = _fl
        var _ft = _ft
        _fl = Inlines.CapToUInt32(_fl)
        _ft = Inlines.CapToUInt32(_ft)

        val ft: Long
        val fl: Long
        var ftb: Int
        /*In order to optimize EC_ILOG(), it is undefined for the value 0.*/
        Inlines.OpusAssert(_ft > 1)
        _ft--
        ftb = Inlines.EC_ILOG(_ft)
        if (ftb > EC_UINT_BITS) {
            ftb -= EC_UINT_BITS
            ft = Inlines.CapToUInt32((_ft shr ftb) + 1)
            fl = Inlines.CapToUInt32(_fl shr ftb)
            encode(fl, fl + 1, ft)
            enc_bits(_fl and Inlines.CapToUInt32((1 shl ftb) - 1), ftb)
        } else {
            encode(_fl, _fl + 1, _ft + 1)
        }
    }

    fun enc_bits(_fl: Long, _bits: Int) {
        var _fl = _fl
        _fl = Inlines.CapToUInt32(_fl)
        var window: Long
        var used: Int
        window = this.end_window
        used = this.nend_bits
        Inlines.OpusAssert(_bits > 0)

        if (used + _bits > EC_WINDOW_SIZE) {
            do {
                this._error = this._error or write_byte_at_end(window and EC_SYM_MAX)
                window = window shr EC_SYM_BITS.toLong().toInt()
                used -= EC_SYM_BITS
            } while (used >= EC_SYM_BITS)
        }

        window = window or Inlines.CapToUInt32(_fl shl used)
        used += _bits
        this.end_window = window
        this.nend_bits = used
        this.nbits_total += _bits
    }

    fun enc_patch_initial_bits(_val: Long, _nbits: Int) {
        val shift: Int
        val mask: Long
        Inlines.OpusAssert(_nbits <= EC_SYM_BITS)
        shift = EC_SYM_BITS - _nbits
        mask = ((1 shl _nbits) - 1 shl shift).toLong()

        if (this.offs > 0) {
            /*The first byte has been finalized.*/
            this.buf!![buf_ptr] = (this.buf!![buf_ptr] and mask.inv() or Inlines.CapToUInt32(_val shl shift)).toByte()
        } else if (this.rem >= 0) {
            /*The first byte is still awaiting carry propagation.*/
            this.rem = Inlines.CapToUInt32(Inlines.CapToUInt32(this.rem and mask.inv().toInt() or _val.toInt()) shl shift).toInt()
        } else if (this.rng <= EC_CODE_TOP shr _nbits) {
            /*The renormalization loop has never been run.*/
            this.`val` = Inlines.CapToUInt32(
                this.`val` and (mask shl EC_CODE_SHIFT).inv() or Inlines.CapToUInt32(
                    Inlines.CapToUInt32(_val) shl EC_CODE_SHIFT + shift
                )
            )
        } else {
            /*The encoder hasn't even encoded _nbits of data yet.*/
            this._error = -1
        }
    }

    fun enc_shrink(_size: Int) {
        Inlines.OpusAssert(this.offs + this.end_offs <= _size)
        Arrays.MemMove(this.buf!!, buf_ptr + _size - this.end_offs, buf_ptr + this.storage - this.end_offs, this.end_offs)
        this.storage = _size
    }

    fun range_bytes(): Int {
        return this.offs
    }

    /// <summary>
    /// Returns the number of bits "used" by the encoded or decoded symbols so far.
    /// This same number can be computed in either the encoder or the decoder, and is
    /// suitable for making coding decisions.
    /// This will always be slightly larger than the exact value (e.g., all
    /// rounding error is in the positive direction).
    /// </summary>
    /// <param name="this"></param>
    /// <returns>The number of bits.</returns>
    fun tell(): Int {
        return nbits_total - Inlines.EC_ILOG(rng)
    }

    /// <summary>
    /// This is a faster version of ec_tell_frac() that takes advantage
    /// of the low(1/8 bit) resolution to use just a linear function
    /// followed by a lookup to determine the exact transition thresholds.
    /// </summary>
    /// <param name="this"></param>
    /// <returns></returns>
    fun tell_frac(): Int {
        val nbits: Int
        val r: Int
        var l: Int
        var b: Long
        nbits = this.nbits_total shl BITRES
        l = Inlines.EC_ILOG(this.rng)
        r = (this.rng shr l - 16).toInt()
        b = Inlines.CapToUInt32((r shr 12) - 8)
        b = Inlines.CapToUInt32(b + if (r > correction[b.toInt()]) 1 else 0)
        l = ((l shl 3) + b).toInt()
        return nbits - l
    }

    fun enc_done() {
        var window: Long
        var used: Int
        var msk: Long
        var end: Long
        var l: Int
        /*We output the minimum number of bits that ensures that the symbols encoded
           thus far will be decoded correctly regardless of the bits that follow.*/
        l = EC_CODE_BITS - Inlines.EC_ILOG(this.rng)
        msk = Inlines.CapToUInt32((EC_CODE_TOP - 1).ushr(l))
        end = Inlines.CapToUInt32(Inlines.CapToUInt32(this.`val` + msk) and msk.inv())

        if (end or msk >= this.`val` + this.rng) {
            l++
            msk = msk shr 1
            end = Inlines.CapToUInt32(Inlines.CapToUInt32(this.`val` + msk) and msk.inv())
        }

        while (l > 0) {
            enc_carry_out((end shr EC_CODE_SHIFT).toInt())
            end = Inlines.CapToUInt32(end shl EC_SYM_BITS and EC_CODE_TOP - 1)
            l -= EC_SYM_BITS
        }

        /*If we have a buffered byte flush it into the output buffer.*/
        if (this.rem >= 0 || this.ext > 0) {
            enc_carry_out(0)
        }

        /*If we have buffered extra bits, flush them as well.*/
        window = this.end_window
        used = this.nend_bits

        while (used >= EC_SYM_BITS) {
            this._error = this._error or write_byte_at_end(window and EC_SYM_MAX)
            window = window shr EC_SYM_BITS
            used -= EC_SYM_BITS
        }

        /*Clear any excess space and add any remaining extra bits to the last byte.*/
        if (this._error == 0) {
            Arrays.MemSetWithOffset(this.buf!!, 0.toByte(), buf_ptr + this.offs, this.storage - this.offs - this.end_offs)
            if (used > 0) {
                /*If there's no range coder data at all, give up.*/
                if (this.end_offs >= this.storage) {
                    this._error = -1
                } else {
                    l = -l
                    /*If we've busted, don't add too many extra bits to the last byte; it
                       would corrupt the range coder data, and that's more important.*/
                    if (this.offs + this.end_offs >= this.storage && l < used) {
                        window = Inlines.CapToUInt32(window and ((1 shl l) - 1).toLong())
                        this._error = -1
                    }

                    val z = buf_ptr + this.storage - this.end_offs - 1
                    this.buf!![z] = (this.buf!![z] or (window and 0xFF).toByte().toInt()).toByte()
                }
            }
        }
    }

    companion object {

        ///*The resolution of fractional-precision bit usage measurements, i.e.,
        //   3 => 1/8th bits.*/
        val BITRES = 3

        private val correction = intArrayOf(35733, 38967, 42495, 46340, 50535, 55109, 60097, 65535)
    }
}
