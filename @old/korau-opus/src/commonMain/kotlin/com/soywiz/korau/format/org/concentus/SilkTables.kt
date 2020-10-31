/* Copyright (c) 2006-2011 Skype Limited. All Rights Reserved
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

internal object SilkTables {

 /// <summary>
    /// Cosine approximation table for LSF conversion
    /// Q12 values (even)
    /// </summary>
     val silk_LSFCosTab_Q12 = shortArrayOf(8192, 8190, 8182, 8170, 8152, 8130, 8104, 8072, 8034, 7994, 7946, 7896, 7840, 7778, 7714, 7644, 7568, 7490, 7406, 7318, 7226, 7128, 7026, 6922, 6812, 6698, 6580, 6458, 6332, 6204, 6070, 5934, 5792, 5648, 5502, 5352, 5198, 5040, 4880, 4718, 4552, 4382, 4212, 4038, 3862, 3684, 3502, 3320, 3136, 2948, 2760, 2570, 2378, 2186, 1990, 1794, 1598, 1400, 1202, 1002, 802, 602, 402, 202, 0, -202, -402, -602, -802, -1002, -1202, -1400, -1598, -1794, -1990, -2186, -2378, -2570, -2760, -2948, -3136, -3320, -3502, -3684, -3862, -4038, -4212, -4382, -4552, -4718, -4880, -5040, -5198, -5352, -5502, -5648, -5792, -5934, -6070, -6204, -6332, -6458, -6580, -6698, -6812, -6922, -7026, -7128, -7226, -7318, -7406, -7490, -7568, -7644, -7714, -7778, -7840, -7896, -7946, -7994, -8034, -8072, -8104, -8130, -8152, -8170, -8182, -8190, -8192)

 val silk_gain_iCDF = arrayOf(shortArrayOf(224, 112, 44, 15, 3, 2, 1, 0), shortArrayOf(254, 237, 192, 132, 70, 23, 4, 0), shortArrayOf(255, 252, 226, 155, 61, 11, 2, 0))

 val silk_delta_gain_iCDF = shortArrayOf(250, 245, 234, 203, 71, 50, 42, 38, 35, 33, 31, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0)

 val silk_LTP_per_index_iCDF = shortArrayOf(179, 99, 0)

 val silk_LTP_gain_iCDF_0 = shortArrayOf(71, 56, 43, 30, 21, 12, 6, 0)

 val silk_LTP_gain_iCDF_1 = shortArrayOf(199, 165, 144, 124, 109, 96, 84, 71, 61, 51, 42, 32, 23, 15, 8, 0)

 val silk_LTP_gain_iCDF_2 = shortArrayOf(241, 225, 211, 199, 187, 175, 164, 153, 142, 132, 123, 114, 105, 96, 88, 80, 72, 64, 57, 50, 44, 38, 33, 29, 24, 20, 16, 12, 9, 5, 2, 0)

 val silk_LTP_gain_middle_avg_RD_Q14:Short = 12304

 val silk_LTP_gain_BITS_Q5_0 = shortArrayOf(15, 131, 138, 138, 155, 155, 173, 173)

 val silk_LTP_gain_BITS_Q5_1 = shortArrayOf(69, 93, 115, 118, 131, 138, 141, 138, 150, 150, 155, 150, 155, 160, 166, 160)

 val silk_LTP_gain_BITS_Q5_2 = shortArrayOf(131, 128, 134, 141, 141, 141, 145, 145, 145, 150, 155, 155, 155, 155, 160, 160, 160, 160, 166, 166, 173, 173, 182, 192, 182, 192, 192, 192, 205, 192, 205, 224)

 val silk_LTP_gain_iCDF_ptrs = arrayOf(silk_LTP_gain_iCDF_0, silk_LTP_gain_iCDF_1, silk_LTP_gain_iCDF_2)

 val silk_LTP_gain_BITS_Q5_ptrs = arrayOf(silk_LTP_gain_BITS_Q5_0, silk_LTP_gain_BITS_Q5_1, silk_LTP_gain_BITS_Q5_2)

 val silk_LTP_gain_vq_0 = arrayOf(byteArrayOf(4, 6, 24, 7, 5), byteArrayOf(0, 0, 2, 0, 0), byteArrayOf(12, 28, 41, 13, -4), byteArrayOf(-9, 15, 42, 25, 14), byteArrayOf(1, -2, 62, 41, -9), byteArrayOf(-10, 37, 65, -4, 3), byteArrayOf(-6, 4, 66, 7, -8), byteArrayOf(16, 14, 38, -3, 33))

 val silk_LTP_gain_vq_1 = arrayOf(byteArrayOf(13, 22, 39, 23, 12), byteArrayOf(-1, 36, 64, 27, -6), byteArrayOf(-7, 10, 55, 43, 17), byteArrayOf(1, 1, 8, 1, 1), byteArrayOf(6, -11, 74, 53, -9), byteArrayOf(-12, 55, 76, -12, 8), byteArrayOf(-3, 3, 93, 27, -4), byteArrayOf(26, 39, 59, 3, -8), byteArrayOf(2, 0, 77, 11, 9), byteArrayOf(-8, 22, 44, -6, 7), byteArrayOf(40, 9, 26, 3, 9), byteArrayOf(-7, 20, 101, -7, 4), byteArrayOf(3, -8, 42, 26, 0), byteArrayOf(-15, 33, 68, 2, 23), byteArrayOf(-2, 55, 46, -2, 15), byteArrayOf(3, -1, 21, 16, 41))

 val silk_LTP_gain_vq_2 = arrayOf(byteArrayOf(-6, 27, 61, 39, 5), byteArrayOf(-11, 42, 88, 4, 1), byteArrayOf(-2, 60, 65, 6, -4), byteArrayOf(-1, -5, 73, 56, 1), byteArrayOf(-9, 19, 94, 29, -9), byteArrayOf(0, 12, 99, 6, 4), byteArrayOf(8, -19, 102, 46, -13), byteArrayOf(3, 2, 13, 3, 2), byteArrayOf(9, -21, 84, 72, -18), byteArrayOf(-11, 46, 104, -22, 8), byteArrayOf(18, 38, 48, 23, 0), byteArrayOf(-16, 70, 83, -21, 11), byteArrayOf(5, -11, 117, 22, -8), byteArrayOf(-6, 23, 117, -12, 3), byteArrayOf(3, -8, 95, 28, 4), byteArrayOf(-10, 15, 77, 60, -15), byteArrayOf(-1, 4, 124, 2, -4), byteArrayOf(3, 38, 84, 24, -25), byteArrayOf(2, 13, 42, 13, 31), byteArrayOf(21, -4, 56, 46, -1), byteArrayOf(-1, 35, 79, -13, 19), byteArrayOf(-7, 65, 88, -9, -14), byteArrayOf(20, 4, 81, 49, -29), byteArrayOf(20, 0, 75, 3, -17), byteArrayOf(5, -9, 44, 92, -8), byteArrayOf(1, -3, 22, 69, 31), byteArrayOf(-6, 95, 41, -12, 5), byteArrayOf(39, 67, 16, -4, 1), byteArrayOf(0, -6, 120, 55, -36), byteArrayOf(-13, 44, 122, 4, -24), byteArrayOf(81, 5, 11, 3, 7), byteArrayOf(2, 0, 9, 10, 88))

 val silk_LTP_vq_ptrs_Q7 = arrayOf(silk_LTP_gain_vq_0, silk_LTP_gain_vq_1, silk_LTP_gain_vq_2)

 /* Maximum frequency-dependent response of the pitch taps above,
       computed as max(abs(freqz(taps))) */
     val silk_LTP_gain_vq_0_gain = shortArrayOf(46, 2, 90, 87, 93, 91, 82, 98)

 val silk_LTP_gain_vq_1_gain = shortArrayOf(109, 120, 118, 12, 113, 115, 117, 119, 99, 59, 87, 111, 63, 111, 112, 80)

 val silk_LTP_gain_vq_2_gain = shortArrayOf(126, 124, 125, 124, 129, 121, 126, 23, 132, 127, 127, 127, 126, 127, 122, 133, 130, 134, 101, 118, 119, 145, 126, 86, 124, 120, 123, 119, 170, 173, 107, 109)

 val silk_LTP_vq_gain_ptrs_Q7 = arrayOf(silk_LTP_gain_vq_0_gain, silk_LTP_gain_vq_1_gain, silk_LTP_gain_vq_2_gain)

 val silk_LTP_vq_sizes = byteArrayOf(8, 16, 32)

 val silk_NLSF_CB1_NB_MB_Q8 = shortArrayOf(12, 35, 60, 83, 108, 132, 157, 180, 206, 228, 15, 32, 55, 77, 101, 125, 151, 175, 201, 225, 19, 42, 66, 89, 114, 137, 162, 184, 209, 230, 12, 25, 50, 72, 97, 120, 147, 172, 200, 223, 26, 44, 69, 90, 114, 135, 159, 180, 205, 225, 13, 22, 53, 80, 106, 130, 156, 180, 205, 228, 15, 25, 44, 64, 90, 115, 142, 168, 196, 222, 19, 24, 62, 82, 100, 120, 145, 168, 190, 214, 22, 31, 50, 79, 103, 120, 151, 170, 203, 227, 21, 29, 45, 65, 106, 124, 150, 171, 196, 224, 30, 49, 75, 97, 121, 142, 165, 186, 209, 229, 19, 25, 52, 70, 93, 116, 143, 166, 192, 219, 26, 34, 62, 75, 97, 118, 145, 167, 194, 217, 25, 33, 56, 70, 91, 113, 143, 165, 196, 223, 21, 34, 51, 72, 97, 117, 145, 171, 196, 222, 20, 29, 50, 67, 90, 117, 144, 168, 197, 221, 22, 31, 48, 66, 95, 117, 146, 168, 196, 222, 24, 33, 51, 77, 116, 134, 158, 180, 200, 224, 21, 28, 70, 87, 106, 124, 149, 170, 194, 217, 26, 33, 53, 64, 83, 117, 152, 173, 204, 225, 27, 34, 65, 95, 108, 129, 155, 174, 210, 225, 20, 26, 72, 99, 113, 131, 154, 176, 200, 219, 34, 43, 61, 78, 93, 114, 155, 177, 205, 229, 23, 29, 54, 97, 124, 138, 163, 179, 209, 229, 30, 38, 56, 89, 118, 129, 158, 178, 200, 231, 21, 29, 49, 63, 85, 111, 142, 163, 193, 222, 27, 48, 77, 103, 133, 158, 179, 196, 215, 232, 29, 47, 74, 99, 124, 151, 176, 198, 220, 237, 33, 42, 61, 76, 93, 121, 155, 174, 207, 225, 29, 53, 87, 112, 136, 154, 170, 188, 208, 227, 24, 30, 52, 84, 131, 150, 166, 186, 203, 229, 37, 48, 64, 84, 104, 118, 156, 177, 201, 230)

 val silk_NLSF_CB1_iCDF_NB_MB = shortArrayOf(212, 178, 148, 129, 108, 96, 85, 82, 79, 77, 61, 59, 57, 56, 51, 49, 48, 45, 42, 41, 40, 38, 36, 34, 31, 30, 21, 12, 10, 3, 1, 0, 255, 245, 244, 236, 233, 225, 217, 203, 190, 176, 175, 161, 149, 136, 125, 114, 102, 91, 81, 71, 60, 52, 43, 35, 28, 20, 19, 18, 12, 11, 5, 0)

 val silk_NLSF_CB2_SELECT_NB_MB = shortArrayOf(16, 0, 0, 0, 0, 99, 66, 36, 36, 34, 36, 34, 34, 34, 34, 83, 69, 36, 52, 34, 116, 102, 70, 68, 68, 176, 102, 68, 68, 34, 65, 85, 68, 84, 36, 116, 141, 152, 139, 170, 132, 187, 184, 216, 137, 132, 249, 168, 185, 139, 104, 102, 100, 68, 68, 178, 218, 185, 185, 170, 244, 216, 187, 187, 170, 244, 187, 187, 219, 138, 103, 155, 184, 185, 137, 116, 183, 155, 152, 136, 132, 217, 184, 184, 170, 164, 217, 171, 155, 139, 244, 169, 184, 185, 170, 164, 216, 223, 218, 138, 214, 143, 188, 218, 168, 244, 141, 136, 155, 170, 168, 138, 220, 219, 139, 164, 219, 202, 216, 137, 168, 186, 246, 185, 139, 116, 185, 219, 185, 138, 100, 100, 134, 100, 102, 34, 68, 68, 100, 68, 168, 203, 221, 218, 168, 167, 154, 136, 104, 70, 164, 246, 171, 137, 139, 137, 155, 218, 219, 139)

 val silk_NLSF_CB2_iCDF_NB_MB = shortArrayOf(255, 254, 253, 238, 14, 3, 2, 1, 0, 255, 254, 252, 218, 35, 3, 2, 1, 0, 255, 254, 250, 208, 59, 4, 2, 1, 0, 255, 254, 246, 194, 71, 10, 2, 1, 0, 255, 252, 236, 183, 82, 8, 2, 1, 0, 255, 252, 235, 180, 90, 17, 2, 1, 0, 255, 248, 224, 171, 97, 30, 4, 1, 0, 255, 254, 236, 173, 95, 37, 7, 1, 0)

 val silk_NLSF_CB2_BITS_NB_MB_Q5 = shortArrayOf(255, 255, 255, 131, 6, 145, 255, 255, 255, 255, 255, 236, 93, 15, 96, 255, 255, 255, 255, 255, 194, 83, 25, 71, 221, 255, 255, 255, 255, 162, 73, 34, 66, 162, 255, 255, 255, 210, 126, 73, 43, 57, 173, 255, 255, 255, 201, 125, 71, 48, 58, 130, 255, 255, 255, 166, 110, 73, 57, 62, 104, 210, 255, 255, 251, 123, 65, 55, 68, 100, 171, 255)

 val silk_NLSF_PRED_NB_MB_Q8 = shortArrayOf(179, 138, 140, 148, 151, 149, 153, 151, 163, 116, 67, 82, 59, 92, 72, 100, 89, 92)

 val silk_NLSF_DELTA_MIN_NB_MB_Q15 = shortArrayOf(250, 3, 6, 3, 3, 3, 4, 3, 3, 3, 461)

 val silk_NLSF_CB_NB_MB = NLSFCodebook()

 val silk_NLSF_CB1_WB_Q8 = shortArrayOf(7, 23, 38, 54, 69, 85, 100, 116, 131, 147, 162, 178, 193, 208, 223, 239, 13, 25, 41, 55, 69, 83, 98, 112, 127, 142, 157, 171, 187, 203, 220, 236, 15, 21, 34, 51, 61, 78, 92, 106, 126, 136, 152, 167, 185, 205, 225, 240, 10, 21, 36, 50, 63, 79, 95, 110, 126, 141, 157, 173, 189, 205, 221, 237, 17, 20, 37, 51, 59, 78, 89, 107, 123, 134, 150, 164, 184, 205, 224, 240, 10, 15, 32, 51, 67, 81, 96, 112, 129, 142, 158, 173, 189, 204, 220, 236, 8, 21, 37, 51, 65, 79, 98, 113, 126, 138, 155, 168, 179, 192, 209, 218, 12, 15, 34, 55, 63, 78, 87, 108, 118, 131, 148, 167, 185, 203, 219, 236, 16, 19, 32, 36, 56, 79, 91, 108, 118, 136, 154, 171, 186, 204, 220, 237, 11, 28, 43, 58, 74, 89, 105, 120, 135, 150, 165, 180, 196, 211, 226, 241, 6, 16, 33, 46, 60, 75, 92, 107, 123, 137, 156, 169, 185, 199, 214, 225, 11, 19, 30, 44, 57, 74, 89, 105, 121, 135, 152, 169, 186, 202, 218, 234, 12, 19, 29, 46, 57, 71, 88, 100, 120, 132, 148, 165, 182, 199, 216, 233, 17, 23, 35, 46, 56, 77, 92, 106, 123, 134, 152, 167, 185, 204, 222, 237, 14, 17, 45, 53, 63, 75, 89, 107, 115, 132, 151, 171, 188, 206, 221, 240, 9, 16, 29, 40, 56, 71, 88, 103, 119, 137, 154, 171, 189, 205, 222, 237, 16, 19, 36, 48, 57, 76, 87, 105, 118, 132, 150, 167, 185, 202, 218, 236, 12, 17, 29, 54, 71, 81, 94, 104, 126, 136, 149, 164, 182, 201, 221, 237, 15, 28, 47, 62, 79, 97, 115, 129, 142, 155, 168, 180, 194, 208, 223, 238, 8, 14, 30, 45, 62, 78, 94, 111, 127, 143, 159, 175, 192, 207, 223, 239, 17, 30, 49, 62, 79, 92, 107, 119, 132, 145, 160, 174, 190, 204, 220, 235, 14, 19, 36, 45, 61, 76, 91, 108, 121, 138, 154, 172, 189, 205, 222, 238, 12, 18, 31, 45, 60, 76, 91, 107, 123, 138, 154, 171, 187, 204, 221, 236, 13, 17, 31, 43, 53, 70, 83, 103, 114, 131, 149, 167, 185, 203, 220, 237, 17, 22, 35, 42, 58, 78, 93, 110, 125, 139, 155, 170, 188, 206, 224, 240, 8, 15, 34, 50, 67, 83, 99, 115, 131, 146, 162, 178, 193, 209, 224, 239, 13, 16, 41, 66, 73, 86, 95, 111, 128, 137, 150, 163, 183, 206, 225, 241, 17, 25, 37, 52, 63, 75, 92, 102, 119, 132, 144, 160, 175, 191, 212, 231, 19, 31, 49, 65, 83, 100, 117, 133, 147, 161, 174, 187, 200, 213, 227, 242, 18, 31, 52, 68, 88, 103, 117, 126, 138, 149, 163, 177, 192, 207, 223, 239, 16, 29, 47, 61, 76, 90, 106, 119, 133, 147, 161, 176, 193, 209, 224, 240, 15, 21, 35, 50, 61, 73, 86, 97, 110, 119, 129, 141, 175, 198, 218, 237)

 val silk_NLSF_CB1_iCDF_WB = shortArrayOf(225, 204, 201, 184, 183, 175, 158, 154, 153, 135, 119, 115, 113, 110, 109, 99, 98, 95, 79, 68, 52, 50, 48, 45, 43, 32, 31, 27, 18, 10, 3, 0, 255, 251, 235, 230, 212, 201, 196, 182, 167, 166, 163, 151, 138, 124, 110, 104, 90, 78, 76, 70, 69, 57, 45, 34, 24, 21, 11, 6, 5, 4, 3, 0)

 val silk_NLSF_CB2_SELECT_WB = shortArrayOf(0, 0, 0, 0, 0, 0, 0, 1, 100, 102, 102, 68, 68, 36, 34, 96, 164, 107, 158, 185, 180, 185, 139, 102, 64, 66, 36, 34, 34, 0, 1, 32, 208, 139, 141, 191, 152, 185, 155, 104, 96, 171, 104, 166, 102, 102, 102, 132, 1, 0, 0, 0, 0, 16, 16, 0, 80, 109, 78, 107, 185, 139, 103, 101, 208, 212, 141, 139, 173, 153, 123, 103, 36, 0, 0, 0, 0, 0, 0, 1, 48, 0, 0, 0, 0, 0, 0, 32, 68, 135, 123, 119, 119, 103, 69, 98, 68, 103, 120, 118, 118, 102, 71, 98, 134, 136, 157, 184, 182, 153, 139, 134, 208, 168, 248, 75, 189, 143, 121, 107, 32, 49, 34, 34, 34, 0, 17, 2, 210, 235, 139, 123, 185, 137, 105, 134, 98, 135, 104, 182, 100, 183, 171, 134, 100, 70, 68, 70, 66, 66, 34, 131, 64, 166, 102, 68, 36, 2, 1, 0, 134, 166, 102, 68, 34, 34, 66, 132, 212, 246, 158, 139, 107, 107, 87, 102, 100, 219, 125, 122, 137, 118, 103, 132, 114, 135, 137, 105, 171, 106, 50, 34, 164, 214, 141, 143, 185, 151, 121, 103, 192, 34, 0, 0, 0, 0, 0, 1, 208, 109, 74, 187, 134, 249, 159, 137, 102, 110, 154, 118, 87, 101, 119, 101, 0, 2, 0, 36, 36, 66, 68, 35, 96, 164, 102, 100, 36, 0, 2, 33, 167, 138, 174, 102, 100, 84, 2, 2, 100, 107, 120, 119, 36, 197, 24, 0)

 val silk_NLSF_CB2_iCDF_WB = shortArrayOf(255, 254, 253, 244, 12, 3, 2, 1, 0, 255, 254, 252, 224, 38, 3, 2, 1, 0, 255, 254, 251, 209, 57, 4, 2, 1, 0, 255, 254, 244, 195, 69, 4, 2, 1, 0, 255, 251, 232, 184, 84, 7, 2, 1, 0, 255, 254, 240, 186, 86, 14, 2, 1, 0, 255, 254, 239, 178, 91, 30, 5, 1, 0, 255, 248, 227, 177, 100, 19, 2, 1, 0)

 val silk_NLSF_CB2_BITS_WB_Q5 = shortArrayOf(255, 255, 255, 156, 4, 154, 255, 255, 255, 255, 255, 227, 102, 15, 92, 255, 255, 255, 255, 255, 213, 83, 24, 72, 236, 255, 255, 255, 255, 150, 76, 33, 63, 214, 255, 255, 255, 190, 121, 77, 43, 55, 185, 255, 255, 255, 245, 137, 71, 43, 59, 139, 255, 255, 255, 255, 131, 66, 50, 66, 107, 194, 255, 255, 166, 116, 76, 55, 53, 125, 255, 255)

 val silk_NLSF_PRED_WB_Q8 = shortArrayOf(175, 148, 160, 176, 178, 173, 174, 164, 177, 174, 196, 182, 198, 192, 182, 68, 62, 66, 60, 72, 117, 85, 90, 118, 136, 151, 142, 160, 142, 155)

 val silk_NLSF_DELTA_MIN_WB_Q15 = shortArrayOf(100, 3, 40, 3, 3, 3, 5, 14, 14, 10, 11, 3, 8, 9, 7, 3, 347)

 val silk_NLSF_CB_WB = NLSFCodebook()

 /* Piece-wise linear mapping from bitrate in kbps to coding quality in dB SNR */
     val silk_TargetRate_table_NB = intArrayOf(0, 8000, 9400, 11500, 13500, 17500, 25000, SilkConstants.MAX_TARGET_RATE_BPS)
 val silk_TargetRate_table_MB = intArrayOf(0, 9000, 12000, 14500, 18500, 24500, 35500, SilkConstants.MAX_TARGET_RATE_BPS)
 val silk_TargetRate_table_WB = intArrayOf(0, 10500, 14000, 17000, 21500, 28500, 42000, SilkConstants.MAX_TARGET_RATE_BPS)
 val silk_SNR_table_Q1 = shortArrayOf(18, 29, 38, 40, 46, 52, 62, 84)

 /* Tables for stereo predictor coding */
     val silk_stereo_pred_quant_Q13 = shortArrayOf(-13732, -10050, -8266, -7526, -6500, -5000, -2950, -820, 820, 2950, 5000, 6500, 7526, 8266, 10050, 13732)

 val silk_stereo_pred_joint_iCDF = shortArrayOf(249, 247, 246, 245, 244, 234, 210, 202, 201, 200, 197, 174, 82, 59, 56, 55, 54, 46, 22, 12, 11, 10, 9, 7, 0)
 val silk_stereo_only_code_mid_iCDF = shortArrayOf(64, 0)

 /* Tables for LBRR flags */
     val silk_LBRR_flags_2_iCDF = shortArrayOf(203, 150, 0)
 val silk_LBRR_flags_3_iCDF = shortArrayOf(215, 195, 166, 125, 110, 82, 0)
 val silk_LBRR_flags_iCDF_ptr = arrayOf(silk_LBRR_flags_2_iCDF, silk_LBRR_flags_3_iCDF)

 /* Table for LSB coding */
     val silk_lsb_iCDF = shortArrayOf(120, 0)

 /* Tables for LTPScale */
     val silk_LTPscale_iCDF = shortArrayOf(128, 64, 0)

 /* Tables for signal type and offset coding */
     val silk_type_offset_VAD_iCDF = shortArrayOf(232, 158, 10, 0)
 val silk_type_offset_no_VAD_iCDF = shortArrayOf(230, 0)

 /* Tables for NLSF interpolation factor */
     val silk_NLSF_interpolation_factor_iCDF = shortArrayOf(243, 221, 192, 181, 0)

 /* Quantization offsets */
     val silk_Quantization_Offsets_Q10 = arrayOf(shortArrayOf(SilkConstants.OFFSET_UVL_Q10, SilkConstants.OFFSET_UVH_Q10), shortArrayOf(SilkConstants.OFFSET_VL_Q10, SilkConstants.OFFSET_VH_Q10))

 /* Table for LTPScale */
     val silk_LTPScales_table_Q14 = shortArrayOf(15565, 12288, 8192)

 /* Uniform entropy tables */
     val silk_uniform3_iCDF = shortArrayOf(171, 85, 0)
 val silk_uniform4_iCDF = shortArrayOf(192, 128, 64, 0)
 val silk_uniform5_iCDF = shortArrayOf(205, 154, 102, 51, 0)
 val silk_uniform6_iCDF = shortArrayOf(213, 171, 128, 85, 43, 0)
 val silk_uniform8_iCDF = shortArrayOf(224, 192, 160, 128, 96, 64, 32, 0)

 val silk_NLSF_EXT_iCDF = shortArrayOf(100, 40, 16, 7, 3, 1, 0)

 /*  Elliptic/Cauer filters designed with 0.1 dB passband ripple,
            80 dB minimum stopband attenuation, and
            [0.95 : 0.15 : 0.35] normalized cut off frequencies. */

 /* Interpolation points for filter coefficients used in the bandwidth transition smoother */
     val silk_Transition_LP_B_Q28 = arrayOf(intArrayOf(250767114, 501534038, 250767114), intArrayOf(209867381, 419732057, 209867381), intArrayOf(170987846, 341967853, 170987846), intArrayOf(131531482, 263046905, 131531482), intArrayOf(89306658, 178584282, 89306658))

 /* Interpolation points for filter coefficients used in the bandwidth transition smoother */
     val silk_Transition_LP_A_Q28 = arrayOf(intArrayOf(506393414, 239854379), intArrayOf(411067935, 169683996), intArrayOf(306733530, 116694253), intArrayOf(185807084, 77959395), intArrayOf(35497197, 57401098))

 val silk_pitch_lag_iCDF = shortArrayOf(253, 250, 244, 233, 212, 182, 150, 131, 120, 110, 98, 85, 72, 60, 49, 40, 32, 25, 19, 15, 13, 11, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0)

 val silk_pitch_delta_iCDF = shortArrayOf(210, 208, 206, 203, 199, 193, 183, 168, 142, 104, 74, 52, 37, 27, 20, 14, 10, 6, 4, 2, 0)

 val silk_pitch_contour_iCDF = shortArrayOf(223, 201, 183, 167, 152, 138, 124, 111, 98, 88, 79, 70, 62, 56, 50, 44, 39, 35, 31, 27, 24, 21, 18, 16, 14, 12, 10, 8, 6, 4, 3, 2, 1, 0)

 val silk_pitch_contour_NB_iCDF = shortArrayOf(188, 176, 155, 138, 119, 97, 67, 43, 26, 10, 0)

 val silk_pitch_contour_10_ms_iCDF = shortArrayOf(165, 119, 80, 61, 47, 35, 27, 20, 14, 9, 4, 0)

 val silk_pitch_contour_10_ms_NB_iCDF = shortArrayOf(113, 63, 0)

 val silk_max_pulses_table = byteArrayOf(8, 10, 12, 16)

 val silk_pulses_per_block_iCDF = arrayOf(shortArrayOf(125, 51, 26, 18, 15, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0), shortArrayOf(198, 105, 45, 22, 15, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0), shortArrayOf(213, 162, 116, 83, 59, 43, 32, 24, 18, 15, 12, 9, 7, 6, 5, 3, 2, 0), shortArrayOf(239, 187, 116, 59, 28, 16, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0), shortArrayOf(250, 229, 188, 135, 86, 51, 30, 19, 13, 10, 8, 6, 5, 4, 3, 2, 1, 0), shortArrayOf(249, 235, 213, 185, 156, 128, 103, 83, 66, 53, 42, 33, 26, 21, 17, 13, 10, 0), shortArrayOf(254, 249, 235, 206, 164, 118, 77, 46, 27, 16, 10, 7, 5, 4, 3, 2, 1, 0), shortArrayOf(255, 253, 249, 239, 220, 191, 156, 119, 85, 57, 37, 23, 15, 10, 6, 4, 2, 0), shortArrayOf(255, 253, 251, 246, 237, 223, 203, 179, 152, 124, 98, 75, 55, 40, 29, 21, 15, 0), shortArrayOf(255, 254, 253, 247, 220, 162, 106, 67, 42, 28, 18, 12, 9, 6, 4, 3, 2, 0))

 val silk_pulses_per_block_BITS_Q5 = arrayOf(shortArrayOf(31, 57, 107, 160, 205, 205, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255), shortArrayOf(69, 47, 67, 111, 166, 205, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255), shortArrayOf(82, 74, 79, 95, 109, 128, 145, 160, 173, 205, 205, 205, 224, 255, 255, 224, 255, 224), shortArrayOf(125, 74, 59, 69, 97, 141, 182, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255), shortArrayOf(173, 115, 85, 73, 76, 92, 115, 145, 173, 205, 224, 224, 255, 255, 255, 255, 255, 255), shortArrayOf(166, 134, 113, 102, 101, 102, 107, 118, 125, 138, 145, 155, 166, 182, 192, 192, 205, 150), shortArrayOf(224, 182, 134, 101, 83, 79, 85, 97, 120, 145, 173, 205, 224, 255, 255, 255, 255, 255), shortArrayOf(255, 224, 192, 150, 120, 101, 92, 89, 93, 102, 118, 134, 160, 182, 192, 224, 224, 224), shortArrayOf(255, 224, 224, 182, 155, 134, 118, 109, 104, 102, 106, 111, 118, 131, 145, 160, 173, 131))

 val silk_rate_levels_iCDF = arrayOf(shortArrayOf(241, 190, 178, 132, 87, 74, 41, 14, 0), shortArrayOf(223, 193, 157, 140, 106, 57, 39, 18, 0))

 val silk_rate_levels_BITS_Q5 = arrayOf(shortArrayOf(131, 74, 141, 79, 80, 138, 95, 104, 134), shortArrayOf(95, 99, 91, 125, 93, 76, 123, 115, 123))

 val silk_shell_code_table0 = shortArrayOf(128, 0, 214, 42, 0, 235, 128, 21, 0, 244, 184, 72, 11, 0, 248, 214, 128, 42, 7, 0, 248, 225, 170, 80, 25, 5, 0, 251, 236, 198, 126, 54, 18, 3, 0, 250, 238, 211, 159, 82, 35, 15, 5, 0, 250, 231, 203, 168, 128, 88, 53, 25, 6, 0, 252, 238, 216, 185, 148, 108, 71, 40, 18, 4, 0, 253, 243, 225, 199, 166, 128, 90, 57, 31, 13, 3, 0, 254, 246, 233, 212, 183, 147, 109, 73, 44, 23, 10, 2, 0, 255, 250, 240, 223, 198, 166, 128, 90, 58, 33, 16, 6, 1, 0, 255, 251, 244, 231, 210, 181, 146, 110, 75, 46, 25, 12, 5, 1, 0, 255, 253, 248, 238, 221, 196, 164, 128, 92, 60, 35, 18, 8, 3, 1, 0, 255, 253, 249, 242, 229, 208, 180, 146, 110, 76, 48, 27, 14, 7, 3, 1, 0)

 val silk_shell_code_table1 = shortArrayOf(129, 0, 207, 50, 0, 236, 129, 20, 0, 245, 185, 72, 10, 0, 249, 213, 129, 42, 6, 0, 250, 226, 169, 87, 27, 4, 0, 251, 233, 194, 130, 62, 20, 4, 0, 250, 236, 207, 160, 99, 47, 17, 3, 0, 255, 240, 217, 182, 131, 81, 41, 11, 1, 0, 255, 254, 233, 201, 159, 107, 61, 20, 2, 1, 0, 255, 249, 233, 206, 170, 128, 86, 50, 23, 7, 1, 0, 255, 250, 238, 217, 186, 148, 108, 70, 39, 18, 6, 1, 0, 255, 252, 243, 226, 200, 166, 128, 90, 56, 30, 13, 4, 1, 0, 255, 252, 245, 231, 209, 180, 146, 110, 76, 47, 25, 11, 4, 1, 0, 255, 253, 248, 237, 219, 194, 163, 128, 93, 62, 37, 19, 8, 3, 1, 0, 255, 254, 250, 241, 226, 205, 177, 145, 111, 79, 51, 30, 15, 6, 2, 1, 0)

 val silk_shell_code_table2 = shortArrayOf(129, 0, 203, 54, 0, 234, 129, 23, 0, 245, 184, 73, 10, 0, 250, 215, 129, 41, 5, 0, 252, 232, 173, 86, 24, 3, 0, 253, 240, 200, 129, 56, 15, 2, 0, 253, 244, 217, 164, 94, 38, 10, 1, 0, 253, 245, 226, 189, 132, 71, 27, 7, 1, 0, 253, 246, 231, 203, 159, 105, 56, 23, 6, 1, 0, 255, 248, 235, 213, 179, 133, 85, 47, 19, 5, 1, 0, 255, 254, 243, 221, 194, 159, 117, 70, 37, 12, 2, 1, 0, 255, 254, 248, 234, 208, 171, 128, 85, 48, 22, 8, 2, 1, 0, 255, 254, 250, 240, 220, 189, 149, 107, 67, 36, 16, 6, 2, 1, 0, 255, 254, 251, 243, 227, 201, 166, 128, 90, 55, 29, 13, 5, 2, 1, 0, 255, 254, 252, 246, 234, 213, 183, 147, 109, 73, 43, 22, 10, 4, 2, 1, 0)

 val silk_shell_code_table3 = shortArrayOf(130, 0, 200, 58, 0, 231, 130, 26, 0, 244, 184, 76, 12, 0, 249, 214, 130, 43, 6, 0, 252, 232, 173, 87, 24, 3, 0, 253, 241, 203, 131, 56, 14, 2, 0, 254, 246, 221, 167, 94, 35, 8, 1, 0, 254, 249, 232, 193, 130, 65, 23, 5, 1, 0, 255, 251, 239, 211, 162, 99, 45, 15, 4, 1, 0, 255, 251, 243, 223, 186, 131, 74, 33, 11, 3, 1, 0, 255, 252, 245, 230, 202, 158, 105, 57, 24, 8, 2, 1, 0, 255, 253, 247, 235, 214, 179, 132, 84, 44, 19, 7, 2, 1, 0, 255, 254, 250, 240, 223, 196, 159, 112, 69, 36, 15, 6, 2, 1, 0, 255, 254, 253, 245, 231, 209, 176, 136, 93, 55, 27, 11, 3, 2, 1, 0, 255, 254, 253, 252, 239, 221, 194, 158, 117, 76, 42, 18, 4, 3, 2, 1, 0)

 val silk_shell_code_table_offsets = shortArrayOf(0, 0, 2, 5, 9, 14, 20, 27, 35, 44, 54, 65, 77, 90, 104, 119, 135)

 val silk_sign_iCDF = shortArrayOf(254, 49, 67, 77, 82, 93, 99, 198, 11, 18, 24, 31, 36, 45, 255, 46, 66, 78, 87, 94, 104, 208, 14, 21, 32, 42, 51, 66, 255, 94, 104, 109, 112, 115, 118, 248, 53, 69, 80, 88, 95, 102)

 // Resampler tables

    /* Tables with delay compensation values to equalize total delay for different modes */
     val delay_matrix_enc = arrayOf(
 /* in  \ out  8  12  16 */
        /*  8 */byteArrayOf(6, 0, 3),
 /* 12 */ byteArrayOf(0, 7, 3),
 /* 16 */ byteArrayOf(0, 1, 10),
 /* 24 */ byteArrayOf(0, 2, 6),
 /* 48 */ byteArrayOf(18, 10, 12))

 val delay_matrix_dec = arrayOf(
 /* in  \ out  8  12  16  24  48 */
        /*  8 */byteArrayOf(4, 0, 2, 0, 0),
 /* 12 */ byteArrayOf(0, 9, 4, 7, 4),
 /* 16 */ byteArrayOf(0, 3, 12, 7, 7))


 /* Tables with IIR and FIR coefficients for fractional downsamplers (123 Words) */
     val silk_Resampler_3_4_COEFS/*[2 + 3 * RESAMPLER_DOWN_ORDER_FIR0 / 2]*/ = shortArrayOf(-20694, -13867, -49, 64, 17, -157, 353, -496, 163, 11047, 22205, -39, 6, 91, -170, 186, 23, -896, 6336, 19928, -19, -36, 102, -89, -24, 328, -951, 2568, 15909)/*silk_DWORD_ALIGN*/

 val silk_Resampler_2_3_COEFS/*[2 + 2 * RESAMPLER_DOWN_ORDER_FIR0 / 2]*/ = shortArrayOf(-14457, -14019, 64, 128, -122, 36, 310, -768, 584, 9267, 17733, 12, 128, 18, -142, 288, -117, -865, 4123, 14459)/*silk_DWORD_ALIGN*/

 val silk_Resampler_1_2_COEFS/*[2 + RESAMPLER_DOWN_ORDER_FIR1 / 2]*/ = shortArrayOf(616, -14323, -10, 39, 58, -46, -84, 120, 184, -315, -541, 1284, 5380, 9024)/*silk_DWORD_ALIGN*/

 val silk_Resampler_1_3_COEFS/*[2 + RESAMPLER_DOWN_ORDER_FIR2 / 2]*/ = shortArrayOf(16102, -15162, -13, 0, 20, 26, 5, -31, -43, -4, 65, 90, 7, -157, -248, -44, 593, 1583, 2612, 3271)/*silk_DWORD_ALIGN*/

 val silk_Resampler_1_4_COEFS/*[2 + RESAMPLER_DOWN_ORDER_FIR2 / 2]*/ = shortArrayOf(22500, -15099, 3, -14, -20, -15, 2, 25, 37, 25, -16, -71, -107, -79, 50, 292, 623, 982, 1288, 1464)/*silk_DWORD_ALIGN*/

 val silk_Resampler_1_6_COEFS/*[2 + RESAMPLER_DOWN_ORDER_FIR2 / 2]*/ = shortArrayOf(27540, -15257, 17, 12, 8, 1, -10, -22, -30, -32, -22, 3, 44, 100, 168, 243, 317, 381, 429, 455)/*silk_DWORD_ALIGN*/

 val silk_Resampler_2_3_COEFS_LQ/*[2 + 2 * 2]*/ = shortArrayOf(-2797, -6507, 4697, 10739, 1567, 8276)/*silk_DWORD_ALIGN*/

 /* Table with interplation fractions of 1/24, 3/24, 5/24, ... , 23/24 : 23/24 (46 Words) */
     val silk_resampler_frac_FIR_12/*[12][RESAMPLER_ORDER_FIR_12 / 2 ]*/ = arrayOf(shortArrayOf(189, -600, 617, 30567), shortArrayOf(117, -159, -1070, 29704), shortArrayOf(52, 221, -2392, 28276), shortArrayOf(-4, 529, -3350, 26341), shortArrayOf(-48, 758, -3956, 23973), shortArrayOf(-80, 905, -4235, 21254), shortArrayOf(-99, 972, -4222, 18278), shortArrayOf(-107, 967, -3957, 15143), shortArrayOf(-103, 896, -3487, 11950), shortArrayOf(-91, 773, -2865, 8798), shortArrayOf(-71, 611, -2143, 5784), shortArrayOf(-46, 425, -1375, 2996))/*silk_DWORD_ALIGN*/

 /* Tables for 2x downsampler */
     val silk_resampler_down2_0:Short = 9872
 val silk_resampler_down2_1 = (39809 - 65536).toShort()

 /* Tables for 2x upsampler, high quality */
     val silk_resampler_up2_hq_0 = shortArrayOf(1746, 14986, (39083 - 65536).toShort())
 val silk_resampler_up2_hq_1 = shortArrayOf(6854, 25769, (55542 - 65536).toShort())

 // from pitch_estimation_tables.c
     val silk_CB_lags_stage2_10_ms = arrayOf(byteArrayOf(0, 1, 0), byteArrayOf(0, 0, 1))

 val silk_CB_lags_stage3_10_ms = arrayOf(byteArrayOf(0, 0, 1, -1, 1, -1, 2, -2, 2, -2, 3, -3), byteArrayOf(0, 1, 0, 1, -1, 2, -1, 2, -2, 3, -2, 3))

 val silk_CB_lags_stage2 = arrayOf(byteArrayOf(0, 2, -1, -1, -1, 0, 0, 1, 1, 0, 1), byteArrayOf(0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0), byteArrayOf(0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0), byteArrayOf(0, -1, 2, 1, 0, 1, 1, 0, 0, -1, -1))/*[ PE_MAX_NB_SUBFR ][ PE_NB_CBKS_STAGE2_EXT ]*/

 val silk_CB_lags_stage3/*[ PE_MAX_NB_SUBFR ][ PE_NB_CBKS_STAGE3_MAX ]*/ = arrayOf(byteArrayOf(0, 0, 1, -1, 0, 1, -1, 0, -1, 1, -2, 2, -2, -2, 2, -3, 2, 3, -3, -4, 3, -4, 4, 4, -5, 5, -6, -5, 6, -7, 6, 5, 8, -9), byteArrayOf(0, 0, 1, 0, 0, 0, 0, 0, 0, 0, -1, 1, 0, 0, 1, -1, 0, 1, -1, -1, 1, -1, 2, 1, -1, 2, -2, -2, 2, -2, 2, 2, 3, -3), byteArrayOf(0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1, -1, 1, 0, 0, 2, 1, -1, 2, -1, -1, 2, -1, 2, 2, -1, 3, -2, -2, -2, 3), byteArrayOf(0, 1, 0, 0, 1, 0, 1, -1, 2, -1, 2, -1, 2, 3, -2, 3, -2, -2, 4, 4, -3, 5, -3, -4, 6, -4, 6, 5, -5, 8, -6, -5, -7, 9))

 val silk_Lag_range_stage3_10_ms = arrayOf(byteArrayOf(-3, 7), byteArrayOf(-2, 7))

 val silk_Lag_range_stage3 = arrayOf(
 /* Lags to search for low number of stage3 cbks */
                arrayOf(byteArrayOf(-5, 8), byteArrayOf(-1, 6), byteArrayOf(-1, 6), byteArrayOf(-4, 10)),
 /* Lags to search for middle number of stage3 cbks */
                arrayOf(byteArrayOf(-6, 10), byteArrayOf(-2, 6), byteArrayOf(-1, 6), byteArrayOf(-5, 10)),
 /* Lags to search for max number of stage3 cbks */
                arrayOf(byteArrayOf(-9, 12), byteArrayOf(-3, 7), byteArrayOf(-2, 7), byteArrayOf(-7, 13)))

 val silk_nb_cbk_searchs_stage3 = byteArrayOf(SilkConstants.PE_NB_CBKS_STAGE3_MIN.toByte(), SilkConstants.PE_NB_CBKS_STAGE3_MID.toByte(), SilkConstants.PE_NB_CBKS_STAGE3_MAX.toByte())

init{
silk_NLSF_CB_NB_MB.nVectors = 32
silk_NLSF_CB_NB_MB.order = 10
silk_NLSF_CB_NB_MB.quantStepSize_Q16 = (0.18f * (1.toLong() shl 16) + 0.5).toInt().toShort()
silk_NLSF_CB_NB_MB.invQuantStepSize_Q6 = (1.0f / 0.18f * (1.toLong() shl 6) + 0.5).toInt().toShort()
silk_NLSF_CB_NB_MB.CB1_NLSF_Q8 = silk_NLSF_CB1_NB_MB_Q8
silk_NLSF_CB_NB_MB.CB1_iCDF = silk_NLSF_CB1_iCDF_NB_MB
silk_NLSF_CB_NB_MB.pred_Q8 = silk_NLSF_PRED_NB_MB_Q8
silk_NLSF_CB_NB_MB.ec_sel = silk_NLSF_CB2_SELECT_NB_MB
silk_NLSF_CB_NB_MB.ec_iCDF = silk_NLSF_CB2_iCDF_NB_MB
silk_NLSF_CB_NB_MB.ec_Rates_Q5 = silk_NLSF_CB2_BITS_NB_MB_Q5
silk_NLSF_CB_NB_MB.deltaMin_Q15 = silk_NLSF_DELTA_MIN_NB_MB_Q15
}

init{
silk_NLSF_CB_WB.nVectors = 32
silk_NLSF_CB_WB.order = 16
silk_NLSF_CB_WB.quantStepSize_Q16 = (0.15f * (1.toLong() shl 16) + 0.5).toInt().toShort()
silk_NLSF_CB_WB.invQuantStepSize_Q6 = (1.0f / 0.15f * (1.toLong() shl 6) + 0.5).toInt().toShort()
silk_NLSF_CB_WB.CB1_NLSF_Q8 = silk_NLSF_CB1_WB_Q8
silk_NLSF_CB_WB.CB1_iCDF = silk_NLSF_CB1_iCDF_WB
silk_NLSF_CB_WB.pred_Q8 = silk_NLSF_PRED_WB_Q8
silk_NLSF_CB_WB.ec_sel = silk_NLSF_CB2_SELECT_WB
silk_NLSF_CB_WB.ec_iCDF = silk_NLSF_CB2_iCDF_WB
silk_NLSF_CB_WB.ec_Rates_Q5 = silk_NLSF_CB2_BITS_WB_Q5
silk_NLSF_CB_WB.deltaMin_Q15 = silk_NLSF_DELTA_MIN_WB_Q15
}
}
