package com.soywiz.klock

import com.soywiz.klock.locale.*
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.test.assertEquals

class KlockLocaleTestJvm {

    private val outputPattern = "EEEE, d MMMM yyyy HH:mm:ss"

    private val klockDate = DateTime(
        year = 1995,
        month = Month.January,
        day = 18,
        hour = 21,
        minute = 36,
        second = 45
    )
    private val javaDate = LocalDateTime.of(
        1995, // year
        java.time.Month.JANUARY, // month
        18, // day of month
        21, // hour
        36, // minute
        45 // second
    )

    @Test
    fun assertDatesAreTheSame() {
        val javaLong = javaDate.toInstant(ZoneOffset.UTC).toEpochMilli()
        val klockLong = klockDate.unixMillisLong
        assertEquals(javaLong, klockLong)
    }

    @Test
    fun assertEnglishLocalization() {
        assertEquals(
            expected = Locale.ENGLISH.getFormattedJavaTestDate(),
            actual = KlockLocale.english.getFormattedKlockTestDate()
        )
    }

    @Test
    fun assertNorwegianLocalization() {
        assertEquals(
            expected = Locale.forLanguageTag("no-nb").getFormattedJavaTestDate(),
            actual = KlockLocale.norwegian.getFormattedKlockTestDate()
        )
    }

    @Test
    fun assertSwedishLocalization() {
        assertEquals(
            expected = Locale.forLanguageTag("sv").getFormattedJavaTestDate(),
            actual = KlockLocale.swedish.getFormattedKlockTestDate()
        )
    }

    @Test
    fun assertSpanishLocalization() {
        assertEquals(
            expected = Locale.forLanguageTag("es").getFormattedJavaTestDate(),
            actual = KlockLocale.spanish.getFormattedKlockTestDate()
        )
    }

    @Test
    fun assertFrenchLocalization() {
        assertEquals(
            expected = Locale.forLanguageTag("fr").getFormattedJavaTestDate(),
            actual = KlockLocale.french.getFormattedKlockTestDate()
        )
    }

    @Test
    fun assertGermanLocalization() {
        assertEquals(
            expected = Locale.GERMAN.getFormattedJavaTestDate(),
            actual = KlockLocale.german.getFormattedKlockTestDate()
        )
    }

    @Test
    fun assertJapaneseLocalization() {
        assertEquals(
            expected = Locale.JAPAN.getFormattedJavaTestDate(),
            actual = KlockLocale.japanese.getFormattedKlockTestDate()
        )
    }

    @Test
    fun assertDutchLocalization() {
        assertEquals(
            expected = Locale.forLanguageTag("nl").getFormattedJavaTestDate(),
            actual = KlockLocale.dutch.getFormattedKlockTestDate()
        )
    }

    @Test
    fun assertPortugueseLocalization() {
        // Java DateTime formats wrong in java 8. Hardcoding expected value instead.
        // val javaOutput = Locale.forLanguageTag("pt").getFormattedJavaTestDate()
        val expectedOutput = "quarta-feira, 18 janeiro 1995 21:36:45"

        assertEquals(
            expected = expectedOutput,
            actual = KlockLocale.portuguese.getFormattedKlockTestDate()
        )
    }

    @Test
    fun assertRussianLocalization() {
        // Java DateTime formats wrong in java 8. Hardcoding expected value instead.
        // val javaOutput = Locale.forLanguageTag("ru").getFormattedJavaTestDate()
        val expectedOutput = "среда, 18 января 1995 21:36:45"

        assertEquals(
            expected = expectedOutput,
            actual = KlockLocale.russian.getFormattedKlockTestDate()
        )
    }

    @Test
    fun assertKoreanLocalization() {
        assertEquals(
            expected = Locale.KOREA.getFormattedJavaTestDate(),
            actual = KlockLocale.korean.getFormattedKlockTestDate()
        )
    }

    @Test
    fun assertChineseLocalization() {
        assertEquals(
            expected = Locale.CHINA.getFormattedJavaTestDate(),
            actual = KlockLocale.chinese.getFormattedKlockTestDate()
        )
    }

    @Test
    fun assertUkrainianLocalization() {
        assertEquals(
            expected = Locale.forLanguageTag("uk").getFormattedJavaTestDate(),
            actual = KlockLocale.ukrainian.getFormattedKlockTestDate()
        )
    }

    private fun Locale.getFormattedJavaTestDate(): String {
        return javaDate.format(
            DateTimeFormatter.ofPattern(outputPattern, this)
        )
    }

    private fun KlockLocale.getFormattedKlockTestDate(): String {
        return KlockLocale.setTemporarily(this) {
            DateFormat(outputPattern).format(klockDate)
        }
    }
}
