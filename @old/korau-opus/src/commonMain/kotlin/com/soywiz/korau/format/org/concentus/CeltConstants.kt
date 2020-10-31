/* Copyright (c) 2007-2008 CSIRO
   Copyright (c) 2007-2011 Xiph.Org Foundation
   Originally written by Jean-Marc Valin, Gregory Maxwell, Koen Vos,
   Timothy B. Terriberry, and the Opus open-source contributors
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

internal object CeltConstants {

    val Q15ONE = 32767

    val CELT_SIG_SCALE = 32768.0f

    val SIG_SHIFT = 12

    val NORM_SCALING = 16384

    val DB_SHIFT = 10

    val EPSILON = 1
    val VERY_SMALL = 0
    val VERY_LARGE16 = 32767.toShort()
    val Q15_ONE = 32767.toShort()

    val COMBFILTER_MAXPERIOD = 1024
    val COMBFILTER_MINPERIOD = 15

    // from opus_decode.c
    val DECODE_BUFFER_SIZE = 2048

    // from modes.c
    /* Alternate tuning (partially derived from Vorbis) */
    val BITALLOC_SIZE = 11
    val MAX_PERIOD = 1024

    // from static_modes_float.h
    val TOTAL_MODES = 1

    // from rate.h
    val MAX_PSEUDO = 40
    val LOG_MAX_PSEUDO = 6

    val CELT_MAX_PULSES = 128

    val MAX_FINE_BITS = 8

    val FINE_OFFSET = 21
    val QTHETA_OFFSET = 4
    val QTHETA_OFFSET_TWOPHASE = 16

    /* The maximum pitch lag to allow in the pitch-based PLC. It's possible to save
CPU time in the PLC pitch search by making this smaller than MAX_PERIOD. The
current value corresponds to a pitch of 66.67 Hz. */
    val PLC_PITCH_LAG_MAX = 720

    /* The minimum pitch lag to allow in the pitch-based PLC. This corresponds to a
       pitch of 480 Hz. */
    val PLC_PITCH_LAG_MIN = 100

    val LPC_ORDER = 24
}
