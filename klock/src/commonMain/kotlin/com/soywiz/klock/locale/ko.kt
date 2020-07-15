package com.soywiz.klock.locale

import com.soywiz.klock.DayOfWeek
import com.soywiz.klock.KlockLocale

val KlockLocale.Companion.korean get() = KoreanKlockLocale

open class KoreanKlockLocale : KlockLocale() {
	companion object : KoreanKlockLocale()

	override val ISO639_1 = "ko"

	override val h12Marker = listOf("오전", "오후")

	override val firstDayOfWeek: DayOfWeek = DayOfWeek.Monday

	override val daysOfWeek = listOf(
		"일요일", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일"
	)
	override val months = listOf(
		"1월", "2월", "3월", "4월", "5월", "6월", "7월", "8월", "9월", "10월", "11월", "12월"
	)

	override val daysOfWeekShort: List<String> = listOf("일", "월", "화", "수", "목", "금", "토")
	override val monthsShort: List<String> get() = months

	override val formatDateTimeMedium = format("y. M. d. a h:mm:ss")
	override val formatDateTimeShort = format("yy. M. d. a h:mm")

	override val formatDateFull = format("y년 M월 d일 EEEE")
	override val formatDateLong = format("y년 M월 d일")
	override val formatDateMedium = format("y. M. d.")
	override val formatDateShort = format("yy. M. d.")

	override val formatTimeMedium = format("a h:mm:ss")
	override val formatTimeShort = format("a h:mm")
}
