package korlibs.io.compression

import korlibs.io.compression.lzo.LZO
import korlibs.io.lang.UTF8
import korlibs.io.lang.toByteArray
import korlibs.io.lang.toString
import korlibs.encoding.fromBase64
import korlibs.encoding.unhex
import korlibs.crypto.md5
import korlibs.crypto.sha1
import korlibs.crypto.sha256
import kotlin.test.Test
import kotlin.test.assertEquals

class LzoTest {
    val REF_TEXT = "HELLO THIS IS A HELLO THIS IS A HELLO WORLD HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO WORLD HELLO WORLD HELLO HELLO HELLO\n"

    @Test
    fun testDecompression() {
        val lzoCompressed = "894C5A4F000D0A1A0A104020A00940030903000001000081A4617DD526000000000968656C6C6F2E747874730E081300000086000000293966230D1C48454C4C4F2054484953204A004120343C0002574F524C44CC01200F14002AEC00351D010A11000000000000".unhex
        val uncompressed = lzoCompressed.uncompress(LZO)
        assertEquals(
            REF_TEXT,
            uncompressed.toString(UTF8)
        )
    }

    @Test
    fun testCompression() {
        val baseString = REF_TEXT
        val uncompressed = baseString
            .toByteArray(UTF8)
            .compress(LZO)
            .uncompress(LZO)
            .toString(UTF8)

        assertEquals(baseString, uncompressed)
    }

    @Test
    fun testDecompression2() {
        val compressedDataBase64 = "TFpErwAAFwAAiIj//60AeyksACoNAFpdAm5NAGxMAG0BWS2cAHwBbQNt3QNpzARs\n" +
            "AWwCbAH8AWwA/AjtCWspfAAqzAF8ACpcAXwANnwCrQEoThgigg4BMiIOADMjDAAI\n" +
            "IzJ1ADIjWAAzM1dMAAX//wMw//8AACwMACgdAXrdCHxcAGwAfCF8AOwMLtwD7AV8\n" +
            "BHwUKiwCKiwDOg0AMcwbfCs63ADsJjw+ASiCDTAiTx8iI3YMAV8dIjJ0XR8+TR8/\n" +
            "nR8zXR8DM/0DeTFsAXwh7CH9Q2/MASosBHwELmwEfQRCIAYMBCoNAHAgBJwDrz3/\n" +
            "/4IpHQYoThgigg57IiJOHyJcTQBzDT4iXh4jPU0fW7w/TQAALQwIKBwH7AFsMX0h\n" +
            "NCk8CPwc7BPsUSrdBGQgErwDKiwB7EjsECp8AOwDbQEujQEoXlwiKA0FIk0BIkwU\n" +
            "vV50DTkiTF19XyJdXiN9Xjwz7QMwTQAzK/wDbFE2jAL8HCqMAuwhKowILswKIAP8\n" +
            "A+xrNnwPNO8HIoIlrF2MAGw+Xj0jIkwdfH1MG40ABi/sA/4eMwOsBDD8B2xQ7QEz\n" +
            "IAJMAvyE7AAu3AMgA1wD7KggBZ8EKIIfXR0jJ/wDnp0iXVw9bRohKMwD7AAq3Aec\n" +
            "Hn1CYpwEMPwDfRkyMS0CeDWMBv1/aikdF2YpnADshiALDBD8X70NKE0jgikMFoxf\n" +
            "jR9kTXwCXB59mwcn3AcynA8u3AfsADQ8AzqMBuwTfAF8YmwC/JZsAiq8A2xq/Ekq\n" +
            "7AQyrAf8AyosFaxsrH1MfHx8jF99XXZMH7zcKjwDKu0DUE0ANSvdA2hNJGTcJL/i\n" +
            "iIhl3A18O/x8/EF8AHwSIAcsFyrsDCr8Bz5sCyq8EyoMAcxrXh8oImzbXB+cPlza\n" +
            "J30TZk0ABC+cD24fVQVNAFNMICd8BCQfIDMzY9yEfB8qzB5sDz4sDvwTfAA2bAt8\n" +
            "GTYcG+wDLgwdKpwZLiwKXDKMIG8fcwBCXdpEIV0jNJ0ZJCjcCynNC1BOHzVVXAJs\n" +
            "AEwgKFwUfCX9IVgpfAo+XQc1KZ0ALylMCyocA3wBKrwaJmwjKuwLKuwDbB4qDB3t\n" +
            "AXac+Xz7jb2IDXsiTAJ9XkJePkQkDgQ0RF0XQw0AQ11ZNDGNF1N8H5weLu0HZyf9\n" +
            "AyOcfzKcBi5MBC58Km0AaCkcByZ8LDIsLX0DHS/tDyhMHyT8ImwdzPicAFwcjHxN\n" +
            "HihtHnOMXpxdT/tDJF8NO0QhTiNDCwwCXhc0NEwfXCArfBtMfQFTNYQAKP0DFynM\n" +
            "B7wgbh8iMux/LgwA7LkqfAvsOPw7KtwHLuww/Fk2DAwhvCaMmsw73Jnse21cIk0d\n" +
            "IkweJ3wbnZxYDjw0DAw/nD6sH2xAKlwrvduEDjg1ggwAJ/wD7J6sAawfbAt8f/xQ\n" +
            "Jpwy7DZsGv0XMC0tBGctbAMqrCxsWDKMBCr8AF0FgkwgnBwizCr82SytBzNc25wA\n" +
            "J4wXIRwr7B78ACZsK/w/nR+DKbwDXCApvBco7AMu/AMunA5sGuwWMtwf/Bt8HSYM\n" +
            "N2xiLqwDJhwt7CF8BbxFvPh9PWQonAdNACNcHSL/MjIzCiefFjMzXClMAy28Gyq8\n" +
            "H10fMC+dAGJcHiFMMIwfIdwwTQyIXS6IfAAzDAvsOCIsOy58Gi7MHuyD/WVBIAI8\n" +
            "FdxFTHutumYrLh8jMgw7vAFtHyC8fCcvA18AU0H6VU0BNVwALkw7/H8qjxcjM10M\n" +
            "PifcA0wAXW6IXG5sjyxMAvye7JoqrCL83WwA/LwqTAMkXDIsnBtsBiwsHfy6rAFM\n" +
            "+kx/KCwffRVeDAFdU0NMIa0eCSfMC+09UCFRI1Ve2gVTDAE2XC+t/F5NH2nMH0xd\n" +
            "nB5tHnMtfD98Pfy5IANsI+wa/F0gAw0AM7y8JS0ngkwO/F8ij0JDM2pc2O1dCCic\n" +
            "DzJdJwXcHyGQIyxcLzANACMn7ANs/fwfLswh/J8u7ApsPCZMJCrcAz68Qi7dB3so\n" +
            "7APsnczbfFxuPUQ0DShEXB0onAs4DDMq/AM0XS9cXD5MiiG9K4JOSYgo/Jz8Xibc\n" +
            "LvxcJjxKMrw3Krwi/BoqXBdsAi7sPC69IHrMPa+9MiIDDiciOE0fI11eH03aIUz6\n" +
            "XBZtAEOs1TIMMyhsI70fNVwfvB4ijEPsHyEcOGx+XD59u4Ir3Ad8XiosQ3wUItxC\n" +
            "LvxG7DcuXD/8uu0bGTFsCGxGfARs5rx9JDwjzF38nF8TMyNxnRZDTF1s+iisFyws\n" +
            "KyeMB/w8XH8kXCtcACNtSz5dPTyduzKMfimcFyfMDyq8KSrMLyp8BzacMypsEzLs\n" +
            "E+weLkwMJuwyJHxCjD8pvDYnXRc0Id5CQ2ko7AMuvEI2bA+cHSJ9TzzdPRAMGGzn\n" +
            "XMcs7ANsACpcUyqcCir8GyrcHi7sB/w7NpxKNmxUIXwrKd0PMly8KNwDKowTrD8v\n" +
            "nEoz7AO8RoweJawgfD0oHCUkbCm8zHx9Kiw1fAAsfEE03D86rAQuTD98AHxoLixL\n" +
            "rSgoI3wrJhwrXRw0ISwjLmwXMqxGzD/8Jbw9KIwMb2gjImol/CQkPSeCXABs/CNd\n" +
            "ISgMnKwg7F4ynFc2/FcuTAQmLDcqHDgqzFY2fFz8H0yfjjVjAGx0MqwLNxwj3ET9\n" +
            "PTInXCMhjDAn3AfcH4xeTBFMTiSsIzZ8UjCcAjhMBPwDNhw7LhxQzL4nvEYp7BMj\n" +
            "vEop7Ac5nBMCQzM5AEQq7CecPTDMC4x9TIx9jIJc6qwRJjxaOkwjNpwXLmwTMkxT\n" +
            "fAoozFsjLCdsHYwA7L4mPF58/FxWOaxKLpwTJ3w3jOIl/GLsfifMA7weJz0rgkxR\n" +
            "jAEqTGI2bAoqPAc6/AM6fETsXyYsJyhcTpw0jT4DJDxeI30rXzcsYiicE0zBIQxX\n" +
            "rOIm3DoqzD4yXBsqPCvsfSosJzL8NipMBD78BzLsACysJCeMD/1TMiRsJypMWiRs\n" +
            "IjXcC5wfIhws7MMhnUpDJs1KZUx9XHtc2ow/J+wDIQw3KzwrNowLKtxq7P4+/A8y\n" +
            "nFZ8CDQ8CScsLyUOOzMiTJwp7HE2vDqtHyUp3ANcQ8zDbR4yJ8w+zB8nvAvs2y4M\n" +
            "TS5sHuwYMmwjIAP8Cy4cDCZ8d3wKrEwkbC8lzkYjQ0wWKqw+bF4wfFYq3QtDIQ1T\n" +
            "RJxdfCVsw6wfI5xCXGYw7AMqLCsgC9wKLrw/NlwQMgwB7AUkvTAoXAEnvBOMP0w1\n" +
            "JuxtLZwTLswLKp0XQ1wfjR9EIS0wQyycD+zaLOwDJkwiKvw2IAMMAC6sWi7cdiL8\n" +
            "XyoMLzAsICTMda5gKIJ8Ei0MaicMPyxMIyI8RiusFyetE1y8nCUsVyesBJ3caydM\n" +
            "I6x+KhwnfF0ijFIgB8xwKuwLKowUNmwYfAIibVRuzB5cuybcZSPMUmwBfRIkTQAl\n" +
            "LxxmL5wLXB03PC8tnD8nLCxNXiMn7AdMXYw/fBYmXFUq7AIyDAMqzDoynA8qjEcq\n" +
            "HC9sJSqsbSEMI2weJ3wXKpxWJXxWLlwfLZwLKsyBrD8ijDYhjEsnTWILrJwhLDcz\n" +
            "bCchTCMkHCsuvAs2bCMqTIUqbAMq/AAgBnx6IUyFJIxOKqxSJCw3LGxarAAjbE5B\n" +
            "H1VRHVVAHyKMTiZ8WivsB2xcJhwnIqxCjR0DJYwsJoxWJqwjTF8nXCMujHkqnE0q\n" +
            "rI0+zCc6LHooLFsn3AsnbFokbIFvUzIiH03cXiftGlNOAFU1TAAhbD7sACcMYkwg\n" +
            "Lxw7nHwmnF8k7DIorAUoTFy8/SacWTaMhCaccT78iCr8NyqsBCZMK/xmIsxKTF8n\n" +
            "DCssHGJ8c2wBKxxiTR8DAn7/AM0AMEwAJIxCMj0YW9z6IXxSXB4nfFJNLYgnHAR8\n" +
            "AioMMyoMlSL8JWwWKpwnKuxyLswPLpwg7AnsHrwAJlyNJGxW7F7sHyHsMiU8Knwe\n" +
            "I20yNCGsUilsViAB3EK8Hnx8JoxKLEwnJf0ygkzMJ1wAKoxdIAdMhSrMD/wcKgwc\n" +
            "JtxL/Arc6l6dKCgiLJEoPGacPyfcOa8AQzQLI8xOI+xtNdw+NsxGjH18fDNMXils\n" +
            "HkwQvALsUzIsMmwDfFkufB/sXGwJJpxiKvwALLwAKPw2JCyRKB0zBt0AECGcWl71\n" +
            "MyUhnFohTGIlzE4gAfxhbR5dnLojDmYyXylcEyocMyY8J+zzKnwa7EAujGEqHGoy\n" +
            "fAQ2XEMuvBMmfH0mHHIobYVDTTQ0bX4yXpwjBww+Xh5DQw0zNCG8Wi/scSu8RiLM\n" +
            "M/zYXJstvAcobAsqbFIyzHT8eS58B+xgKkylMhxX7CIu/AAqbEtMXiXspCZvhUMj\n" +
            "YyNffUM0HiFcXiHMdXwZI20yDSNsYnxeMlwnKDwUJtwyKYwPKrwHJ2wLKvw2Oqw+\n" +
            "OkyUKkwDNrw+LGwXLhwdJOykIsw6fLpMACqcdSN9KiMjbFImbHGcXilcJ/wfvQYi\n" +
            "LAxaK4wPKuxt7AAmTHQqHHEqXAMmDHUqDAAiXF8yHIk2/AQqjAjsLaxNJOykzH4v\n" +
            "7TJcnB4njBNsHjV8lSIsMyStSiAlPFpc1y+sPvx8LryLKvyt/HLsXCQsYSy8Ey2c\n" +
            "XSfcEDq8RiYMVCgsBCR8I0yeJWwrKPwDNDxaLswDK1wn7EYivHklfIG8HypsFyqc\n" +
            "Dy5cViz8UDwsIyZcZypMKzLcnjJcaiactiH8qCdsXilsZiPMQiQ8YjJ8UivMAyVc\n" +
            "JyqMeip8hSQcXiqsPvwa7FxsOS4cJvwh/FUqDEcyDG7smToMqy4cDKxIJowpJ7wX\n" +
            "K619WCf8AyqOSjMwXCOMASmcQykcNyTMdfx+XF4jTFYq/CUmfDk6rC78kzA8pDTc\n" +
            "DyALvKkoPAgmPHIuLStDIY02BDPMPkzDXEIkrHr8JSbspSRsU5wfL9wLLqwPOuw9\n" +
            "Mk2qPzWMAyZMPCALjKkoXIsq/DYiTCMmDHpuAQRALpxKzEON/z4l/JUhfIJMBiY8\n" +
            "OybcOrwfLqwTJryMNsy7LmwLKiwQPtw2Kpy+KrwUOi2rWl0+ZZx8rBlcN03dQ1wZ\n" +
            "IkxKL/wDI80nDFwFIQwgL8wLIYyJIbxTKOxxKlwj/Bs6nEI2rB58JS7MpC6MCyr8\n" +
            "B+xLOmxyKIwB/LytGwicIIwfXSAA3B/sf6wEIjy9JD1iI10AIiHsdTQsMyrcCyAH\n" +
            "PMhsACJ8bTZsDCrcA2ydbDgyfDcgBxwlXD8mzF0hPTNDTDsu7AMr7g80X0wEIt2p\n" +
            "Q11cCE09Y10dPi9tK4gh3HE0jB8gC2wnPvwGNqxbLozGPCwcIjyg/X4EXQAALuwD\n" +
            "KtwXnURiI/+hIzI9zccCIwxQLAw7JnwpfAAqvA8y/AMqXA8mnFIyXLE+bCMgBxxb\n" +
            "KnxAfF9+Wl8ALswHbQBATQA0J7zAvB8rLHYnjUYkKbyBLpwXMkzENvwu/PcuHJIm\n" +
            "fKIuzAcmHLIu7MAuXEQmvCEujUdajNkurBN9HkRtAAQmXIrsvyIdY10pXDMpLKEn\n" +
            "7AP8lSZsJyKcQS4cYSzMRTjsHipMeS7sCC7sACaclirsCCrMJC48dL0AAE0fQEw/\n" +
            "/B+MH3weJTySjMEhHKYkfGYqbGIqPDdsnTLsOTqMH2wSLpwHMlwjKrwCLgwrKrwD\n" +
            "MrwQKnyGLryurBCMPlwd7V9AzB98Hqz9Ic1DRCG8I30hQyHcWp2cAyTsqCuMHyo8\n" +
            "AiaMhHwObFoqrHQ2XBJ8BiZMSiqcIDocFDo91kMxPJv8Uii8Ai9MM1wffHsoLSQL\n" +
            "TH4hXCNNASImTF4nbI0yfI06DGY2fJUqXBNsHybcrSpsGzzs5yy8U3wiMqxILkxQ\n" +
            "KPwDKhyhrIJc3Iw+TB8pPDMmfI0kfCMqzEo67AMm/D4urDM+HAcmjHIgA3zYIAMs\n" +
            "fzCMACyMH+y8JBwoKG0nYiWMXiFMbifsByqcGyATTBoqbAt8PyJMvS7MTy4MPDIs\n" +
            "EextKnxX7CcmfIoiXDwqHDUonAwyjB8mrMycPib8TiusFyrMSiADbBvszzIsoTIs\n" +
            "CybcICADHCcu/JVsB2wALjwOLswJJKxIMMy8Jvx9zH0hHIMsTJ38fSaMvC5cBewN\n" +
            "7JkqfAR8BSLMtDLMzCocNCbcIGwIKvzYKiwYKuy1KvwDJnx4OswEKGwFMpyh/X0y\n" +
            "IRzxJByPKIzpJIwnKgxHJtzDMCxlNKwfKhwALiwAKkxILtz8JvxeMmwA/OkuHEls\n" +
            "TSZ8cDLNAFmUH6wgLBQABF8AKQAGAB83fAAwFADDACkAIUAHIBDYAAIiAAwADyAR\n" +
            "2wArABYgE9kALVUNBCAR2wAcACUgE9kAHgDYIBORAjdYBiRoYSAM2QBFTRsBIBHZ\n" +
            "AERNGwcgEdkAQ0EiFyAR2AACTQAbAAkgEdsATwARIBPZAFhZLxUgEdgAAogAAgAU\n" +
            "IBHbAJIACyATAQYzWS8KIBHbADwAKlgUIBIpCw4gE98GOwAFSDAgENkAj1mCPSAR\n" +
            "2AABLwA6ACIA8yAQvw0aADkgEyUFGSATtQFRVQ03IBHbAFUAEyAT3QYeRFIgEnEJ\n" +
            "W0meIiATtwEsACQgEdsAlgAyXA0gEuUSMkg+IBK+DS4AIggnIBDpGCZIZyASCRIr\n" +
            "RAAgEgkSMSATcQlTSUs7IBHZAHdUbSI0MyAQMRc+IBPBE0NUbSAScQlEXW0+IBPn\n" +
            "EjYAIyATXSI3IBU4IyFcpiATkwIgADggExckLQAoIBHZAJhAByAS4QwYXF8gEtkA\n" +
            "k1k9GSATkQIkIAbAACvaAEIAIZBdIBHZAExZBicgExEePCAT2QAtTekLIBMBBhMh\n" +
            "/CogENkANFQ2IBKlIBZAByASKwsWAAghrCAgENkALlRtIBJtAyFMKSASbQMoUKsg\n" +
            "EmYuKAAhVJMgEdkALEVSHSATcwkgABIgE7kHISAT5RI6UKsgFD0pBlwbIBKZDgMg\n" +
            "EzEXQkhZIBLVMUFNwBMgE9kAGyGwJiAQ2QB2TAAgEoEhXEXACCAT5RIOIBN9G0xF\n" +
            "GykgEdkARk1SICAK2gDyAQwAAkgAZQByTAACIABsAGldAHNFAUpHAW0ADURO7AB8\n" +
            "CvwAIA8FAF9UgGQAIBH9AC5FAC8pPAAgFRQBIAUEAGwLLEUALFQPIDe0ASAB/QEn\n" +
            "RAAgWzwCIAWkAjDUBOUCLikkAKwBfAAgYfwCKMQCMOwCbBUs3AV1AS4tPAAgPw0D\n" +
            "XyfEBCzcAnUCPTV8COwStASkALRufARBAB0h5TEMIBFFER1A8SASARkfIYxGIBLl\n" +
            "JU5YxyASxSxOIegrIBJFSCchlCcgEkkXJ0TBIBLxNyNE6iAStUsjIV1BMSAR2QA1\n" +
            "Ieg3IBIJJTVYrSASiUA5SCIgEr0gIExSIBIhST9IIhHUDCARtQEGIBNJBIZURCAS\n" +
            "tQF7RBsgEqUziyE5QjYgEdkAc0CAIBLZSnNceyAS8TcWUY3/DXweIArkACERYTcg\n" +
            "DKRFRQwdIAzcT0UMJyAMRDQhNV42IAwkX0UMCCAMFDRBBhYgDPhkUR4nIAyUYEEG\n" +
            "CiEYLCAJwABBYAQgDJA0TcYaIAxsWSHJSQsh9GEgCcEAEA0BOCAM0ANBBisgDNAD\n" +
            "TTYeIAxcGSGFPAchyDUgCcAAIblAIiAMXD1BBhQgDrlG/whiIA0hGP8I9iALlARd\n" +
            "ACUgDkI1/ykgDAENDCEEVCV5IDMj6MEgBt0AAd4GATVEByAM3QBSnQYCCbwdIBPe\n" +
            "AAUcVHcgDN0ATrkiOlwbIApdBAEhHFqVARUhwD4gDN0ASb0GLkDpIBPdAD1UvyAM\n" +
            "3QBIvQYtRPAgDN0AS70GEyG4aSAT3QAEQJ0gE30DAiH4JCAM3ABUnUAAIUVzASEk\n" +
            "KiAPsCC1ygYgELQgaQ4DAA0gELgBuAYgE7QBWFogD1R/tccHIBDdAAEBbwwgEGls\n" +
            "AQUCDSAQYGyhAQ0gEFhauTYMIBB0fmEpAggNIBAgBeAASBUgFBAGSBUgFIUJDSAV\n" +
            "qQgIIBAsXKEBByAV3QADDA0gD5Q0gTEFIBAwVichBgMIpyAP9DjQaCAP8HylHAUg\n" +
            "ELRTlRQHIuAjIBHZAAMhYGIgEtkACCAQ7HCNIg4gERgGlQAMIBjcACAUDAZcFCAQ\n" +
            "yAdwAFx9IBNYBLlMDSARRAx5Bg4gEOBfmAYjkDYgEdkAFyAQcJOdPgogECRFrWES\n" +
            "IBWVAhQgFLUWAwzdIBTZABIgEAAbjSIVIBUGIQQbIBDoGwEE/wQaIBDRAAMBNhwg\n" +
            "EZAdfSELIAzoPyeJFgsgEOBrKO0MByAMXGMnCQYZIBA8arlSGSAUWB9QZyATPAUh\n" +
            "RHAgEHwJJURLIBTeAAMiIBQ9CwMIKSAT9QwEBBsgD1AEIfknKCAV+ScDDA0gFGkD\n" +
            "KSAU2gAEJiAQeAlJGxYgEdAAlQAlIBD8JiGQNkEACSAQaKCcDVh1IA30QCm6DgIa\n" +
            "IBRYFyZYLCAPgIKpPwogEdwAsJkgD3gElRQJIBDMgKEBBCAR7BSVGwMgFPIUAw8g\n" +
            "EZgCeQYbIBFYJaTeIBCIKpUALyAVpBxQFCAV1SERIBVxKwwgELyBpXA5IBAAq6EB\n" +
            "OCAMDFIoeQM4IBCUoZE+OyAV2QA6IBGUAm2nOSAQ2ABNpz0gEICPoQE7IdyNIBPd\n" +
            "ACogFeUGAgwNIBNMBIIAAysgEcwBuAYgE8gBIUxmIA8Ebq1MKiGoUiASlQIqIBG4\n" +
            "AXFFKiAQeXEBfFkgD5h2hSI6IBWuCAQEIBChPQMBNgIgENAAIa0vMCAQDBmQBiAP\n" +
            "ABkhgTAwIBTaLQMaIBDIepU8GiAQRKNFKBogEEkQBAUCLyAQUJa1XjAgEEx3nSEt\n" +
            "IBBQfZkGLiAVtQEaIBCwspkGGSAV2QAbIBDgeZVsGiARcA6FqxogEHh8KFUqLiAR\n" +
            "1AfhAAQEpiAP0KBZ1TEgEODEuUwzIBAkt4kxMiHQjyAS2Q0vIBC0DlGrLiAUiA9Y\n" +
            "BiAQAB95BicgEHDHmQYoIBH8Bbh1IBPYAESRIBGQGiEYJCAPTApRLz0gFMBaQD0g\n" +
            "EhUdLyAUxB5EpSAPrAElMCkgE9kAAgRRIBOQAoIAAyQgFbENISAV2QAgIBAcEZml\n" +
            "IyAR2ACVACMgEZQClQAiIBa9ASEgFt0AICAVGCpcBiAQvAjcFCAUvQEEDA0gEgkN\n" +
            "IyAUaC5YBiATPDZINyAQ2ACVACQgEWQd3AYgEAAglQAxIBZVPDAgFlU8MiAVeTsa\n" +
            "IBUVBhYgFnUDHiAQ3AAkjUEnIBDUMiOUT1iYIA/MLdBMIBT4JlxuIBTeAAcuIBDF\n" +
            "RgRMBiAPKmsEBBEAriATOB4gE6A5WAYgEKA5QQABCCggEoKoAAMADSAUnS41IBZ1\n" +
            "DzYgETQxlQA3IBZdVTggEaQ6mSI5IBRcVUyCIA8IPrl0OCAQNL2adAQjIBBoLwEC\n" +
            "/wQ6IBBxQQIiBCYgD0SwJeAmIA9sVNgGIBOSYQM/IBVRgzggFbUBDiAUNlwEDyAQ\n" +
            "NFxNNiYgBrgAK80GJSAVHXckIBXYACFMnSATyQwlIBa0chEsyCAViAggDwDdJRBc\n" +
            "IBTZAD0gENzKjSI8IBG0AZUAOiARuAGVADwRnAQgE90APyAVvEwhkS0hIBTcABEM\n" +
            "0CAWvAEgF9wAIA+00oEBAgxTIBPYB4AAXFogC5S87gcHFCAQL1EDBxggEc4ABAEg\n" +
            "EOEfBCIknSAUJSQBIBTZAAIIDSATACWBAAQEtCAPyHFZGz8gFVmIPiAU2QACAKUg\n" +
            "GGypWBsgELCAIZS1IBBlKQIFAiQhwH4gFWRCIBSYf0T4IBDoH9wGIBCcf5UALCAR\n" +
            "YCOVAC0gFt0AOiAW4bM6IBFUHiXALSAVHQY3IBGgoCWAOyAVyRsdIBbJGwwgEZgV\n" +
            "3AYgEMxmlQAVIBYFTBQgFt0ABSAWoUEGIBbdAAUgEcBA3AYgFOh+ITzFIBTFYAYg\n" +
            "FX1iOCAVEZE6IBBs9SXYmSATdY4EAcIcIBEtaiogEewzhbwqIBRgN4AARGggFZW1\n" +
            "JSAZlrUEFyAScgQDGCAQ0ACNDR4gEHgrVewUIBDTBwL/AQQaIA94Q4UbFCAQXANh\n" +
            "AAQSMCkgECEgFhArpABBABcQKwg2QYMXIBVNcBsgFII3BAsgEHgWIfmECiAUgCoh\n" +
            "SKIgFMkTCyAUyBMhUMIgFEAXVJAgD80TAwUCKSAWVdApIBWBFSogF1wWIBBMKyRg\n" +
            "RCAT7MQhhGEgFCELOSAVPdA9IBUVygggEJQOJWjVIBOUdCFoIiAUYKchHD8gELwH\n" +
            "lQA7IBYQLCIUayAVcNwgD6wUJvSwIBRAiSHY0iAZPaIPIBSMQ1gGIBSdFQwgEukZ\n" +
            "BADCIBHgOiFFtT8gE10DBCAV2QAFIBXZAAggFdkAAyAUJJAhfEMgFDXWNiAUuSYD\n" +
            "EpAaIBJZRDAgEUzP4QACESzHIBqciCAUhWMjIBTMYVwUIBUdbCIgFn3BISAU/Owh\n" +
            "qJQgEzQZIcQhIBTo81StIBSwSSHYmSATQB9AnyAVdeknIBY5GSIgEUQMlQA1IBYl\n" +
            "8jIgFeEOMCAU9G1MiyAVKXkRIBbxvhQgFpEdESAWEWEIIBZlSxcgFskOESARcGUk\n" +
            "ZEQgFIVrEiAUoIgh7HQgFFWxGiARoGqstCAUvbofIBXNeykgFR0MHiAVVEQhoIIg\n" +
            "FV0KJCAVbP1UIiATeQMEADcgELNp/wQuIBAEEyF5PS4gFQU0LiAVqRsXIBURvxYg\n" +
            "FNoAAj4gFJyogAAh9HQgFdGnPSAW3QA8IBXcABFEBCAU4TsMIBUBEw0gFAQ7Evx9\n" +
            "ISSHdQEBIfg1IYxsZBfhAgghLCorQQANLoUADS6FAAhEXWEAAeECCUDVK0EACy6F\n" +
            "AAtAhitBAAxIAitBABdUCGEAAuECGCEoQmEAHKEZAU0MDiwdAhYhNPdhAAbhAhkl\n" +
            "nPEnQQADWAorlQEKIUwqK0EABlitK0EAGlgbK5QBEaD1LEEAKsACJ8kAKy7FBCZY\n" +
            "CmAAJ9UFHBHAFCtBADAhMGErQQAQTKorUQE/IdAtK0EAEMgoJ4EEEy5BAA/IFyfJ\n" +
            "AAQucQMxSNkr2QE4RF8r6QI4LoUAEy5BACYhNGorQQA2SDUrHQIrEjAvKlEBK8wI\n" +
            "J10GMFz+K0EAGiGAJCvJADHUDCdBABshREUrQQA0IYA4K0EALVAGLUAAERAzKmEC\n" +
            "J1huKz0EKUwEK0EALMACJw0BPC5hAhtMjC3EBCI0kSmVAT5AUyuFAC8ukQUgwAIn\n" +
            "CQUiyRcFoQICRSYwLGECJCGELCuVASIjuHQpQQA0LoUAClDELREODCyFADBQOS3k\n" +
            "BqwqJ0EAN1Q7K6UCOtxxJ0EADth0J0EAJC69DDku1QU+IVQyK4UAPhMshin5AxTI\n" +
            "KCdBADXYMCdBAAjYDicECRksGCxBAD3YlidgAhFU8SzZARVEMytBABwUpGEqBAmo\n" +
            "WycNATtUKiuFADcu7Q8lLpUSGy45CAzULidBAAcuvQwozH8nGQYpSJsrQQAlwIon\n" +
            "yQAZwJsnpQIVLkEAH8xuJ0EAFlg9KUwFYBOgJCdBACoTMDIp2QEaxDcpyBGwXydR\n" +
            "AQdQBi10EKjBJ8gAbHsrQQA62JYnQQAMLqkPDtgOJ1EBC8hKJ0EAD9xUJ8kABVBK\n" +
            "K1EBB8ACKfwQuKcp9QcaLgwSpEgpzQ0iLIUAJNRhxQQCRDst5QYiwDUnhQAhLq0c\n" +
            "MNRyJ0EAL9DWJ8kAINgOJ0EAGVA5Kx0CO9x2YAARAAA="

        val compressedData = compressedDataBase64.fromBase64(ignoreSpaces = true)
        val uncompressedData = compressedData.uncompress(LZO)
        assertEquals(44868, uncompressedData.size)
        assertEquals("366F810206E2ACDCA532665BBEDC97B6", uncompressedData.md5().hexUpper)
        assertEquals("C22721A02CB45F09EEE3C65B6411E655913F4323", uncompressedData.sha1().hexUpper)
        assertEquals("C66CE440CF8BB8D225017C1A65220B4867BF5780D62345C81C0BE7E82A6F80CB", uncompressedData.sha256().hexUpper)
    }
}
