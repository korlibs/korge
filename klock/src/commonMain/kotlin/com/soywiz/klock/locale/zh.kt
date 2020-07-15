package com.soywiz.klock.locale

import com.soywiz.klock.DayOfWeek
import com.soywiz.klock.KlockLocale

val KlockLocale.Companion.chinese get() = ChineseKlockLocale

open class ChineseKlockLocale : KlockLocale() {
	companion object : ChineseKlockLocale()

	override val ISO639_1 = "zh"

	override val h12Marker = listOf("上午", "下午")

	override val firstDayOfWeek: DayOfWeek = DayOfWeek.Sunday

	override val daysOfWeek = listOf(
		"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"
	)
	override val months = listOf(
		"一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"
	)

	override val daysOfWeekShort: List<String> = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
	override val monthsShort: List<String> = listOf(
		"1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"
	)

	override val formatDateTimeMedium = format("y年M月d日 ah:mm:ss")
	override val formatDateTimeShort = format("y/M/d ah:mm")

	override val formatDateFull = format("y年M月d日EEEE")
	override val formatDateLong = format("y年M月d日")
	override val formatDateMedium = format("y年M月d日")
	override val formatDateShort = format("y/M/d")

	override val formatTimeMedium = format("h:mm:ss")
	override val formatTimeShort = format("h:mm")
}
