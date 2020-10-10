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
			Mié, 13 Mar 2019 21:36:45 UTC
			13/03/2019 21:36:45
			13/03/19 21:36
			Miércoles, 13 de Marzo de 2019
			13 de Marzo de 2019
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
			Mer, 13 Mar 2019 21:36:45 UTC
			13 Mar 2019 21:36:45
			13/03/2019 21:36
			Mercredi 13 Mars 2019
			13 Mars 2019
			13 Mar 2019
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
			Woe, 13 Maa 2019 21:36:45 UTC
			13 Maa 2019 21:36:45
			13-03-19 21:36
			Woensdag 13 Maart 2019
			13 Maart 2019
			13 Maa 2019
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
			Qua, 13 Mar 2019 21:36:45 UTC
			13 de Mar de 2019 21:36:45
			13/03/2019 21:36
			Quarta-feira, 13 de Março de 2019
			13 de Março de 2019
			13 de Mar de 2019
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
			Сре, 13 Мар 2019 21:36:45 UTC
			13 Мар 2019 г. 21:36:45
			13.03.2019 21:36
			Среда, 13 Марта 2019 г.
			13 Марта 2019 г.
			13 Мар 2019 г.
			13.03.2019
			21:36:45
			21:36
			""".trimIndent(),
			multiFormat(RussianKlockLocale, KlockLocale.russian)
		)
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
			Ср, 13 Бер 2019 21:36:45 UTC
			13 Бер 2019 р. 21:36:45
			13.03.2019 21:36
			Середа, 13 Березня 2019 р.
			13 Березня 2019 р.
			13 Бер 2019 р.
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
	}

	@Test
	fun testTemporalSetDefault() {
		assertEquals("Wed, 13 Mar 2019 21:36:45 UTC", date.toString())
		KlockLocale.setTemporarily(KlockLocale.spanish) {
			assertEquals("Mié, 13 Mar 2019 21:36:45 UTC", date.toString())
		}
		assertEquals("Wed, 13 Mar 2019 21:36:45 UTC", date.toString())
	}

	val HttpDate = DateFormat("EEE, dd MMM yyyy HH:mm:ss z")

	@Test
	fun testExtendedTimezoneNames() {
		assertEquals(
			"Tue, 19 Sep 2017 00:58:45 GMT+0300",
			HttpDate.withTimezoneNames(ExtendedTimezoneNames).parse("Tue, 19 Sep 2017 00:58:45 MSK").toString()
		)
	}
}
