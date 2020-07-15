package com.soywiz.klock.locale

import com.soywiz.klock.DayOfWeek
import com.soywiz.klock.KlockLocale

val KlockLocale.Companion.japanese get() = JapaneseKlockLocale

open class JapaneseKlockLocale : KlockLocale() {
	companion object : JapaneseKlockLocale()

	override val ISO639_1 = "ja"

	override val h12Marker = listOf("午前", "午後")

	override val firstDayOfWeek: DayOfWeek = DayOfWeek.Sunday

	override val daysOfWeek = listOf(
		"日曜日", "月曜日", "火曜日", "水曜日", "木曜日", "金曜日", "土曜日"
	)
	override val months = listOf(
		"1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"
	)

	override val monthsShort: List<String> get() = months
	override val daysOfWeekShort: List<String> = listOf("日", "月", "火", "水", "木", "金", "土")

	override val formatDateTimeMedium = format("y/MM/dd H:mm:ss")
	override val formatDateTimeShort = format("y/MM/dd H:mm")

	override val formatDateFull = format("y'年'M'月'd'日'EEEE")
	override val formatDateLong = format("y'年'M'月'd'日'")
	override val formatDateMedium = format("y/MM/dd")
	override val formatDateShort = format("y/MM/dd")

	override val formatTimeMedium = format("H:mm:ss")
	override val formatTimeShort = format("H:mm")
}
