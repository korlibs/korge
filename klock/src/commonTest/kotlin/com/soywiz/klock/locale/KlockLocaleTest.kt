package com.soywiz.klock.locale

import com.soywiz.klock.*
import kotlin.test.Test
import kotlin.test.assertEquals

class KlockLocaleTest {
	val date = DateTime(year = 2019, month = Month.March, day = 13, hour = 21, minute = 36, second = 45, milliseconds = 512)

	@Test
	fun testSpanishLocale() {
		assertEquals(
			"""
			mié, 13 mar 2019 21:36:45 UTC
			13/03/2019 21:36:45
			13/03/19 21:36
			miércoles, 13 de marzo de 2019
			13 de marzo de 2019
			13/03/2019
			13/03/19
			21:36:45
			21:36
			""".trimIndent(),
			multiFormat(SpanishKlockLocale, KlockLocale.spanish)
		)
	}

	@Test
	fun testFrenchLocale() {
		assertEquals(
			"""
			mer, 13 mar 2019 21:36:45 UTC
			13 mar 2019 21:36:45
			13/03/2019 21:36
			mercredi 13 mars 2019
			13 mars 2019
			13 mar 2019
			13/03/2019
			21:36:45
			21:36
			""".trimIndent(),
			multiFormat(FrenchKlockLocale, KlockLocale.french)
		)
	}

	@Test
	fun testGermanLocale() {
		assertEquals(
			"""
			Mit, 13 Mär 2019 21:36:45 UTC
			13.03.2019 21:36:45
			13.03.19 21:36
			Mittwoch, 13. März 2019
			13. März 2019
			13.03.2019
			13.03.19
			21:36:45
			21:36
			""".trimIndent(),
			multiFormat(GermanKlockLocale, KlockLocale.german)
		)
	}

    @Test
    fun testNorwegianLocale() {
        assertEquals(
            """
			ons, 13 mar 2019 21:36:45 UTC
			13.03.2019 21:36:45
			13.03.19 21:36
			onsdag, 13. mars 2019
			13. mars 2019
			13.03.2019
			13.03.19
			21:36:45
			21:36
			""".trimIndent(),
            multiFormat(NorwegianKlockLocale, KlockLocale.norwegian)
        )
    }

    @Test
    fun testSwedishLocale() {
        assertEquals(
            """
			ons, 13 mar 2019 21:36:45 UTC
			13.03.2019 21:36:45
			13.03.19 21:36
			onsdag, 13. mars 2019
			13. mars 2019
			13.03.2019
			13.03.19
			21:36:45
			21:36
			""".trimIndent(),
            multiFormat(SwedishKlockLocale, KlockLocale.swedish)
        )
    }

	@Test
	fun testJapaneseLocale() {
		assertEquals(
			"""
			水, 13 3月 2019 21:36:45 UTC
			2019/03/13 21:36:45
			2019/03/13 21:36
			2019年3月13日水曜日
			2019年3月13日
			2019/03/13
			2019/03/13
			21:36:45
			21:36
			""".trimIndent(),
			multiFormat(JapaneseKlockLocale, KlockLocale.japanese)
		)
	}

	@Test
	fun testDutchLocale() {
		assertEquals(
			"""
			woe, 13 maa 2019 21:36:45 UTC
			13 maa 2019 21:36:45
			13-03-19 21:36
			woensdag 13 maart 2019
			13 maart 2019
			13 maa 2019
			13-03-2019
			21:36:45
			21:36
			""".trimIndent(),
			multiFormat(DutchKlockLocale, KlockLocale.dutch)
		)
	}

	@Test
	fun testPortugueseLocale() {
		assertEquals(
			"""
			qua, 13 mar 2019 21:36:45 UTC
			13 de mar de 2019 21:36:45
			13/03/2019 21:36
			quarta-feira, 13 de março de 2019
			13 de março de 2019
			13 de mar de 2019
			13/03/2019
			21:36:45
			21:36
			""".trimIndent(),
			multiFormat(PortugueseKlockLocale, KlockLocale.portuguese)
		)
	}

	@Test
	fun testRussianLocale() {
		assertEquals(
			"""
			ср, 13 мар 2019 21:36:45 UTC
			13 мар 2019 г. 21:36:45
			13.03.2019 21:36
			среда, 13 марта 2019 г.
			13 марта 2019 г.
			13 мар 2019 г.
			13.03.2019
			21:36:45
			21:36
			""".trimIndent(),
			multiFormat(RussianKlockLocale, KlockLocale.russian)
		)

        val neuter = KlockLocaleContext(gender = KlockLocaleGender.Neuter)
        assertEquals("1-е", RussianKlockLocale.getOrdinalByDay(1, neuter))
        assertEquals("20-е", RussianKlockLocale.getOrdinalByDay(20, neuter))
        val masculine = KlockLocaleContext(gender = KlockLocaleGender.Masculine)
        assertEquals("1-й", RussianKlockLocale.getOrdinalByDay(1, masculine))
        assertEquals("20-й", RussianKlockLocale.getOrdinalByDay(20, masculine))

        assertEquals(1, RussianKlockLocale.getDayByOrdinal("1-й"))
        assertEquals(5, RussianKlockLocale.getDayByOrdinal("5-е"))
    }

	@Test
	fun testKoreanLocale() {
		assertEquals(
			"""
			수, 13 3월 2019 21:36:45 UTC
			2019. 3. 13. pm 9:36:45
			19. 3. 13. pm 9:36
			2019년 3월 13일 수요일
			2019년 3월 13일
			2019. 3. 13.
			19. 3. 13.
			pm 9:36:45
			pm 9:36
			""".trimIndent(),
			multiFormat(KoreanKlockLocale, KlockLocale.korean)
		)
	}

	@Test
	fun testChineseLocale() {
		assertEquals(
			"""
			周三, 13 三月 2019 21:36:45 UTC
			2019年3月13日 pm9:36:45
			2019/3/13 pm9:36
			2019年3月13日星期三
			2019年3月13日
			2019年3月13日
			2019/3/13
			9:36:45
			9:36
			""".trimIndent(),
			multiFormat(ChineseKlockLocale, KlockLocale.chinese)
		)
	}

	@Test
	fun testUkrainianLocale() {
		assertEquals(
			"""
			ср, 13 бер 2019 21:36:45 UTC
			13 бер 2019 р. 21:36:45
			13.03.2019 21:36
			середа, 13 березня 2019 р.
			13 березня 2019 р.
			13 бер 2019 р.
			13.03.2019
			21:36:45
			21:36
			""".trimIndent(),
			multiFormat(UkrainianKlockLocale, KlockLocale.ukrainian)
		)
	}

	fun multiFormat(locale1: KlockLocale, locale2: KlockLocale, date: DateTime = this@KlockLocaleTest.date): String {
		return listOf(
			date.toString(com.soywiz.klock.DateFormat.DEFAULT_FORMAT.withLocale(locale1)),
			locale2.formatDateTimeMedium.format(date),
			locale2.formatDateTimeShort.format(date),
			locale2.formatDateFull.format(date),
			locale2.formatDateLong.format(date),
			locale2.formatDateMedium.format(date),
			locale2.formatDateShort.format(date),
			locale2.formatTimeMedium.format(date),
			locale2.formatTimeShort.format(date)
		).joinToString("\n")
	}

	@Test
	fun testMonthLocaleName() {
		assertEquals("febrero", Month.February.localName(KlockLocale.spanish))
		assertEquals("feb", Month.February.localShortName(KlockLocale.spanish))

		assertEquals("2月", Month.February.localName(KlockLocale.japanese))
		assertEquals("2月", Month.February.localShortName(KlockLocale.japanese))
	}

	@Test
	fun testDayOfWeekLocalName() {
		assertEquals("月曜日", DayOfWeek.Monday.localName(KlockLocale.japanese))
		assertEquals("月", DayOfWeek.Monday.localShortName(KlockLocale.japanese))

		assertEquals("середа", DayOfWeek.Wednesday.localName(KlockLocale.ukrainian))
		assertEquals("ср", DayOfWeek.Wednesday.localShortName(KlockLocale.ukrainian))

		assertEquals("воскресенье", DayOfWeek.Sunday.localName(KlockLocale.russian))
		assertEquals("вс", DayOfWeek.Sunday.localShortName(KlockLocale.russian))
	}

	@Test
	fun testTemporalSetDefault() {
		assertEquals("Wed, 13 Mar 2019 21:36:45 UTC", date.toStringDefault())
		KlockLocale.setTemporarily(KlockLocale.spanish) {
			assertEquals("mié, 13 mar 2019 21:36:45 UTC", date.toStringDefault())
		}
		assertEquals("Wed, 13 Mar 2019 21:36:45 UTC", date.toStringDefault())
	}

	val HttpDate = DateFormat("EEE, dd MMM yyyy HH:mm:ss z")

	@Test
	fun testExtendedTimezoneNames() {
		assertEquals(
			"Tue, 19 Sep 2017 00:58:45 GMT+0300",
			HttpDate.withTimezoneNames(ExtendedTimezoneNames).parse("Tue, 19 Sep 2017 00:58:45 MSK").toStringDefault()
		)
	}

    @Test
    fun testOrdinalsEnglish() {
        val englishOrdinalsMap = mapOf(0 to "0th",  1 to "1st",  2 to "2nd",  3 to "3rd",  4 to "4th", 5 to "5th", 6 to "6th", 7 to "7th", 8 to "8th", 9 to "9th",
            10 to "10th", 11 to "11th", 12 to "12th", 13 to "13th", 14 to "14th", 15 to "15th", 16 to "16th", 17  to "17th", 18 to "18th", 19 to "19th",
            20 to "20th", 21 to "21st", 22 to "22nd", 23 to "23rd", 24 to "24th", 25 to "25th", 26 to "26th", 27 to "27th", 28 to "28th", 29 to "29th",
            30 to "30th", 31 to "31st")
        englishOrdinalsMap.forEach {
            assertEquals(it.value, KlockLocale.english.getOrdinalByDay(it.key))
            assertEquals(it.key, KlockLocale.english.getDayByOrdinal(it.value))
        }
    }
}
